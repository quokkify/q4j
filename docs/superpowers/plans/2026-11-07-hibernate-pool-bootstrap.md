# Hibernate Pool and Bootstrap Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace c3p0 with Agroal in `dev.quokkify:sql`, guard pool behavior with a live-PostgreSQL test, and add a `PersistenceConfiguration` bootstrap path while deprecating `PersistenceItem`.

**Architecture:** PR1 swaps the pool dependency, adds a compose profile `sql` with PostgreSQL, and adds `ConnectionPoolTest` that counts connections in `pg_stat_activity`. PR2 adds `PersistenceConfigurationFactory` (returns `HibernatePersistenceConfiguration`) and `DatabaseService.createSqlQuery(PersistenceConfiguration)`; the deprecated `PersistenceItem` path converts to the new one, and both share the name-keyed cache.

**Tech Stack:** Java 21, Gradle version catalog, Hibernate ORM 7.4 (`hibernate-core`, `hibernate-agroal`), Jakarta Persistence 3.2, ClassGraph, TestNG + AssertJ, H2 (existing tests), PostgreSQL (pool tests), Docker Compose.

**Spec:** `docs/superpowers/specs/2026-11-07-hibernate-pool-bootstrap-design.md`

**Branches:** PR1 on `feat/818-agroal-pool` (from `main`, already holds the spec and this plan). PR2 on `feat/818-persistence-configuration`, created from `feat/818-agroal-pool` after Task 3 and targeting it until PR1 merges, then rebased onto `main`.

## Global Constraints

- Module: `:data-utils:sql`, packages `dev.quokkify.*`. Public API changes are only the ones named in this plan.
- Pool: `org.hibernate.orm:hibernate-agroal`, catalog alias `hibernate-agroal`, `version.ref = "hibernate"`. No reference to any `org.hibernate.*.internal.*` class in main code.
- Pool tests: PostgreSQL from compose profile `sql`; URL from `POSTGRES_JDBC_URL`, default `jdbc:postgresql://localhost:5432/q4j`, user `q4j`, password `q4j`, database `q4j`. Unreachable database = test failure, never a skip.
- Deprecations: `@Deprecated(since = "<version>", forRemoval = true)` where `<version>` is the release that ships PR2, read from the repository's release manifest at implementation time; Javadoc `@deprecated` names the replacement.
- Issue linkage: both PR bodies say `Refs #818`, never `Closes`.
- Style: 2-space indent; imports grouped `java.*` / `dev.quokkify.*` / third-party / static; no star imports; `./gradlew :data-utils:sql:check` (checkstyle + ArchGate) passes.
- Markdown: run `npx prettier --write` on every changed `.md` before commit (CI runs `prettier --check .`).
- Commits: Conventional Commits; each ends with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. No machine paths in committed files.
- PR bodies fill the Highlight / Usage example / Migration sections used by the release-notes tooling.

## Review Focus

- PostgreSQL not running locally: `ConnectionPoolTest` must fail fast (≤ 10 s) with a message naming `POSTGRES_JDBC_URL` and `docker compose ... --profile sql up -d`, not hang or skip. Pinned in Task 2 `@BeforeClass` precondition.
- Parallel test classes in the module share the database: counts must be isolated per test through `ApplicationName`, and the class runs `@SingleThread`. Pinned in Task 2 (every scenario filters by its own `ApplicationName`).
- Same name through the deprecated and the new path: both return the same `SqlFactory`, not two factories with two pools. Pinned in Task 6 `checkDeprecatedAndNewPathShareCache`.
- A consumer entity outside `dev.quokkify` keeps working after the switch to the new bootstrap. Pinned in Task 4 (characterization) and Task 5 (new path).
- A second configuration with an already used name but different properties returns the cached factory (current behavior, kept and documented in Javadoc). Pinned in Task 5 `checkSameNameReturnsCachedFactory`.

---

## PR1 — c3p0 → Agroal

### Task 1: Compose profile `sql` with PostgreSQL

**Files:**

- Modify: `tools/environment/docker/docker-compose.yml` (new service `postgres`)
- Modify: `tools/environment/docker/docker-compose.local.yml`, `tools/environment/docker/docker-compose.ci.yml` (ports)
- Create: `tools/environment/scripts/infra/hooks/sql.sh`
- Modify: `tools/environment/scripts/infra/after_health.sh`, `tools/environment/scripts/infra/hooks/pre_up_ci.sh`, `tools/environment/scripts/infra/stop_app.sh`
- Modify: `.github/workflows/reusable-module-pipeline.yml` (env-file loop), `.github/workflows/test.yml` (`data-utils-sql` job)

