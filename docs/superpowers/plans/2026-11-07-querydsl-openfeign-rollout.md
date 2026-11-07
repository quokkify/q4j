# Querydsl OpenFeign Rollout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish moving the quokkify org off `com.querydsl` 5.1.0 onto `io.github.openfeign.querydsl` 7.7, and use the move to add type-safe Querydsl queries to `q4j` `data-utils/nosql/morphia`.

**Rollout (one PR per repository, all `Refs quokkify/q4j#818`):**

| # | Repository | Change | State |
|---|---|---|---|
| 0 | `quokkify/q4j` | `data-utils/sql` → fork (breaking) | done, PR #819 |
| 1 | `quokkify/q4j` | Task 1: `MongoDatabaseSteps.selectQuery(EntityPath<T>)` over `MorphiaQuery<T>`, keeping `selectDsl(Class<T>)` untouched | this plan; branch stacked on #819 |
| 2 | `quokkify/archgate` | Task 2: test fixture dependency `querydsl-core` → fork | this plan; independent of #819 |

Tasks 1 and 2 touch different repositories and can run in parallel. Task 1's PR targets `feat/querydsl-openfeign` until #819 merges, then is retargeted to `main`.

**Architecture:** `io.github.openfeign.querydsl:querydsl-mongodb` ships `com.querydsl.mongodb.morphia.MorphiaQuery<K>` with the constructor `MorphiaQuery(dev.morphia.Datastore, EntityPath<K>)`. The new step wraps the thread-local `Datastore` from `NoSqlFactory`. Q-classes for Morphia entities come from `querydsl-apt` with the `morphia` classifier (`MorphiaAnnotationProcessor` reads `dev.morphia.annotations.Entity`).

**Tech Stack:** Java 21, Gradle version catalog, Morphia 2.5.3, mongodb-driver-sync 5.13.0, Querydsl OpenFeign 7.7, TestNG, AssertJ, Allure.

**Spec:** design approved in chat on 2026-11-07 (bounded change, no spec file). Source: issue #818. This branch sits on top of `feat/querydsl-openfeign` (PR #819), which already sets `querydsl = "7.7"` in the catalog.

## Global Constraints (Task 1, q4j)

- Purely additive: no change to the signature or behavior of `selectDsl(Class<T>)`, `save`, `update`, `delete`, `verify()`.
- New public method, exactly: `public <T extends MongoEntityInterface> MorphiaQuery<T> selectQuery(EntityPath<T> path)` with `@Step("Create Querydsl query for mongo database")`.
- Querydsl version comes from the existing catalog key `querydsl`; no inline coordinates in `build.gradle`.
- `querydsl-mongodb` is an `api` dependency (its types appear in the public API).
- Morphia and the Mongo driver versions stay as they are in the catalog (`morphia = "2.5.3"`, `mongodb-driver = "5.13.0"`).
- Code style: 2-space indent; imports grouped `java.*` / `dev.quokkify.*` / third-party / static; no star imports; checkstyle must pass (`:data-utils:nosql:morphia:check`).
- Commits: Conventional Commits, scope `morphia`; type `feat` (not breaking). PR body says `Refs #818`.

## Review Focus (Task 1, q4j; Task 2 has no new behavior)

- A filter on the inherited `id` (`ObjectId`, declared in main-source-set `Identity`, not in the test entity): the query must match by id. `checkSelectQuery` pins it with `q.id.eq(...)`.
- A filter that matches nothing: `fetch()` returns an empty list and `fetchFirst()` returns `null`, not an exception. `checkSelectQueryNoMatch` pins it.
- Several documents match and the caller uses `fetchFirst()`: a single entity comes back, not an error. `checkSelectQueryNoMatch` does not cover this, so `checkSelectQueryFetchesAll` pins `fetch()` count with two saved documents sharing a `lastName`.
- Mixed driver versions on the classpath (`mongodb-driver-legacy` 5.12 pulled by `querydsl-mongodb` next to `driver-sync` 5.13): the exclusion step and the green test run pin it.
- The query runs on the calling thread's `Datastore` (`getThreadLocalDatastore()`), the same as `selectDsl`; no new state is cached in `MongoDatabaseSteps`.

