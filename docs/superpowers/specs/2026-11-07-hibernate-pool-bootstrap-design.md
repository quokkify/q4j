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
| Pool test        | Live PostgreSQL through Testcontainers, test scope only.                                                                                                               |
| API policy       | Deprecate and bridge: new `PersistenceConfiguration` path; `PersistenceItem` path keeps working, `@Deprecated(forRemoval = true)`, removed in a later release.         |
| Bridge signature | `DatabaseService.createSqlQuery(jakarta.persistence.PersistenceConfiguration)` takes the Jakarta supertype; q4j's factory returns `HibernatePersistenceConfiguration`. |
| Delivery         | Two PRs from one plan. PR1: pool. PR2: bootstrap, on top of PR1. Both reference `Refs #818`, not `Closes`.                                                             |

## PR1 — c3p0 → Agroal

### Dependencies

- `gradle/libs.versions.toml`: remove `hibernate-c3p0`, add `hibernate-agroal = { module = "org.hibernate.orm:hibernate-agroal", version.ref = "hibernate" }`. If `io.agroal:agroal-pool` is not resolved transitively (check `:data-utils:sql:dependencies --configuration runtimeClasspath`), add it to the catalog and to `implementation`.
- `data-utils/sql/build.gradle`: `implementation libs.hibernate.agroal` replaces `libs.hibernate.c3p0`. Scope stays `implementation`, so consumers keep getting a pool at runtime.
- Test scope: Testcontainers PostgreSQL module and the PostgreSQL JDBC driver, added to the catalog.

### Code

- `PersistencePropertiesProvider`: drop the import of `C3P0ConnectionProvider`. `CONNECTION_PROVIDER` is set as a string: the strategy short name `agroal` if Hibernate registers it (proven by the pool-name assertion in `ConnectionProviderTest`), otherwise the fully qualified class name as a string literal, never an import of an `internal` package. The explicit provider stays for variant 2 so the pool does not depend on what else is on the consumer's classpath.
- Variants 1 (`persistence.xml`) and 3 (manual map) keep auto-detection: Agroal is the only registered `ConnectionProvider`, as c3p0 is today.

### Tests

- `ConnectionProviderTest`: the three scenarios (xml, provider, item) assert Agroal in the `org.hibernate.orm.connections.pooling` log. Exact log lines are taken from a real run. Methods renamed `checkC3P0…` → `checkAgroal…`; `@TmsLink` IDs unchanged. These are characterization tests of the chosen pool, so changing their expectation is intended.
- `ConnectionPoolTest` (new, live PostgreSQL via Testcontainers, one container per class). Each test builds its own `EntityManagerFactory` with a unique `ApplicationName` in the JDBC URL and counts that application's rows in `pg_stat_activity` through a separate plain JDBC connection:
  1. **On-demand growth (regression guard for the HikariCP behavior):** pool size 100, load from K threads (K = 10) through `SqlDatabaseSteps` (`save` + `selectDsl(...).fetchFirst()`), then idle for a few seconds. Assert connections ≤ K + 2 after the idle period, never close to 100.
  2. **Upper bound and pool-size wiring:** `hibernate.connection.pool_size` = 5 (the key `SQL_DATABASE_POOL_SIZE` maps to), 10 threads. Assert the sampled peak ≤ 5 and all threads finish without errors within a timeout.
  3. **Release:** after `EntityManagerFactory.close()`, connections = 0.

  The tests bootstrap with a properties map because the container URL is dynamic and `DatabaseStage` reads a classpath file; the `DatabaseStage` → properties mapping stays covered by `ConnectionProviderTest` on H2.

- Docker is available on GitHub Actions `ubuntu` runners. If the module test job runs on a runner without Docker, the plan adds the requirement to the job rather than skipping the test.

### PR notes (Migration)

