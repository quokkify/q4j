# MongoDB Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename `dev.quokkify:morphia` to `dev.quokkify:morphia-legacy` (deprecated for removal) and add `dev.quokkify:mongodb`, a MongoDB test module built on `mongodb-driver-sync` + POJO codecs + Querydsl's document API, with no Morphia and no `mongodb-driver-legacy` on its classpath.

**Architecture:** A thin wrapper over the driver in the shape of the legacy module. `MongoFactory` maps `@MongoDocument` classes to `MongoCollection<T>` with a POJO `CodecRegistry`. `MongoDatabaseSteps` does insert/update/delete by `_id`. `MongoQuery<T>` adapts `AbstractFetchableMongodbQuery` with a serializer that maps `@BsonId`/`@BsonProperty` to document keys. Verification is a copy of the legacy fluent verifier.

**Tech Stack:** Java 21, Gradle version catalog, `org.mongodb:mongodb-driver-sync` 5.13.0, `io.github.openfeign.querydsl:querydsl-mongodb` / `querydsl-apt` 7.7, TestNG, AssertJ, Allure 3 (AspectJ weaving through `gradle/aspectj.gradle`), MongoDB 9.0.4 for tests.

**Spec:** `docs/superpowers/specs/2026-11-07-mongodb-module-design.md`