---

### Task 1: `selectQuery` over `MorphiaQuery`, with Q-class generation and docs

**Files:**
- Modify: `gradle/libs.versions.toml` (QueryDSL block, next to `querydsl-apt`)
- Modify: `data-utils/nosql/morphia/build.gradle`
- Modify: `data-utils/nosql/morphia/src/main/java/dev/quokkify/steps/MongoDatabaseSteps.java`
- Maybe modify (only per Step 4): `data-utils/nosql/morphia/src/main/java/dev/quokkify/entity/nosql/Identity.java`
- Test: `data-utils/nosql/morphia/src/test/java/dev/quokkify/test/DatabaseNoSqlTest.java`
- Docs: `data-utils/nosql/morphia/README.md`

**Interfaces:**
- Consumes: `NoSqlFactory#getThreadLocalDatastore(): dev.morphia.Datastore`; catalog key `querydsl` (7.7); catalog entry `querydsl-apt` (`io.github.openfeign.querydsl:querydsl-apt`); catalog entry `morphia-core`.
- Produces: `MongoDatabaseSteps#selectQuery(EntityPath<T>): MorphiaQuery<T>`; catalog entry `querydsl-mongodb = { module = "io.github.openfeign.querydsl:querydsl-mongodb", version.ref = "querydsl" }`; generated test Q-class `dev.quokkify.entity.nosql.QDatabaseTestUserMongo` with default instance `QDatabaseTestUserMongo.databaseTestUserMongo`.

- [ ] **Step 1: Start a local MongoDB for the module tests**

Run: `docker run -d --rm --name q4j-mongo-morphia -p 27017:27017 mongo:9.0.4`
Expected: container id printed; `nc -z localhost 27017` succeeds. (The tests read `MONGODB_URL`, default `mongodb://localhost:27017`.)

- [ ] **Step 2: Baseline**

Run: `./gradlew :data-utils:nosql:morphia:build --console=plain`
Expected: `BUILD SUCCESSFUL`, existing tests `NO_SQL_DATABASE_ID_1..4` and `NoSqlFactoryTest` green. Record the test count.

- [ ] **Step 3: Write the failing tests in `DatabaseNoSqlTest`**

Add three tests with fresh entities (do not reuse the existing four fields):

```java
@TmsLink("NO_SQL_DATABASE_ID_5")
@Test(description = "Check type-safe select by inherited id and field")
public void checkSelectQuery() {
  DatabaseTestUserMongo user = new DatabaseTestUserMongo("Query", "Dsl");
  mongoDatabaseSteps.save(user);
  QDatabaseTestUserMongo q = QDatabaseTestUserMongo.databaseTestUserMongo;
  DatabaseTestUserMongo actual = mongoDatabaseSteps.selectQuery(q)
      .where(q.id.eq(user.getId()), q.firstName.eq("Query"))
      .fetchFirst();
  Assertions.assertThat(actual).usingRecursiveComparison().isEqualTo(user);
}

@TmsLink("NO_SQL_DATABASE_ID_6")
@Test(description = "Check type-safe select returns nothing for unknown value")
public void checkSelectQueryNoMatch() {
  QDatabaseTestUserMongo q = QDatabaseTestUserMongo.databaseTestUserMongo;
  String unknown = UUID.randomUUID().toString();
  Assertions.assertThat(mongoDatabaseSteps.selectQuery(q).where(q.firstName.eq(unknown)).fetch()).isEmpty();
  Assertions.assertThat(mongoDatabaseSteps.selectQuery(q).where(q.firstName.eq(unknown)).fetchFirst()).isNull();
}

@TmsLink("NO_SQL_DATABASE_ID_7")
@Test(description = "Check type-safe select returns all matching records")
public void checkSelectQueryFetchesAll() {
  String lastName = UUID.randomUUID().toString();
  mongoDatabaseSteps.save(List.of(new DatabaseTestUserMongo("One", lastName), new DatabaseTestUserMongo("Two", lastName)));
  QDatabaseTestUserMongo q = QDatabaseTestUserMongo.databaseTestUserMongo;
  Assertions.assertThat(mongoDatabaseSteps.selectQuery(q).where(q.lastName.eq(lastName)).fetch())
      .extracting(DatabaseTestUserMongo::getFirstName)
      .containsExactlyInAnyOrder("One", "Two");
  Assertions.assertThat(mongoDatabaseSteps.selectQuery(q).where(q.lastName.eq(lastName)).fetchFirst()).isNotNull();
}
```

