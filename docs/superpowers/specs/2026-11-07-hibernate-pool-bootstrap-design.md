# Hibernate Pool and Bootstrap Modernization — Design

Date: 2026-11-07. Context: issue #818 (adoption review), items "Connection pool: c3p0 → Agroal or HikariCP" and "Hibernate: bootstrap through `HibernatePersistenceConfiguration`". The third item (Querydsl fork) is out of scope; #818 stays open.

## Goal

Move `dev.quokkify:sql` off c3p0 and off the hand-written `PersistenceUnitInfo` without silently breaking consumers, and lock the connection-pool behavior that consumers rely on with a test against a live database.

## Why

- Hibernate 7.0 migration guide recommends Agroal or HikariCP over c3p0; the 7.4 Introduction uses Agroal throughout. `PersistencePropertiesProvider` references the internal class `org.hibernate.c3p0.internal.C3P0ConnectionProvider`.
- `PersistenceItem` implements every `PersistenceUnitInfo` method by hand, including `getTransactionType()` under `@SuppressWarnings("removal")`. Jakarta Persistence 3.2 adds `PersistenceConfiguration`; Hibernate 7.4 extends it as `HibernatePersistenceConfiguration`.
- A throwaway spike against PostgreSQL 17 (connections counted in `pg_stat_activity` by `ApplicationName`, load through `SqlDatabaseSteps`) measured the three candidate pools:

  | Scenario                                           | c3p0 | Agroal | HikariCP |
  | -------------------------------------------------- | ---- | ------ | -------- |
  | pool 5, 10 threads: peak connections               | 5    | 5      | 5        |
  | pool 100, 10 threads: peak under load              | 15   | 10     | 13       |
  | pool 100, 10 threads: after 8 s idle               | 15   | 10     | **100**  |
  | any scenario: after `EntityManagerFactory.close()` | 0    | 0      | 0        |

  No pool leaked connections, including for reads outside a transaction. HikariCP defaults `minimumIdle` to `maximumPoolSize` and fills the pool in the background regardless of demand; with the module's default `SQL_DATABASE_POOL_SIZE=100` one factory exhausts PostgreSQL's default `max_connections=100`. Agroal (`minSize` 0) grows only on demand, like c3p0. This matches the owner's earlier experience with HikariCP and is the behavior the new test guards against.

## Decisions

| Topic            | Decision                                                                                                                                                               |
| ---------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Pool             | Agroal (`org.hibernate.orm:hibernate-agroal`, versioned with `hibernate`). c3p0 removed.                                                                               |
| Pool test        | Live PostgreSQL from a new Docker Compose profile `sql`, following the repository's existing compose-profile pattern (as `storage` does for MongoDB).                  |
| API policy       | Deprecate and bridge: new `PersistenceConfiguration` path; `PersistenceItem` path keeps working, `@Deprecated(forRemoval = true)`, removed in a later release.         |
| Bridge signature | `DatabaseService.createSqlQuery(jakarta.persistence.PersistenceConfiguration)` takes the Jakarta supertype; q4j's factory returns `HibernatePersistenceConfiguration`. |
| Delivery         | Two PRs from one plan. PR1: pool. PR2: bootstrap, on top of PR1. Both reference `Refs #818`, not `Closes`.                                                             |

## PR1 — c3p0 → Agroal

### Dependencies

- `gradle/libs.versions.toml`: remove `hibernate-c3p0`, add `hibernate-agroal = { module = "org.hibernate.orm:hibernate-agroal", version.ref = "hibernate" }`. If `io.agroal:agroal-pool` is not resolved transitively (check `:data-utils:sql:dependencies --configuration runtimeClasspath`), add it to the catalog and to `implementation`.
- `data-utils/sql/build.gradle`: `implementation libs.hibernate.agroal` replaces `libs.hibernate.c3p0`. Scope stays `implementation`, so consumers keep getting a pool at runtime.
- Test scope: the PostgreSQL JDBC driver (`testRuntimeOnly`), added to the catalog.