**Interfaces:**

- Produces: compose profile `sql`; env file `tools/environment/.postgres.env` with `POSTGRES_JDBC_URL=jdbc:postgresql://<host>:<port>/q4j`.

- [ ] **Step 1: Add the service.** In `docker-compose.yml`, service `postgres`: `profiles: [sql]`, image copied verbatim (tag + digest) from `reportportal-postgres`, `POSTGRES_USER/PASSWORD/DB` = `q4j`, no volume, healthcheck `pg_isready -U q4j -d q4j` (interval 2s, retries 30). Local override: `"${POSTGRES_PUBLISHED_PORT:-5432}:5432"`. CI override: `"${POSTGRES_PUBLISHED_PORT:-0}:5432"`.
- [ ] **Step 2: Add `hooks/sql.sh`**, modeled on `hooks/storage.sh`: resolve port with `resolve_published_port postgres 5432 5432 "${require_port}"`, host with `select_runtime_host_for_port`, write `POSTGRES_JDBC_URL=jdbc:postgresql://${host}:${port}/q4j` to `tools/environment/.postgres.env`. `chmod +x`.
- [ ] **Step 3: Register the profile.** `after_health.sh`: `sql)` case calling the hook. `pre_up_ci.sh`: `sql) export POSTGRES_PUBLISHED_PORT=0 ;;`. `stop_app.sh`: add `--profile sql` to the `all` list. `reusable-module-pipeline.yml`: add `postgres` to the `for env_file in …` list. `test.yml` job `data-utils-sql`: add `profile: sql`.
- [ ] **Step 4: Verify locally.**
      Run: `docker compose -f tools/environment/docker/docker-compose.yml -f tools/environment/docker/docker-compose.local.yml --profile sql up -d --wait && tools/environment/scripts/infra/after_health.sh sql && cat tools/environment/.postgres.env`
      Expected: `POSTGRES_JDBC_URL=jdbc:postgresql://localhost:5432/q4j`. Then `shellcheck tools/environment/scripts/infra/hooks/sql.sh` is clean (if `shellcheck` is installed; otherwise note it). Leave the container running for Task 2.
- [ ] **Step 5: Commit** — `ci: add sql compose profile with PostgreSQL for pool tests`.

### Task 2: Live pool test and switch to Agroal

**Files:**

- Modify: `gradle/libs.versions.toml` (remove `hibernate-c3p0`; add `hibernate-agroal`, `postgresql` driver; add `agroal-pool` only if Step 4 shows it missing)
- Modify: `data-utils/sql/build.gradle`
- Modify: `data-utils/sql/src/main/java/dev/quokkify/provider/PersistencePropertiesProvider.java:16,50`
- Create: `data-utils/sql/src/test/java/dev/quokkify/test/ConnectionPoolTest.java`
- Modify: `data-utils/sql/src/test/java/dev/quokkify/test/ConnectionProviderTest.java`
- Modify: `data-utils/sql/README.md` (env table)

**Interfaces:**

- Consumes: `POSTGRES_JDBC_URL` (Task 1).
- Produces: `ConnectionPoolTest` helpers reused in Task 6: `private SqlFactory bootstrap(String applicationName, int poolSize)` and `private static int connections(String applicationName)`.

- [ ] **Step 1: Write `ConnectionPoolTest`** (class-level `@SingleThread`, `@Listeners(SuiteListener.class)` like `BaseDatabaseTest`). Each test uses a unique `ApplicationName` (`"q4j-pool-" + UUID`), appended to the URL as `?ApplicationName=…&connectTimeout=5`, and bootstraps through `DatabaseService.getInstance().createSqlQuery(new PersistenceItem(name, props))` with JDBC driver/url/user/password, `AvailableSettings.POOL_SIZE` and `HBM2DDL_AUTO=update`. `connections(app)` counts `pg_stat_activity` rows with that `application_name` over a separate `DriverManager` connection. Load = K threads each running 10 × (`steps.save(new DatabaseTestUser(...))` + `steps.selectDsl(q).where(q.firstName.isNotNull()).fetchFirst()`), a sampler thread records the peak every 200 ms. Close the factory's `EntityManagerFactory` in `@AfterMethod(alwaysRun = true)` via `sqlFactory.getThreadLocalEntityManager().getEntityManagerFactory().close()` (no new production accessor).
  - `@BeforeClass(alwaysRun = true)` precondition: open one connection; on `SQLException` fail with `"PostgreSQL is not reachable at " + url + ". Set POSTGRES_JDBC_URL or run: docker compose -f tools/environment/docker/docker-compose.yml -f tools/environment/docker/docker-compose.local.yml --profile sql up -d"`.
  - `checkPoolGrowsOnDemandOnly`: pool 100, K = 10, then sleep 8 s. `assertThat(connections(app)).as("idle connections after load from 10 threads").isLessThanOrEqualTo(12)`.
  - `checkPoolNeverExceedsMaxSize`: pool 5, K = 10, threads joined with a 60 s timeout. `assertThat(peak).isLessThanOrEqualTo(5)`; `assertThat(errors).isEmpty()`; `assertThat(finishedThreads).isEqualTo(10)`.
  - `checkPoolReleasesConnectionsOnClose`: pool 5, K = 3; after closing the `EntityManagerFactory` and waiting up to 5 s (`Waiter`/Awaitility), `connections(app) == 0`.