Imports: `java.util.UUID`, `dev.quokkify.entity.nosql.QDatabaseTestUserMongo`.

- [ ] **Step 4: Wire dependencies and Q-class generation; confirm the tests compile and fail only on the missing method**

- `libs.versions.toml`: add the `querydsl-mongodb` entry from Interfaces under `# QueryDSL`.
- `morphia/build.gradle`:
  - `api(libs.querydsl.mongodb) { exclude group: "org.mongodb", module: "mongodb-driver-legacy" }`
  - `testAnnotationProcessor variantOf(libs.querydsl.apt) { classifier("morphia") }`
  - `testAnnotationProcessor libs.morphia.core`

Run: `./gradlew :data-utils:nosql:morphia:compileTestJava --console=plain`
Expected: `QDatabaseTestUserMongo.java` exists under `build/generated/sources/annotationProcessor/java/test/dev/quokkify/entity/nosql/`, and the only compile error is `cannot find symbol ... selectQuery`.

Branch decision, made here on the observed output:
- If compilation fails because `QIdentity` is missing (the supertype from the main source set was not generated), annotate `Identity` with `@com.querydsl.core.annotations.QuerySupertype` and add `annotationProcessor variantOf(libs.querydsl.apt) { classifier("morphia") }` and `annotationProcessor libs.morphia.core` to the main configuration. Re-run; expected: `QIdentity.java` under `.../java/main/...` and only the `selectQuery` error remains.
- Otherwise leave `Identity` untouched.

- [ ] **Step 5: Implement `selectQuery` in `MongoDatabaseSteps`**

Signature and annotation exactly as in Global Constraints; body constructs `new MorphiaQuery<>(noSqlFactory.getThreadLocalDatastore(), path)`. Javadoc in the style of the existing methods (one sentence, `@param path` the generated Q-class instance, `@param <T>` extends `MongoEntityInterface`).

- [ ] **Step 6: Run the module build**

Run: `./gradlew :data-utils:nosql:morphia:build --console=plain`
Expected: `BUILD SUCCESSFUL`; test count = baseline + 3; `NO_SQL_DATABASE_ID_5..7` pass.

Driver check, decided here: if any test fails with `NoClassDefFoundError`/`ClassNotFoundException` for a `com.mongodb.*` legacy type (for example `com.mongodb.DBObject`), remove the `exclude` from Step 4 and re-run; record in the commit body why the legacy driver stays.

- [ ] **Step 7: Verify the dependency tree and POM**

Run: `./gradlew :data-utils:nosql:morphia:dependencies --configuration runtimeClasspath --console=plain -q | grep -E 'querydsl|org.mongodb|morphia'`
Expected: `io.github.openfeign.querydsl:querydsl-mongodb:7.7`, no `com.querydsl` group, every `org.mongodb:*` artifact resolved to `5.13.0`, a single `morphia-core:2.5.3`.

Run: `./gradlew :data-utils:nosql:morphia:generatePomFileForMavenPublication validateMavenCentralPoms --console=plain -q` then `grep -A3 querydsl-mongodb data-utils/nosql/morphia/build/publications/maven/pom-default.xml`
Expected: `querydsl-mongodb` `7.7` with `<scope>compile</scope>`.