**Branch:** `feat/mongodb-module`. It is stacked on `feat/morphia-querydsl` (PR #820, which contains #819) plus a cherry-pick of #822 (`fix/morphia-mongodb-legacy-alignment`), so the catalog already has `querydsl = "7.7"`, `querydsl-mongodb`, `querydsl-apt` and `mongodb-driver-legacy`. The PR targets `feat/morphia-querydsl` and is rebased onto `main` after #819, #820 and #822 merge.

## Global Constraints

- New module: directory `data-utils/nosql/mongodb`, Gradle `:data-utils:nosql:mongodb`, artifactId `mongodb`, displayName `Q4J MongoDB`, description `MongoDB persistence, type-safe query and verification helpers for automated tests (no Morphia).`, packages `dev.quokkify.mongodb.*`.
- Legacy module: directory `data-utils/nosql/morphia-legacy`, Gradle `:data-utils:nosql:morphia-legacy`, artifactId `morphia-legacy`, displayName `Q4J Morphia (legacy)`. Java packages and behavior unchanged.
- New module dependencies, exactly: `api libs.mongodb.driver.sync`; `api(libs.querydsl.mongodb) { exclude group: "org.mongodb", module: "mongodb-driver-legacy" }`; `api libs.allure.java.commons`; `implementation project(":common-utils:awaitility")`; `implementation project(":testng-extensions")`; `testAnnotationProcessor variantOf(libs.querydsl.apt) { classifier("general") }`. No other new dependencies, test dependencies included.
- Entity contract: `@dev.quokkify.mongodb.annotation.MongoDocument("<collection>")` (required) + `@com.querydsl.core.annotations.QueryEntity` + `@org.bson.codecs.pojo.annotations.BsonId` on the id field.
- Error messages, verbatim: `<FQCN> is not annotated with @MongoDocument`; `<FQCN> has no _id; save it first`; `DBRef references are not supported`; `update requires at least one update operation`.
- Tests: TestNG + AssertJ against MongoDB at `System.getenv().getOrDefault("MONGODB_URL", "mongodb://localhost:27017")`, database `q4j_mongodb_test`. Each test class empties its collections in `@BeforeMethod(alwaysRun = true)`. Locally: `docker run -d --rm --name q4j-mongo -p 27017:27017 mongo:9.0.4`, and `docker stop q4j-mongo` when done.
- Style: 2-space indent; imports grouped `java.*` / `dev.quokkify.*` / third-party / static; no star imports; line length ≤ 139; checkstyle and ArchGate (`check`) must pass.
- Commits: Conventional Commits; each ends with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. No machine paths in any committed file.

## Review Focus

- The same entity is saved twice: the second `save` must fail loudly (driver `MongoWriteException`, duplicate key), not silently create a second document. Pinned in Task 4 `checkSaveTwiceFails`.
- `update(entity)` with no update operations: fail fast with `IllegalArgumentException("update requires at least one update operation")` before touching the server. Pinned in Task 4 `checkUpdateWithoutOperationsFails`.
- A query on a class without `@MongoDocument`: the factory's `IllegalArgumentException` naming the class, not an NPE. Pinned in Task 3 `checkQueryOnUnannotatedClassFails`.
- `@BsonProperty` on a field of an embedded object (`address.zip_code`): the query key must use the stored name. Pinned in Task 3 `checkFilterByRenamedNestedField`.
- `fetchOne()` with several matches: Querydsl's `NonUniqueResultException`, not an arbitrary document. Pinned in Task 3 `checkFetchOneWithSeveralMatchesFails`.

---

### Task 1: Rename `morphia` to `morphia-legacy` and deprecate it

**Files:**

- Move: `data-utils/nosql/morphia/**` → `data-utils/nosql/morphia-legacy/**` (`git mv`)
- Modify: `settings.gradle` (line `include(":data-utils:nosql:morphia")` and its comment → `morphia-legacy`, comment `// MongoDB utilities (Morphia, deprecated: use :data-utils:nosql:mongodb)`)
- Modify: `gradle/module-metadata.gradle` (`':data-utils:nosql:morphia'` entry → `':data-utils:nosql:morphia-legacy'`, artifactId/displayName per Global Constraints, description `Deprecated MongoDB and Morphia helpers; use dev.quokkify:mongodb.`)
- Modify: `.github/workflows/test.yml` (job `data-utils-nosql-morphia` → `data-utils-nosql-morphia-legacy`: `name: "Data • morphia-legacy"`, `name: morphia-legacy`, `module: ":data-utils:nosql:morphia-legacy"`, `slug: data-utils-nosql-morphia-legacy`; the `tests-complete.needs` entry likewise)
- Modify: `.github/workflows/reusable-module-matrices.yml` (`dataBuildModules` morphia entry → `{ name: 'morphia-legacy', module: ':data-utils:nosql:morphia-legacy', slug: 'data-utils-nosql-morphia-legacy' }`)
- Modify: the 8 public types under `data-utils/nosql/morphia-legacy/src/main/java/dev/quokkify/` (`entity/nosql/Identity`, `entity/nosql/MongoEntityInterface`, `service/NoSqlFactory`, `steps/AbstractMongoSteps`, `steps/MongoDatabaseSteps`, `verification/BaseMongoVerification`, `verification/MongoVerification`, `verification/MongoVerifier`)
- Create: `data-utils/nosql/morphia-legacy/src/test/resources/META-INF/services/dev.quokkify.archgate.api.ArchitectureRule`
- Modify: `data-utils/nosql/morphia-legacy/README.md`, root `README.md` (module table row `morphia`)
- Any other reference: `git grep -n -e 'nosql:morphia' -e 'nosql/morphia' -e 'dev.quokkify:morphia'` must return only `morphia-legacy` hits afterwards. Existing `CHANGELOG.md` entries stay unchanged.

**Interfaces:**

- Produces: Gradle project `:data-utils:nosql:morphia-legacy`, artifactId `morphia-legacy`; public API unchanged apart from the deprecation annotations.

- [ ] **Step 1: Baseline.** Start MongoDB (Global Constraints). Run `./gradlew :data-utils:nosql:morphia:build --console=plain`. Expected: `BUILD SUCCESSFUL`. Record the test counts (expected 11: `NoSqlFactoryTest` 4, `DatabaseNoSqlTest` 7).
- [ ] **Step 2: Move and re-register.** Apply the moves and the `settings.gradle`, `module-metadata.gradle` and CI workflow edits listed under Files.
- [ ] **Step 3: Deprecate.** Annotate each of the 8 public types with `@Deprecated(since = "<V>", forRemoval = true)`, where `<V>` is the version in `.github/release-please/manifest.json` with the minor bumped (for example `0.13.0` gives `since = "0.14.0"`). Add Javadoc `@deprecated Use {@code dev.quokkify:mongodb} ({@code dev.quokkify.mongodb.*}); this module will be removed.`.
- [ ] **Step 4: ArchGate list.** The shared list `tools/archgate/META-INF/services/dev.quokkify.archgate.api.ArchitectureRule` includes `dev.quokkify.archgate.rules.archunit.NoDeprecatedApiUsageRule`, which fails as soon as the module's own code uses its newly deprecated types. Create the module's own list as a copy of the shared list, with that one line replaced by `# omitted: dev.quokkify.archgate.rules.archunit.NoDeprecatedApiUsageRule the module deprecates its own public API for removal` (format required by `gradle/archgate.gradle` `verifyArchgateRuleList`).
- [ ] **Step 5: Docs.**
  - Legacy README: put a first-line banner `> **Deprecated.** Use [\`dev.quokkify:mongodb\`](../mongodb/README.md); this module will be removed in a future release.`, add the spec's migration table (section "Migrating to dev.quokkify:mongodb", copied verbatim from the spec), and change the installation coordinate to `dev.quokkify:morphia-legacy`.
  - Root `README.md` module table: `morphia` → `morphia-legacy` with description `Deprecated Morphia helpers; use mongodb`.
- [ ] **Step 6: Verify.** Run `./gradlew :data-utils:nosql:morphia-legacy:build validateMavenCentralPoms --console=plain`. Expected: `BUILD SUCCESSFUL`, same test counts as Step 1, `verifyArchgateRuleList` and `archgateCheck` pass. Run `git grep -n -e 'nosql:morphia\b' -e 'nosql/morphia/' -e 'dev.quokkify:morphia:'`; expected: no hits outside `CHANGELOG.md`.
- [ ] **Step 7: Commit** `refactor(morphia)!: rename morphia to morphia-legacy and deprecate it`, with body footer `BREAKING CHANGE: dev.quokkify:morphia is renamed to dev.quokkify:morphia-legacy; switch the coordinate or migrate to dev.quokkify:mongodb.`

### Task 2: New module skeleton, `@MongoDocument`, `MongoFactory`, classpath guard

**Files:**

- Create: `data-utils/nosql/mongodb/build.gradle` (dependencies exactly as in Global Constraints)
- Modify: `settings.gradle` (`include(":data-utils:nosql:mongodb")             // MongoDB utilities (driver + Querydsl, no Morphia)`, placed before morphia-legacy)
- Modify: `gradle/module-metadata.gradle` (new entry per Global Constraints)
- Modify: `.github/workflows/test.yml` (new job `data-utils-nosql-mongodb`, a copy of the morphia-legacy job with name/module/slug `mongodb` / `:data-utils:nosql:mongodb` / `data-utils-nosql-mongodb`, `profile: storage`; add it to `tests-complete.needs`)
- Modify: `.github/workflows/reusable-module-matrices.yml` (`dataBuildModules`: `{ name: 'mongodb', module: ':data-utils:nosql:mongodb', slug: 'data-utils-nosql-mongodb' }`)
- Create: `src/main/java/dev/quokkify/mongodb/annotation/MongoDocument.java`
- Create: `src/main/java/dev/quokkify/mongodb/service/MongoFactory.java`
- Create: `src/test/java/dev/quokkify/mongodb/entity/TestUser.java`, `Address.java`, `Role.java` (test entities used by Tasks 2-4)
- Create: `src/test/java/dev/quokkify/mongodb/test/MongoFactoryTest.java`, `ClasspathGuardTest.java`
- Create: `src/test/resources/log4j2.xml` (copy from the legacy module)

(Paths under `src/` are relative to `data-utils/nosql/mongodb/`.)

**Interfaces:**

- Produces:
  - `@Retention(RUNTIME) @Target(TYPE) public @interface MongoDocument { String value(); }`
  - `public final class MongoFactory`:
    - `MongoFactory(MongoClient client, String databaseName)`
    - `MongoFactory(MongoClient client, String databaseName, CodecRegistry codecRegistry)`
    - `<T> MongoCollection<T> collection(Class<T> type)`
    - `MongoCollection<Document> documents(Class<?> type)`: same collection, `Document`-typed, same registry
    - `CodecRegistry codecRegistry()`
  - Test entities (all with public no-arg constructors and getters/setters):
    - `@MongoDocument("test_users") @QueryEntity TestUser { @BsonId ObjectId id; @BsonProperty("first_name") String firstName; String lastName; int age; Role role; Address address; }`
    - `@QueryEmbeddable Address { String city; @BsonProperty("zip_code") String zipCode; }`
    - `enum Role { ADMIN, USER }`
    - plus, inside `MongoFactoryTest`, a nested `static class Unannotated` with no `@MongoDocument`.

- [ ] **Step 1: Failing tests.**

```java
// MongoFactoryTest
@Test public void checkCollectionNameFromAnnotation() {
  assertThat(factory.collection(TestUser.class).getNamespace().getCollectionName()).isEqualTo("test_users");
  assertThat(factory.documents(TestUser.class).getNamespace().getCollectionName()).isEqualTo("test_users");
}
@Test public void checkUnannotatedClassFails() {
  assertThatThrownBy(() -> factory.collection(Unannotated.class))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(Unannotated.class.getName() + " is not annotated with @MongoDocument");
}
@Test public void checkPojoRoundTrip() { // insert via collection(TestUser.class), read back by _id, recursive comparison equal; id assigned
}
// ClasspathGuardTest
@Test public void checkNoLegacyDriverOrMorphiaOnClasspath() {
  assertThatThrownBy(() -> Class.forName("com.mongodb.DBCollection")).isInstanceOf(ClassNotFoundException.class);
  assertThatThrownBy(() -> Class.forName("dev.morphia.Datastore")).isInstanceOf(ClassNotFoundException.class);
}
```

- [ ] **Step 2: Run** `./gradlew :data-utils:nosql:mongodb:test --console=plain`. Expected: compile failure on the missing `MongoDocument`/`MongoFactory`. (`ClasspathGuardTest` alone would pass; it pins the guarantee.)
- [ ] **Step 3: Implement.** Default registry: `CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()))`. `Objects.requireNonNull` with parameter names on every constructor argument. No `ThreadLocal`. The spec cites `@ThreadSafe` on `MongoDatabase`/`MongoCollection` in a class Javadoc sentence.
- [ ] **Step 4: Run** `./gradlew :data-utils:nosql:mongodb:build validateMavenCentralPoms --console=plain`. Expected: `BUILD SUCCESSFUL`; `QTestUser` and `QAddress` generated under `build/generated/sources/annotationProcessor/java/test/dev/quokkify/mongodb/entity/`.
- [ ] **Step 5: Commit** `feat(mongodb): add module skeleton, @MongoDocument and MongoFactory`.

### Task 3: `PojoDocumentSerializer` and `MongoQuery<T>`

**Files:**

- Create: `src/main/java/dev/quokkify/mongodb/query/PojoDocumentSerializer.java`
- Create: `src/main/java/dev/quokkify/mongodb/query/MongoQuery.java`
- Create: `src/test/java/dev/quokkify/mongodb/test/MongoQueryTest.java`
- Create: `src/test/java/dev/quokkify/mongodb/test/AllureCapture.java`. This is a test helper that runs an action inside a temporary in-memory `AllureLifecycle` and returns the recorded attachments as `Map<String name, String content>`. Port it from `captureSqlAttachments` / `InMemoryResultsWriter` in `data-utils/sql/src/test/java/dev/quokkify/test/DatabaseSqlTest.java` on branch `feat/sql-stream-attachments` (PR #823): `git show origin/feat/sql-stream-attachments:data-utils/sql/src/test/java/dev/quokkify/test/DatabaseSqlTest.java`. Restore the previous lifecycle in `finally`.

**Interfaces:**

- Consumes: `MongoFactory.documents(Class<?>)`, `MongoFactory.codecRegistry()`, `MongoFactory.collection(Class<T>)` (Task 2); test entities (Task 2).
- Produces:
  - `public class PojoDocumentSerializer extends com.querydsl.mongodb.document.MongodbDocumentSerializer`. Overrides `getKeyForPath(Path<?>, PathMetadata)`: `@BsonId` → `"_id"`, non-empty `@BsonProperty(value)` → `value`, read from `expr.getAnnotatedElement()`; otherwise `super`. Also `isReference` → `false`, and `asReference` → `throw new UnsupportedOperationException("DBRef references are not supported")`.
  - `public class MongoQuery<T> extends AbstractFetchableMongodbQuery<T, MongoQuery<T>>` with constructor `MongoQuery(MongoFactory factory, Class<T> type)`. The transformer decodes `Document` → `T` with `factory.codecRegistry().get(type)` through a `BsonDocumentReader` over `document.toBsonDocument(Document.class, registry)`. `getCollection(Class<?>)` returns `factory.documents(type)`. Overrides `fetch()`, `fetchFirst()` and `fetchOne()` (all non-final in 7.7) with `@Step("Execute Mongo query")`; each first calls a private `@Attachment(value = "Mongo query", type = "application/json") String attachQuery()` returning `asDocument().toJson()`, then `super`.

- [ ] **Step 1: Failing tests** in `MongoQueryTest`. Seed `@BeforeMethod`: empty `test_users`, then insert `ann` (Ann, Lee, 30, ADMIN, Riga/LV-1010), `bob` (Bob, Lee, 25, USER, Oslo/0150) and `cid` (Cid, Moe, 41, USER, Riga/LV-1050) via `factory.collection(TestUser.class)`. `q = QTestUser.testUser`.
  - `checkFilterByField`: `where(q.lastName.eq("Lee")).fetch()` → firstNames `containsExactlyInAnyOrder("Ann", "Bob")`.
  - `checkFilterById`: `where(q.id.eq(bob.getId())).fetchOne()` → recursive-equals `bob`.
  - `checkFilterByRenamedField`: `where(q.firstName.eq("Cid")).fetchOne()` → recursive-equals `cid` (stored key `first_name`).
  - `checkFilterByRenamedNestedField`: `where(q.address.zipCode.eq("0150")).fetch()` → single `Bob`.
  - `checkFilterByNestedFieldAndEnum`: `where(q.address.city.eq("Riga"), q.role.eq(Role.USER)).fetch()` → single `Cid`.
  - `checkNoMatch`: `where(q.lastName.eq("Nobody"))`, then `fetch()` → empty and `fetchFirst()` → `null`.
  - `checkFetchOneWithSeveralMatchesFails`: `where(q.lastName.eq("Lee")).fetchOne()` → throws `com.querydsl.core.NonUniqueResultException`.
  - `checkQueryOnUnannotatedClassFails`: `new MongoQuery<>(factory, Unannotated.class)`. Put a local `@QueryEntity`-free `static class` in the test; any call path that reaches `factory.documents` (construction or first `fetch()`) throws `IllegalArgumentException` with the Global Constraints message.
  - `checkFetchAttachesQuery`: `AllureCapture.run(() -> query(q.lastName.eq("Moe")).fetch())` → exactly one attachment `Mongo query` whose content contains `"lastName"` and `"Moe"`; the same check for `fetchFirst()` and `fetchOne()`.
- [ ] **Step 2: Run** `./gradlew :data-utils:nosql:mongodb:test --tests 'dev.quokkify.mongodb.test.MongoQueryTest' --console=plain`. Expected: compile failure (missing classes).
- [ ] **Step 3: Implement** both classes per Interfaces.
- [ ] **Step 4: Run** the same command. Expected: all `MongoQueryTest` methods pass. Then `./gradlew :data-utils:nosql:mongodb:build --console=plain`: `BUILD SUCCESSFUL`.
- [ ] **Step 5: Commit** `feat(mongodb): add type-safe MongoQuery over Querydsl document API`.

### Task 4: `MongoDatabaseSteps`, verification, README

**Files:**

- Create: `src/main/java/dev/quokkify/mongodb/steps/MongoDatabaseSteps.java`
- Create: `src/main/java/dev/quokkify/mongodb/verification/MongoVerification.java`, `BaseMongoVerification.java`, `MongoVerifier.java`. Copy them from the legacy module's `dev.quokkify.verification` with the package changed to `dev.quokkify.mongodb.verification`; the API stays identical.
- Create: `src/test/java/dev/quokkify/mongodb/test/MongoDatabaseStepsTest.java`
- Create: `data-utils/nosql/mongodb/README.md`
- Modify: root `README.md` module table (add the `mongodb` row: `MongoDB helpers on the official driver with type-safe Querydsl queries`)

**Interfaces:**

- Consumes: `MongoFactory` (Task 2), `MongoQuery<T>` (Task 3).
- Produces: `public class MongoDatabaseSteps extends dev.quokkify.step.AbstractSteps<MongoVerifier>` with constructor `MongoDatabaseSteps(MongoFactory factory)`. Each method carries `@Step`:

| Method                                               | `@Step` text                                                                                      |
| ---------------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| `<T> void save(T entity)`                            | `Save entity to mongo database`                                                                   |
| `<T> void save(List<T> entities)`                    | `Save entities to mongo database`; an empty list is a no-op                                       |
| `<T> UpdateResult update(T entity, Bson... updates)` | `Update entity in mongo database`                                                                 |
| `<T> DeleteResult delete(T entity)`                  | `Delete entity in mongo database`                                                                 |
| `<T> MongoQuery<T> selectQuery(EntityPath<T> path)`  | `Create query for mongo database`; returns `new MongoQuery<>(factory, (Class<T>) path.getType())` |
| `MongoVerifier verify()`                             | —                                                                                                 |

Implementation details:

- The id is taken by encoding the entity with `factory.codecRegistry().get(entity.getClass())` into a `BsonDocument` (`BsonDocumentWriter`) and reading `_id`. A missing or null `_id` throws `IllegalArgumentException("<FQCN> has no _id; save it first")`.
- `update` with `updates.length == 0` throws `IllegalArgumentException("update requires at least one update operation")`. Otherwise it calls `updateOne(Filters.eq("_id", id), Updates.combine(updates))`.
- `delete` calls `deleteOne(Filters.eq("_id", id))`.

- [ ] **Step 1: Failing tests** in `MongoDatabaseStepsTest`. `@BeforeMethod` empties `test_users`.
  - `checkSaveAssignsId`: a new `TestUser` with null id; after `save`, `getId()` is not null and `selectQuery(q).where(q.id.eq(id)).fetchOne()` recursive-equals it.
  - `checkSaveList`: `save(List.of(a, b))` → `selectQuery(q).fetch()` has size 2; `save(List.of())` throws nothing and changes nothing.
  - `checkSaveTwiceFails`: `save(u); save(u)` → the second throws `com.mongodb.MongoWriteException`.
  - `checkUpdate`: `update(u, Updates.set("lastName", "Pop"))` → `getModifiedCount()` is 1; reload has `lastName` `Pop`; a second user stays unchanged.
  - `checkUpdateWithoutOperationsFails`: `update(u)` → `IllegalArgumentException`, message `update requires at least one update operation`.
  - `checkDeleteRemovesOnlyThisDocument` (regression guard against the legacy delete-all behavior): save `u1` and `u2` of the same class, `delete(u1)` → `getDeletedCount()` is 1 and the remaining documents are exactly `[u2]`.
  - `checkUnsavedEntityFails`: `update(new TestUser(), Updates.set("age", 1))` and `delete(new TestUser())` → `IllegalArgumentException` with message `dev.quokkify.mongodb.entity.TestUser has no _id; save it first`.
  - `checkVerifyHasDocument`: `verify().withTimeout(Duration.ofSeconds(5)).hasDocument(() -> selectQuery(q).fetch(), x -> "Ann".equals(x.getFirstName()))` passes after saving Ann. `doesNotHaveDocument` with `withTimeout(Duration.ofSeconds(2))` passes for `"Zed"`.
- [ ] **Step 2: Run** `./gradlew :data-utils:nosql:mongodb:test --tests 'dev.quokkify.mongodb.test.MongoDatabaseStepsTest' --console=plain`. Expected: compile failure (missing classes).
- [ ] **Step 3: Implement** the steps and copy the verification classes.
- [ ] **Step 4: README** `data-utils/nosql/mongodb/README.md`, with these sections in this order, in the style of `data-utils/sql/README.md`:
  - Installation: `testImplementation("dev.quokkify:mongodb:<current q4j version from release-please manifest>")` plus `testAnnotationProcessor("io.github.openfeign.querydsl:querydsl-apt:7.7:general")`.
  - Entity: the `TestUser`-like example with `@MongoDocument`, `@QueryEntity`, `@BsonId`, `@BsonProperty`.
  - Setup: `new MongoFactory(MongoClients.create(url), "db")`, `new MongoDatabaseSteps(factory)`.
  - CRUD.
  - Type-safe queries.
  - Verification.
  - Limitations: annotations only on fields; no `PropertyNamingStrategy`; no DBRef; no aggregation or transactions.
  - Migrating from morphia-legacy: link `../morphia-legacy/README.md`.
- [ ] **Step 5: Run** `./gradlew :data-utils:nosql:mongodb:build :data-utils:nosql:morphia-legacy:build validateMavenCentralPoms --console=plain` with MongoDB running. Expected: `BUILD SUCCESSFUL`; all mongodb tests and the unchanged legacy tests pass. Then run `./gradlew :data-utils:nosql:mongodb:dependencies --configuration runtimeClasspath --console=plain -q | grep -E 'legacy|morphia'`. Expected: no output.
- [ ] **Step 6: Commit** `feat(mongodb): add MongoDatabaseSteps, verification and README`.

## Finishing (not a task)

PR title: `feat(mongodb)!: add MongoDB module without Morphia and rename morphia to morphia-legacy`. The body follows `.github/pull_request_template.md`:

- `## Highlight`: one bold sentence plus at most three sentences.
- `## Usage example`: an entity and a `selectQuery` call.
- `## Migration`: the coordinate change, plus a link to the legacy README table.

Dry-run `render_notes` from `.github/scripts/enrich_release_notes.py` before `gh pr create`. Base: `feat/morphia-querydsl`.