- [ ] **Step 2: Add the driver and run the test on c3p0.** Catalog `postgresql = { module = "org.postgresql:postgresql", version = "<latest 42.x>" }`, `testRuntimeOnly libs.postgresql`.
      Run: `./gradlew :data-utils:sql:test --tests 'dev.quokkify.test.ConnectionPoolTest'`
      Expected: `checkPoolGrowsOnDemandOnly` FAILS (c3p0 holds about 15 connections, the spike measured 15); the other two pass.
- [ ] **Step 3: Switch to Agroal.** Catalog: remove `hibernate-c3p0`, add `hibernate-agroal`. `build.gradle`: `implementation libs.hibernate.agroal`. `PersistencePropertiesProvider`: remove the `C3P0ConnectionProvider` import; `CONNECTION_PROVIDER` → `"agroal"` (a private constant).
- [ ] **Step 4: Check the runtime classpath.**
      Run: `./gradlew :data-utils:sql:dependencies --configuration runtimeClasspath | grep -i agroal`
      Expected: `io.agroal:agroal-pool` present. If missing, add it to the catalog and `implementation`.
- [ ] **Step 5: Run the pool test on Agroal.** Same command as Step 2. Expected: 3/3 PASS. If Hibernate rejects the short name `agroal` (`Unable to resolve name [agroal]`), use the string `"org.hibernate.agroal.internal.AgroalConnectionProvider"` instead and rerun.
- [ ] **Step 6: Update `ConnectionProviderTest`.** Run it once, copy the two pool lines Agroal logs to `org.hibernate.orm.connections.pooling` into `sequences`, rename methods `checkC3P0ProviderVia*` → `checkAgroalProviderVia*`, keep `@TmsLink` values.
      Run: `./gradlew :data-utils:sql:test` — Expected: all PASS.
- [ ] **Step 7: README.** Add row `SQL_DATABASE_POOL_SIZE` | `100` | `Maximum connections in the pool (Agroal)` to the env table. Prettier.
- [ ] **Step 8: Commit** — `feat(sql): replace c3p0 with Agroal connection pool`. Body: spike result in one sentence (HikariCP fills the pool to max; Agroal grows on demand) and `Refs #818`.

### Task 3: PR1 handoff

- [ ] **Step 1:** `./gradlew :data-utils:sql:check :data-utils:sql:test` and `npx prettier --check .` — all green. Stop the compose stack (`tools/environment/scripts/infra/stop_app.sh sql`).
- [ ] **Step 2:** Use superpowers:finishing-a-development-branch. PR title `feat(sql): replace c3p0 with Agroal connection pool`. Migration section, verbatim points from the spec's "PR notes (Migration)". `Refs #818`.

---

## PR2 — `PersistenceItem` → `HibernatePersistenceConfiguration`

### Task 4: Characterize consumer-entity discovery (no production change)

**Files:**

- Create: `data-utils/sql/src/test/java/org/example/entity/ConsumerEntity.java`
- Create: `data-utils/sql/src/test/java/dev/quokkify/test/ConsumerEntityDiscoveryTest.java`

**Interfaces:**

- Produces: `org.example.entity.ConsumerEntity` (`@Entity`, implements `SqlEntityInterface`, `@Id @GeneratedValue Long id`, `String name`, constructor `(String name)`), Q-class `QConsumerEntity` generated by `querydsl-apt`.