- [ ] **Step 8: Document in `data-utils/nosql/morphia/README.md`**

Add a section "Type-safe queries with Querydsl" after the existing query example:
- the Gradle snippet `testAnnotationProcessor("io.github.openfeign.querydsl:querydsl-apt:7.7:morphia")` and `testAnnotationProcessor("dev.morphia.morphia:morphia-core:2.5.3")`;
- a usage example mirroring `checkSelectQuery` with a `QUser` (`mongoSteps.selectQuery(user).where(user.email.eq("alice@example.com")).fetchFirst()`);
- one sentence that `selectDsl(Class)` with Morphia `Filters` keeps working unchanged.

- [ ] **Step 9: Commit**

```bash
git add gradle/libs.versions.toml data-utils/nosql/morphia docs/superpowers/plans/2026-11-07-querydsl-openfeign-rollout.md
git commit -m "feat(morphia): add type-safe Querydsl queries via selectQuery"
```
Body: what was added, the legacy-driver decision from Step 6, `Refs #818`, and the session's `Co-Authored-By` trailer.

- [ ] **Step 10: Stop the local MongoDB**

Run: `docker stop q4j-mongo-morphia`
Expected: container name printed (it was started with `--rm`, so it is removed).

---

### Task 2: archgate test fixtures on the OpenFeign fork

Repository `quokkify/archgate`. Work in a fresh worktree on a new branch `chore/querydsl-openfeign` created from `origin/main`; do not modify an existing local clone's checkout.

**Context:** `com.querydsl:querydsl-core` 5.1.0 is only a `testImplementation` dependency of `backends/archunit/context` and `backends/archunit/rules`. It compiles the fixtures `QFoo`, `QDate`, `QLogger`, which extend `com.querydsl.core.types.dsl.BeanPath<Object>` through `super(Object.class, "<name>")`. The production rule `GeneratedCode` matches the string `com.querydsl.core.types.dsl.BeanPath`, so it has no dependency on the library. The fork keeps the package and the `BeanPath(Class<? extends T>, String)` constructor, so no Java changes are expected.

**Files:**
- Modify: `gradle/libs.versions.toml` (`querydsl = "5.1.0"` → `"7.7"`; `querydsl-core` module → `io.github.openfeign.querydsl:querydsl-core`)
- No other file changes expected; `backends/archunit/{context,rules}/build.gradle` keep `testImplementation libs.querydsl.core`.

**Interfaces:**
- Consumes: nothing from Task 1.
- Produces: nothing used by other tasks.

- [ ] **Step 1: Baseline**

Run: `./gradlew check --console=plain`
Expected: `BUILD SUCCESSFUL`; record the test count of `:backends:archunit:context:test` and `:backends:archunit:rules:test`.

- [ ] **Step 2: Switch the catalog entry**

Edit the two catalog lines listed under Files.

- [ ] **Step 3: Verify**

Run: `./gradlew check --console=plain`
Expected: `BUILD SUCCESSFUL`, same test counts as Step 1; the generated-code rule tests that use `QFoo`/`QDate`/`QLogger` still pass (they prove `GeneratedCode` still recognizes fork `BeanPath` subclasses).

Run: `./gradlew :backends:archunit:rules:dependencies --configuration testRuntimeClasspath --console=plain -q | grep -i querydsl`
Expected: only `io.github.openfeign.querydsl:querydsl-core:7.7`, no `com.querydsl`.

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml
git commit -m "chore(deps): move Querydsl test fixtures to the OpenFeign fork"
```
Body: `com.querydsl` has had no release since 5.1.0 (January 2024); the fork keeps the `com.querydsl.*` packages, so the fixtures and `GeneratedCode` are unchanged; `Refs quokkify/q4j#818`; the session's `Co-Authored-By` trailer. Test-only dependency, so no release-note impact for archgate users.