### Live database for tests

- `tools/environment/docker/docker-compose.yml`: service `postgres` under profile `sql`, same pinned `postgres` image as `reportportal-postgres`, with a `pg_isready` healthcheck; published port `${POSTGRES_PUBLISHED_PORT:-5432}` in the local override and `${POSTGRES_PUBLISHED_PORT:-0}` in the CI override.
- Hook `tools/environment/scripts/infra/hooks/sql.sh` resolves the published port and writes `POSTGRES_JDBC_URL` to `tools/environment/.postgres.env`; registered in `after_health.sh`, `pre_up_ci.sh` (dynamic port) and `stop_app.sh` (profile list).
- `reusable-module-pipeline.yml` sources `.postgres.env` with the other env files; the `data-utils-sql` job in `test.yml` sets `profile: sql`.
- Tests read `POSTGRES_JDBC_URL`, defaulting to the local compose port. An unreachable database fails the test with a message naming the variable and the compose command; it is never skipped.

### Code

- `PersistencePropertiesProvider`: drop the import of `C3P0ConnectionProvider`. `CONNECTION_PROVIDER` is set as a string: the strategy short name `agroal` if Hibernate registers it (proven by the pool-name assertion in `ConnectionProviderTest`), otherwise the fully qualified class name as a string literal, never an import of an `internal` package. The explicit provider stays for variant 2 so the pool does not depend on what else is on the consumer's classpath.
- Variants 1 (`persistence.xml`) and 3 (manual map) keep auto-detection: Agroal is the only registered `ConnectionProvider`, as c3p0 is today.

### Tests

- `ConnectionProviderTest`: the three scenarios (xml, provider, item) assert Agroal in the `org.hibernate.orm.connections.pooling` log. Exact log lines are taken from a real run. Methods renamed `checkC3P0…` → `checkAgroal…`; `@TmsLink` IDs unchanged. These are characterization tests of the chosen pool, so changing their expectation is intended.
- `ConnectionPoolTest` (new, live PostgreSQL from the `sql` profile, single-threaded). Each test builds its own `EntityManagerFactory` with a unique `ApplicationName` in the JDBC URL and counts that application's rows in `pg_stat_activity` through a separate plain JDBC connection:
  1. **On-demand growth (regression guard for the HikariCP behavior):** pool size 100, load from K threads (K = 10) through `SqlDatabaseSteps` (`save` + `selectDsl(...).fetchFirst()`), then idle for a few seconds. Assert connections ≤ K + 2 after the idle period, never close to 100.
  2. **Upper bound and pool-size wiring:** `hibernate.connection.pool_size` = 5 (the key `SQL_DATABASE_POOL_SIZE` maps to), 10 threads. Assert the sampled peak ≤ 5 and all threads finish without errors within a timeout.
  3. **Release:** after `EntityManagerFactory.close()`, connections = 0.

  The tests bootstrap with a properties map because the database URL comes from the environment and `DatabaseStage` reads a classpath file; the `DatabaseStage` → properties mapping stays covered by `ConnectionProviderTest` on H2.

### PR notes (Migration)

- `hibernate.c3p0.*` properties in `persistence.xml` stop having any effect; use `hibernate.agroal.*`.
- Without `hibernate.connection.pool_size`, Agroal's default maximum is 10. Variant 2 always sets it (`SQL_DATABASE_POOL_SIZE`, default 100).
- Agroal opens connections on demand, so consumers see as many connections as their tests use, not a fixed pool.
- To stay on c3p0: add `org.hibernate.orm:hibernate-c3p0` and set `hibernate.connection.provider_class` explicitly.

## PR2 — `PersistenceItem` → `HibernatePersistenceConfiguration`

### New public path

