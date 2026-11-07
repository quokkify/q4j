# MongoDB Module Without Morphia — Design

Date: 2026-11-07. Context: issue #818 (adoption review), #821 (driver alignment), PRs #819, #820, #822.

## Goal

Give q4j a MongoDB test-automation module that does not depend on Morphia or `org.mongodb:mongodb-driver-legacy`, built on `mongodb-driver-sync` + driver POJO codecs + the OpenFeign Querydsl fork's document API. The current Morphia module stays available under a new name and is deprecated for removal.

## Why

- Morphia needs the legacy driver at runtime. `jdeps` shows `morphia-core` 2.5.3 using `com.mongodb.AggregationOptions`, which ships only in `mongodb-driver-legacy`. Morphia's `master` (`3.0.0-SNAPSHOT`) still declares `mongodb-driver-legacy` in `core/pom.xml`. As long as q4j is on Morphia, the legacy driver stays on consumers' classpaths.
- A throwaway spike (11/11 tests against MongoDB 9.0.4) confirmed the replacement stack. With `mongodb-driver-sync` 5.13.0, POJO codecs and `io.github.openfeign.querydsl:querydsl-mongodb` 7.7 (legacy excluded), the runtime classpath contains only `bson`, `bson-record-codec`, `mongodb-driver-core` and `mongodb-driver-sync`. `querydsl-apt` with the `general` classifier generates Q-classes for `@QueryEntity` POJOs, including nested types. The adapter takes about 37 lines.
- No known consumer depends on `dev.quokkify:morphia`. The quokkify org and the owner's local clones use only `dev.quokkify:sql`. The artifact is public on Maven Central, so unknown consumers get a clear rename path rather than silent removal.

## Decisions

| Topic           | Decision                                                                                                                                                                                                                                                                                                                                     |
| --------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Old module      | Renamed: directory `data-utils/nosql/morphia` → `data-utils/nosql/morphia-legacy`, Gradle `:data-utils:nosql:morphia-legacy`, artifactId `morphia-legacy`, displayName `Q4J Morphia (legacy)`. Java packages and behavior unchanged. All public types get `@Deprecated(forRemoval = true)` with a Javadoc pointer to `dev.quokkify:mongodb`. |
| New module      | Directory `data-utils/nosql/mongodb`, Gradle `:data-utils:nosql:mongodb`, artifactId `mongodb`, displayName `Q4J MongoDB`, packages `dev.quokkify.mongodb.*`. There are no split packages with the legacy module, so both can sit on one classpath during migration.                                                                         |
| Entity contract | `@MongoDocument("collection")` (q4j annotation, required) + `@com.querydsl.core.annotations.QueryEntity` + `@BsonId` on the id field. No marker interface.                                                                                                                                                                                   |
| API shape       | A thin wrapper over the driver that mirrors the legacy shape, so migration is method for method (approach A).                                                                                                                                                                                                                                |
| Release         | One breaking PR `feat(mongodb)!`, merged after #819, #820 and #822.                                                                                                                                                                                                                                                                          |

## Module layout and dependencies

`data-utils/nosql/mongodb/build.gradle`:

- `api libs.mongodb.driver.sync`
- `api(libs.querydsl.mongodb) { exclude group: "org.mongodb", module: "mongodb-driver-legacy" }`. Catalog entry `querydsl-mongodb` with `version.ref = "querydsl"`; added here if #820 has not added it yet.
- `api libs.allure.java.commons`
- `implementation project(":common-utils:awaitility")`, `implementation project(":testng-extensions")`
- `testAnnotationProcessor variantOf(libs.querydsl.apt) { classifier("general") }`

Registration: `settings.gradle` and `gradle/module-metadata.gradle` (new entry; legacy entry renamed). Root `README.md` module table: add `mongodb`, mark `morphia-legacy` deprecated.

## Components

All under `dev.quokkify.mongodb`.

### `annotation.MongoDocument`

`@Retention(RUNTIME) @Target(TYPE) public @interface MongoDocument { String value(); }`: the collection name.

### `service.MongoFactory`

- `public MongoFactory(MongoClient client, String databaseName)`. Uses `CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), fromProviders(PojoCodecProvider.builder().automatic(true).build()))`.
- `public MongoFactory(MongoClient client, String databaseName, CodecRegistry codecRegistry)`: an explicit registry.
- `public <T> MongoCollection<T> collection(Class<T> type)`: collection named by `@MongoDocument`, with the factory's registry.
- `public CodecRegistry codecRegistry()`.
- No `ThreadLocal`. `MongoDatabase` and `MongoCollection` are `@ThreadSafe` in driver 5.13.0 (`driver-sync/src/main/com/mongodb/client/MongoDatabase.java:42`, `MongoCollection.java:70`), and `MongoClient` is designed to be shared across threads.
- Errors: a class without `@MongoDocument` throws `IllegalArgumentException("<FQCN> is not annotated with @MongoDocument")`. A `null` argument throws `NullPointerException` with the parameter name (`Objects.requireNonNull`).

### `steps.MongoDatabaseSteps extends AbstractSteps<MongoVerifier>`

Constructor `MongoDatabaseSteps(MongoFactory factory)`. Every public method carries `@Step`.