- [ ] **Step 1: Write `checkConsumerEntityViaPersistenceItem`** (extends `BaseDatabaseTest`): `createSqlQuery(new PersistenceItem("consumer-" + UUID, getH2Properties()))`, save `new ConsumerEntity("acme")`, select by `name.eq("acme")`, `assertThat(found).isNotNull()`.
- [ ] **Step 2: Run it.** `./gradlew :data-utils:sql:test --tests 'dev.quokkify.test.ConsumerEntityDiscoveryTest'` — Expected: PASS. If it FAILS, stop: the spec's premise about classpath scanning is wrong; report to the owner before continuing.
- [ ] **Step 3: Commit** — `test(sql): characterize discovery of consumer entities outside dev.quokkify`.

### Task 5: `PersistenceConfigurationFactory` and the new `DatabaseService` overload

**Files:**

- Create: `data-utils/sql/src/main/java/dev/quokkify/persistence/PersistenceClasspath.java` (package-private)
- Create: `data-utils/sql/src/main/java/dev/quokkify/persistence/PersistenceConfigurationFactory.java`
- Modify: `data-utils/sql/src/main/java/dev/quokkify/persistence/PersistenceItem.java` (`getManagedClassNames`, `getJarFileUrls` delegate to `PersistenceClasspath`)
- Modify: `data-utils/sql/src/main/java/dev/quokkify/service/DatabaseService.java`
- Modify: `data-utils/sql/src/test/java/dev/quokkify/test/ConsumerEntityDiscoveryTest.java`
- Create: `data-utils/sql/src/test/java/dev/quokkify/test/PersistenceConfigurationTest.java`

**Interfaces:**

- Produces:
  - `final class PersistenceClasspath` — `static List<String> entityClassNames()` (today's ClassGraph query, unchanged), `static List<URL> roots()` (today's `getResources("")`, `IOException` → `UncheckedIOException`).
  - `public final class PersistenceConfigurationFactory` — `public static HibernatePersistenceConfiguration create(String name, Map<String, Object> properties)`; `Objects.requireNonNull` on both.
  - `DatabaseService` — `public SqlFactory createSqlQuery(jakarta.persistence.PersistenceConfiguration configuration)`.

- [ ] **Step 1: Write failing tests.**
  - `ConsumerEntityDiscoveryTest.checkConsumerEntityViaPersistenceConfiguration`: as Task 4 but `createSqlQuery(PersistenceConfigurationFactory.create(name, getH2Properties()))`.
  - `PersistenceConfigurationTest.checkFactoryBuildsResourceLocalHibernateConfiguration`: `create("cfg", props)` → `name()` is `"cfg"`, `provider()` is `HibernatePersistenceProvider.class.getName()`, `transactionType()` is `jakarta.persistence.PersistenceUnitTransactionType.RESOURCE_LOCAL`, `properties()` contains every entry of `props`, `managedClasses()` contains `DatabaseTestUser.class`.
  - `PersistenceConfigurationTest.checkSameNameReturnsCachedFactory`: two `createSqlQuery(create(sameName, props))` calls return the same `SqlFactory` instance (`isSameAs`).
  - `PersistenceConfigurationTest.checkNullArgumentsRejected`: `create(null, props)` and `create("x", null)` throw `NullPointerException`.
- [ ] **Step 2: Run** `./gradlew :data-utils:sql:test --tests 'dev.quokkify.test.PersistenceConfigurationTest' --tests 'dev.quokkify.test.ConsumerEntityDiscoveryTest'` — Expected: compilation FAILS (`PersistenceConfigurationFactory` not found).
- [ ] **Step 3: Implement `PersistenceClasspath`** by moving the two bodies out of `PersistenceItem`; `PersistenceItem` delegates. Run Task 4's test — still PASS.
- [ ] **Step 4: Implement `create`**: `new HibernatePersistenceConfiguration(name)`, `.provider(HibernatePersistenceProvider.class.getName())`, `.transactionType(RESOURCE_LOCAL)`, `.properties(properties)`, managed classes = `entityClassNames()` loaded via the thread context class loader, `.jarFileUrls(roots())`.
- [ ] **Step 5: Implement `createSqlQuery(PersistenceConfiguration)`**: if the name is cached return it, else `new SqlFactory(configuration.createEntityManagerFactory())` and cache by `configuration.name()`. Javadoc states that an already used name returns the cached factory and ignores the new configuration.
- [ ] **Step 6: Run the Step 2 command.** Expected: all PASS. If only `checkConsumerEntityViaPersistenceConfiguration` FAILS (unknown entity `ConsumerEntity`), Hibernate does not scan `jarFileUrls` for this bootstrap: change `PersistenceClasspath` to add an `allEntityClassNames()` that uses ClassGraph over classpath directories only (`.enableAnnotationInfo()`, `.overrideClasspath(roots())`, all `@Entity` classes), use it in `create`, and rerun. Record which mechanism passed for the PR body.
- [ ] **Step 7: Run the module suite** `./gradlew :data-utils:sql:test` — Expected: all PASS.
- [ ] **Step 8: Commit** — `feat(sql): bootstrap through HibernatePersistenceConfiguration`.