- `dev.quokkify.persistence.PersistenceConfigurationFactory.create(String name, Map<String, Object> properties)` returns `HibernatePersistenceConfiguration`: Hibernate provider, `jakarta.persistence.PersistenceUnitTransactionType.RESOURCE_LOCAL`, the given properties, and the entity discovery described below. Replaces README variant 3.
- `PersistenceItemProvider.getPersistenceConfiguration(DatabaseStage)` returns the configuration for a stage, cached per stage like `getPersistenceItem`. Replaces README variant 2.
- `DatabaseService.createSqlQuery(jakarta.persistence.PersistenceConfiguration)` calls `configuration.createEntityManagerFactory()` and caches the `SqlFactory` by `configuration.name()`.

### Deprecated path

- `PersistenceItem`, `PersistenceItemProvider.getPersistenceItem(DatabaseStage)` and `DatabaseService.createSqlQuery(PersistenceItem)` get `@Deprecated(since = "<version>", forRemoval = true)` and a Javadoc `@deprecated` pointer to the replacement. `<version>` is the release that ships PR2, read from the repository's release manifest at implementation time.
- `PersistenceItem.toPersistenceConfiguration()` (new, deprecated with the class) builds a `HibernatePersistenceConfiguration` from `getPersistenceUnitName()`, `getProperties()` and `getManagedClassNames()`. Because it calls the overridable `getManagedClassNames()`, subclasses that list their own entities keep working (a known consumer does this). `createSqlQuery(PersistenceItem)` delegates to the new overload through it, so both paths share one cache keyed by name.
- `PersistenceItem` keeps implementing `PersistenceUnitInfo` until removal; dropping the interface earlier would break binary compatibility.
- Variant 1 (`createSqlQuery(String)` with `persistence.xml`) is unchanged.

### Entity discovery

Finding (Task 4 of the plan, 2026-11-07): the premise that classpath-root scanning finds consumer entities is wrong. On the current code an `@Entity` outside `dev.quokkify` fails with `Unknown entity type ... does not belong to this persistence unit`; only the ClassGraph scan of `dev.quokkify` works. A known consumer works around it by subclassing `PersistenceItem` and overriding `getManagedClassNames()`.

Decision (owner, 2026-11-07): discover entities by contract, not by package.

- ClassGraph finds every class annotated `@Entity` that implements `dev.quokkify.entity.SqlEntityInterface`, in all packages on the classpath. The interface belongs to q4j, so unrelated entities from other libraries are not picked up.
- The scan result is computed once per JVM and reused.
- It is the default for both paths: `PersistenceItem.getManagedClassNames()` and `PersistenceConfigurationFactory.create`. This widens discovery for existing `PersistenceItem` users; overrides of `getManagedClassNames()` still win.
- Callers can add classes with `.managedClasses(...)` on the returned `HibernatePersistenceConfiguration`.
- `jarFileUrls` / `rootUrl` are not used by the new path.

### Tests

- `DatabaseSqlTest` and `SqlConverterTest` move to the new API.
- One test keeps exercising the deprecated `PersistenceItem` path so the bridge cannot break silently, and one pins that a subclass override of `getManagedClassNames()` is honored by the bridge.
- A consumer entity outside `dev.quokkify` is usable through both paths without listing it.
- Cache test: a second `createSqlQuery` with the same configuration name returns the same `SqlFactory`.
- The PR1 pool tests move to `PersistenceConfigurationFactory`.

### Docs and PR notes

- `data-utils/sql/README.md`: variants 2 and 3 use the new API; a short note says `PersistenceItem` is deprecated for removal.
- PR Migration section: before/after snippets for variants 2 and 3; note that entities implementing `SqlEntityInterface` are now found in any package, so `getManagedClassNames()` overrides that only work around the old `dev.quokkify` limit can be dropped. No breaking change.

## Out of scope

- Querydsl fork (third #818 item).
- Thread safety of `DatabaseService`'s `HashMap` cache and the never-cleared `ThreadLocal` in `SqlFactory`. The spike showed no effect on pool usage; file separate issues if wanted.
- Removing `PersistenceItem` (a later release).