- `hibernate.c3p0.*` properties in `persistence.xml` stop having any effect; use `hibernate.agroal.*`.
- Without `hibernate.connection.pool_size`, Agroal's default maximum is 10. Variant 2 always sets it (`SQL_DATABASE_POOL_SIZE`, default 100).
- Agroal opens connections on demand, so consumers see as many connections as their tests use, not a fixed pool.
- To stay on c3p0: add `org.hibernate.orm:hibernate-c3p0` and set `hibernate.connection.provider_class` explicitly.

## PR2 — `PersistenceItem` → `HibernatePersistenceConfiguration`

### New public path

- `dev.quokkify.persistence.PersistenceConfigurationFactory.create(String name, Map<String, Object> properties)` returns `HibernatePersistenceConfiguration`: Hibernate provider, `jakarta.persistence.PersistenceUnitTransactionType.RESOURCE_LOCAL`, the given properties, and the same entity discovery as today (see below). Replaces README variant 3.
- `PersistenceItemProvider.getPersistenceConfiguration(DatabaseStage)` returns the configuration for a stage, cached per stage like `getPersistenceItem`. Replaces README variant 2.
- `DatabaseService.createSqlQuery(jakarta.persistence.PersistenceConfiguration)` calls `configuration.createEntityManagerFactory()` and caches the `SqlFactory` by `configuration.name()`.

### Deprecated path

- `PersistenceItem`, `PersistenceItemProvider.getPersistenceItem(DatabaseStage)` and `DatabaseService.createSqlQuery(PersistenceItem)` get `@Deprecated(since = "<version>", forRemoval = true)` and a Javadoc `@deprecated` pointer to the replacement. `<version>` is the release that ships PR2, read from the repository's release manifest at implementation time.
- `createSqlQuery(PersistenceItem)` converts the item with `PersistenceConfigurationFactory.create(item.getPersistenceUnitName(), <item properties as map>)` and delegates to the new overload, so both paths build the factory the same way and share one cache keyed by name.
- `PersistenceItem` keeps implementing `PersistenceUnitInfo` until removal; dropping the interface earlier would break binary compatibility.
- Variant 1 (`createSqlQuery(String)` with `persistence.xml`) is unchanged.

### Entity discovery (main risk)

Today an entity is found two ways: ClassGraph lists `SqlEntityInterface` + `@Entity` classes under `dev.quokkify`, and Hibernate scans every classpath root returned by `getJarFileUrls()`. The second way is what finds consumer entities in their own packages. `PersistenceConfiguration` has no `excludeUnlistedClasses`; scanning is available only through Hibernate's `rootUrl` / `jarFileUrls`.

1. Before any production change, add a characterization test: an `@Entity` in a package outside `dev.quokkify` is usable through `PersistenceItem` (persist + query).
2. Run the same test through `PersistenceConfigurationFactory`, which passes the ClassGraph list to `managedClasses` and the classpath roots to `jarFileUrls`.
3. If Hibernate does not scan `jarFileUrls` for a `PersistenceConfiguration` bootstrap, the factory instead uses ClassGraph to find all `@Entity` classes in classpath directories (not in jar dependencies) and passes them to `managedClasses`. The PR states which mechanism is used.

### Tests

- `DatabaseSqlTest` and `SqlConverterTest` move to the new API.
- One test keeps exercising the deprecated `PersistenceItem` path so the bridge cannot break silently.
- Cache test: a second `createSqlQuery` with the same configuration name returns the same `SqlFactory`.
- The PR1 pool tests move to `PersistenceConfigurationFactory`.

### Docs and PR notes

- `data-utils/sql/README.md`: variants 2 and 3 use the new API; a short note says `PersistenceItem` is deprecated for removal.
- PR Migration section: before/after snippets for variants 2 and 3. No breaking change.

## Out of scope

- Querydsl fork (third #818 item).
- Thread safety of `DatabaseService`'s `HashMap` cache and the never-cleared `ThreadLocal` in `SqlFactory`. The spike showed no effect on pool usage; file separate issues if wanted.
- Removing `PersistenceItem` (a later release).