### Task 6: Deprecate `PersistenceItem`, bridge, migrate tests and README

**Files:**

- Modify: `data-utils/sql/src/main/java/dev/quokkify/persistence/PersistenceItem.java`
- Modify: `data-utils/sql/src/main/java/dev/quokkify/provider/PersistenceItemProvider.java`
- Modify: `data-utils/sql/src/main/java/dev/quokkify/service/DatabaseService.java`
- Modify: `data-utils/sql/src/test/java/dev/quokkify/test/DatabaseSqlTest.java`, `SqlConverterTest.java`, `ConnectionPoolTest.java`, `ConnectionProviderTest.java`, `PersistenceConfigurationTest.java`
- Modify: `data-utils/sql/README.md`

**Interfaces:**

- Consumes: `PersistenceConfigurationFactory.create`, `DatabaseService.createSqlQuery(PersistenceConfiguration)` (Task 5).
- Produces: `PersistenceItemProvider.getPersistenceConfiguration(DatabaseStage)` → `HibernatePersistenceConfiguration`, cached per stage in its own `ConcurrentHashMap`, built from `stage.getProjectName()` and `PersistencePropertiesProvider.getPersistenceProperties(stage)`.

- [ ] **Step 1: Write failing tests** in `PersistenceConfigurationTest`:
  - `checkProviderBuildsConfigurationForStage`: `getPersistenceConfiguration(stage)` → `name()` equals `"quokkify"`, properties contain `AvailableSettings.POOL_SIZE` → `"100"`; a second call returns the same instance.
  - `checkDeprecatedAndNewPathShareCache`: `createSqlQuery(new PersistenceItem(n, props))` and `createSqlQuery(create(n, props))` return the same `SqlFactory` (both orders, two distinct names).
- [ ] **Step 2: Run** — Expected: compilation FAILS (`getPersistenceConfiguration` not found).
- [ ] **Step 3: Implement.** Add `getPersistenceConfiguration`. Rewrite `createSqlQuery(PersistenceItem)` as `return createSqlQuery(PersistenceConfigurationFactory.create(item.getPersistenceUnitName(), <item.getProperties() as Map<String, Object>>))`, removing the provider-resolver code. Annotate `PersistenceItem`, `getPersistenceItem`, `createSqlQuery(PersistenceItem)` with the Global Constraints deprecation and Javadoc pointers (`PersistenceConfigurationFactory#create`, `PersistenceItemProvider#getPersistenceConfiguration`, `DatabaseService#createSqlQuery(PersistenceConfiguration)`).
- [ ] **Step 4: Migrate tests.** `DatabaseSqlTest`, `SqlConverterTest`, `ConnectionPoolTest` and `ConnectionProviderTest` scenarios "via Provider" use the new API; `ConnectionProviderTest.checkAgroalProviderViaPersistenceItem` and `ConsumerEntityDiscoveryTest.checkConsumerEntityViaPersistenceItem` stay on `PersistenceItem` (with `@SuppressWarnings("removal")`) to guard the bridge.
- [ ] **Step 5: Run** `./gradlew :data-utils:sql:check :data-utils:sql:test` with the `sql` profile up — Expected: all PASS, no new compiler warnings outside the two guarded tests.
- [ ] **Step 6: README.** Variant 2 uses `PersistenceItemProvider.getPersistenceConfiguration(stage)`; variant 3 uses `PersistenceConfigurationFactory.create("my-persistence-unit", Map.of(...))`; one note line: `PersistenceItem` and `getPersistenceItem` are deprecated for removal. Prettier.
- [ ] **Step 7: Commit** — `feat(sql): deprecate PersistenceItem in favor of PersistenceConfiguration`.

### Task 7: PR2 handoff

- [ ] **Step 1:** `./gradlew :data-utils:sql:check :data-utils:sql:test` and `npx prettier --check .` — green. Stop the compose stack.
- [ ] **Step 2:** Use superpowers:finishing-a-development-branch. PR title `feat(sql): bootstrap through HibernatePersistenceConfiguration`, base `feat/818-agroal-pool` (or `main` if PR1 merged). Body: which entity-discovery mechanism passed (Task 5 Step 6), Migration with before/after snippets for variants 2 and 3, "no breaking change", `Refs #818`.