| Method                                               | Behavior                                                                                                                                                |
| ---------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `<T> void save(T entity)`                            | `insertOne`. The POJO codec generates an `ObjectId` for a `null` `@BsonId` and writes it back to the entity (`PojoCodecImpl.encodeIdProperty`, 5.13.0). |
| `<T> void save(List<T> entities)`                    | `insertMany`. An empty list is a no-op.                                                                                                                 |
| `<T> UpdateResult update(T entity, Bson... updates)` | `updateOne(eq("_id", id), Updates.combine(updates))`.                                                                                                   |
| `<T> DeleteResult delete(T entity)`                  | `deleteOne(eq("_id", id))`. It deletes exactly one document, unlike legacy, which deletes every document of the class.                                  |
| `<T> MongoQuery<T> selectQuery(EntityPath<T> path)`  | A new query over `factory.collection(path.getType())`.                                                                                                  |
| `MongoVerifier verify()`                             | A new verifier.                                                                                                                                         |

The id is read by encoding the entity with the factory's codec into a `BsonDocument` and taking `_id`. A missing `_id` throws `IllegalArgumentException("<FQCN> has no _id; save it first")`.

### `query.MongoQuery<T> extends AbstractFetchableMongodbQuery<T, MongoQuery<T>>`

- Built from the `Document`-typed collection, a transformer that decodes `Document` → `T` through the factory's codec, and `PojoDocumentSerializer`.
- Allure: `fetch()` and `fetchOne()` get `@Step("Execute Mongo query")` and attach the serialized filter as JSON under the name `Mongo query`, where the superclass allows overriding. `fetchFirst()` delegates to `fetchOne()` where the superclass does. The plan verifies which fetch methods are overridable in 7.7 and pins the exact set.

### `query.PojoDocumentSerializer extends MongodbDocumentSerializer`

- `getKeyForPath`: if `expr.getAnnotatedElement()` has `@BsonId`, return `_id`. If it has a non-empty `@BsonProperty(value)`, return that value. Otherwise use the default.
- `isReference` → `false`. `asReference` → `UnsupportedOperationException("DBRef references are not supported")`.
- Known limitation: annotations are read from fields only. `@BsonProperty` on getters or setters and a custom `PropertyNamingStrategy` are not reflected in query keys. This is documented in the README.

### `verification.MongoVerification`, `BaseMongoVerification`, `MongoVerifier`

Copied from the legacy module with the same API (`hasDocument(Callable<List<E>>, Predicate<E>)`, `doesNotHaveDocument(...)`, `withTimeout`, `withPolling`), so the new module does not depend on `morphia-legacy`.

## Testing

TestNG + AssertJ against a live MongoDB at `MONGODB_URL` (default `mongodb://localhost:27017`), as in the legacy module. CI already provisions MongoDB through `tools/environment`.

- `MongoFactoryTest`: the collection name comes from `@MongoDocument`; a class without the annotation throws `IllegalArgumentException` naming the class.
- `MongoDatabaseStepsTest`:
  - `save` assigns `_id`, and `save(List)` inserts all entities.
  - `update` with `Updates.set` changes only the target document.
  - `delete` removes exactly one document while another document of the same class remains. This is the regression guard against the legacy behavior.
  - `update` or `delete` of an unsaved entity throws `IllegalArgumentException`.
- `MongoQueryTest`:
  - filters by a plain field, by `id` (`_id`), by a field with `@BsonProperty`, by a nested path (`address.city`) and by an enum;
  - no match: `fetch()` returns an empty list and `fetchFirst()` returns `null`;
  - several matches;
  - the `Mongo query` Allure attachment is recorded, captured with an in-memory `AllureResultsWriter` and no new dependencies.
- Classpath guard: the module build fails if `runtimeClasspath` resolves `org.mongodb:mongodb-driver-legacy` or `dev.morphia.morphia:*`.
- Legacy module: its existing tests pass unchanged after the rename.

## Documentation

- `data-utils/nosql/mongodb/README.md`: installation, annotation processor (`querydsl-apt:7.7:general`), entity example, CRUD, `selectQuery`, `verify()`, limitations.
- `data-utils/nosql/morphia-legacy/README.md`: a deprecation banner and a migration table:

| Morphia (legacy)                                         | `dev.quokkify:mongodb`                     |
| -------------------------------------------------------- | ------------------------------------------ |
| `@dev.morphia.annotations.Entity`                        | `@MongoDocument("name")` + `@QueryEntity`  |
| `@dev.morphia.annotations.Id`                            | `@org.bson.codecs.pojo.annotations.BsonId` |
| `new NoSqlFactory(client, config)`                       | `new MongoFactory(client, "db")`           |
| `update(entity, List.of(UpdateOperators.set(f, v)))`     | `update(entity, Updates.set(f, v))`        |
| `selectDsl(Class)` / `selectQuery(EntityPath)` (Morphia) | `selectQuery(EntityPath)`                  |
| `delete(entity)`: deletes all documents of the class     | `delete(entity)`: deletes this document    |

## Release

PR title: `feat(mongodb)!: add MongoDB module without Morphia and rename morphia to morphia-legacy`. The body follows the PR template, and `render_notes` from `.github/scripts/enrich_release_notes.py` is dry-run before publishing:

- `## Highlight`: the new module without Morphia or the legacy driver, with type-safe queries.
- `## Usage example`: an entity and `selectQuery`.
- `## Migration`: change the coordinate `dev.quokkify:morphia` → `dev.quokkify:morphia-legacy`, plus a link to the migration table.

## Out of scope

- DBRef/reference support, aggregation helpers, transactions, the reactive driver.
- Removing `morphia-legacy`. That is a later breaking release.
- Changes to `dev.quokkify:sql`.
