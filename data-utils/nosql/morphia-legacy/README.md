> **Deprecated.** Use [`dev.quokkify:mongodb`](../mongodb/README.md); this module will be removed in a future release.

# data-utils/nosql/morphia-legacy

Morphia-based MongoDB access utility for test automation with thread-safe datastore management.

## Migrating to dev.quokkify:mongodb

| Morphia (legacy)                                                             | `dev.quokkify:mongodb`                                                                            |
| ---------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| `@dev.morphia.annotations.Entity`                                            | `@MongoDocument("name")` + `@QueryEntity`                                                         |
| `@dev.morphia.annotations.Id`                                                | `@org.bson.codecs.pojo.annotations.BsonId`                                                        |
| `@Property`                                                                  | `@BsonProperty`                                                                                   |
| `@Embedded`                                                                  | `@QueryEmbeddable`                                                                                |
| `implements MongoEntityInterface` / `extends Identity`                       | declare your own `@BsonId ObjectId id`                                                            |
| `querydsl-apt:7.7:morphia` + `morphia-core`                                  | `querydsl-apt:7.7:general`                                                                        |
| `dev.quokkify.steps` / `dev.quokkify.verification`                           | `dev.quokkify.mongodb.steps` / `dev.quokkify.mongodb.verification`                                |
| `new NoSqlFactory(client, "db")` / `new NoSqlFactory(client, morphiaConfig)` | `new MongoFactory(client, "db")`                                                                  |
| `getNoSqlFactory()`                                                          | keep your own `MongoFactory` reference                                                            |
| `update(entity, List.of(UpdateOperators.set(f, v)))`                         | `update(entity, Updates.set(f, v))`                                                               |
| `selectDsl(Class)` + Morphia `Filters`                                       | `selectQuery(QEntity)` or `factory.collection(T.class).find(com.mongodb.client.model.Filters...)` |
| `selectQuery(EntityPath)` (Morphia)                                          | `selectQuery(EntityPath)`                                                                         |
| `delete(entity)`: deletes all documents of the class                         | `delete(entity)`: deletes this document                                                           |

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/morphia-legacy):

```kotlin
dependencies {
    testImplementation("dev.quokkify:morphia-legacy:0.15.0")
}
```

The module brings `dev.quokkify:testng` (`AbstractSteps`) transitively; do not declare it separately.

## Environment variables

| Variable         | Default | Description           |
| ---------------- | ------- | --------------------- |
| `MONGO_HOST`     | —       | MongoDB host          |
| `MONGO_PORT`     | —       | MongoDB port          |
| `MONGO_DATABASE` | —       | MongoDB database name |

## Initialization in BaseTest

Define an Owner config interface to read env vars in a type-safe way (requires `common-utils/config`):

```java
@Config.Sources({"system:env"})
interface MongoConfig extends Config {
    @Key("MONGO_HOST")     String host();
    @Key("MONGO_PORT")     @DefaultValue("27017") int port();
    @Key("MONGO_DATABASE") String database();
}
```

Then initialize in `@BeforeClass`:

```java
public abstract class BaseTest {

    protected MongoDatabaseSteps mongoSteps;

    @BeforeClass(alwaysRun = true)
    public void initDatabase() {
        MongoConfig config = ConfigRegistry.get(MongoConfig.class);

        MongoClient mongoClient = MongoClients.create(
            "mongodb://" + config.host() + ":" + config.port());
        NoSqlFactory noSqlFactory = new NoSqlFactory(mongoClient, config.database());
        mongoSteps = new MongoDatabaseSteps(noSqlFactory);
    }
}
```

> **Alternative** (without Owner): read values directly via `System.getenv("MONGO_HOST")`, `System.getenv("MONGO_PORT")`, `System.getenv("MONGO_DATABASE")`.

## Usage in tests

```java
public class UserTest extends BaseTest {

    @Test
    public void checkSaveAndQueryUser() {
        User user = new User("alice@example.com", 25);
        mongoSteps.save(user);

        User found = mongoSteps.selectDsl(User.class)
            .filter(Filters.eq("email", "alice@example.com"))
            .first();

        Assertions.assertThat(found).isNotNull();
        Assertions.assertThat(found.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    public void checkBulkSaveAndFilter() {
        List<User> users = List.of(new User("bob@example.com", 20), new User("carol@example.com", 30));
        mongoSteps.save(users);

        List<User> adults = mongoSteps.selectDsl(User.class)
            .filter(Filters.gte("age", 18))
            .iterator().toList();

        Assertions.assertThat(adults).hasSizeGreaterThanOrEqualTo(2);
    }
}
```

## Type-safe queries with Querydsl

`selectQuery` builds a Querydsl `MorphiaQuery` from a generated Q-class. Add the annotation processors that generate Q-classes for your `@Entity` types:

```kotlin
dependencies {
    testAnnotationProcessor("io.github.openfeign.querydsl:querydsl-apt:7.7:morphia")
    testAnnotationProcessor("io.github.openfeign.querydsl:querydsl-mongodb:7.7")
    testAnnotationProcessor("dev.morphia.morphia:morphia-core:2.5.3")
}
```

Use `annotationProcessor` instead of `testAnnotationProcessor` if the entities live in `src/main`. Keep the processor's Querydsl and `morphia-core` versions equal to the ones `dev.quokkify:morphia-legacy` brings transitively (check with `./gradlew dependencies`).

```java
QUser user = QUser.user;
User found = mongoSteps.selectQuery(user)
    .where(user.email.eq("alice@example.com"))
    .fetchFirst();
```

`selectDsl(Class)` with Morphia `Filters` keeps working unchanged.

## Type-safe queries with Querydsl

`selectQuery` builds a Querydsl `MorphiaQuery` from a generated Q-class. Add the annotation processors that generate Q-classes for your `@Entity` types:

```kotlin
dependencies {
    testAnnotationProcessor("io.github.openfeign.querydsl:querydsl-apt:7.7:morphia")
    testAnnotationProcessor("io.github.openfeign.querydsl:querydsl-mongodb:7.7")
    testAnnotationProcessor("dev.morphia.morphia:morphia-core:2.5.3")
}
```

Use `annotationProcessor` instead of `testAnnotationProcessor` if the entities live in `src/main`. Keep the processor's Querydsl and `morphia-core` versions equal to the ones `dev.quokkify:morphia` brings transitively (check with `./gradlew dependencies`).

```java
QUser user = QUser.user;
User found = mongoSteps.selectQuery(user)
    .where(user.email.eq("alice@example.com"))
    .fetchFirst();
```

`selectDsl(Class)` with Morphia `Filters` keeps working unchanged.

## Fluent verification chain

Use `verify()` to assert MongoDB state with polling and configurable timeouts:

```java
mongoSteps.verify()
    .hasDocument(
        () -> mongoSteps.selectDsl(User.class)
            .filter(Filters.eq("email", "alice@example.com")).iterator().toList(),
        user -> user.getStatus().equals("active")
    );
```

Negative assertion — document must not appear within the window:

```java
mongoSteps.verify()
    .doesNotHaveDocument(
        () -> mongoSteps.selectDsl(User.class)
            .filter(Filters.eq("email", "deleted@example.com")).iterator().toList(),
        user -> user.getStatus().equals("active")
    );
```

Override timing per-call:

```java
mongoSteps.verify()
    .withTimeout(Duration.ofSeconds(15))
    .withPolling(Duration.ofMillis(500))
    .hasDocument(query, predicate);
```

### Verification methods

| Method                                                 | Description                                                                |
| ------------------------------------------------------ | -------------------------------------------------------------------------- |
| `hasDocument(Callable<List<E>>, Predicate<E>)`         | Waits until the query returns at least one document matching the predicate |
| `doesNotHaveDocument(Callable<List<E>>, Predicate<E>)` | Asserts no matching document appears within the configured window          |
| `hasDocumentCount(Callable<List<E>>, int)`             | Waits until the query returns at least N documents                         |

Default timeout is 30 seconds with 1 second polling.

---

## Key API

| Method                                   | Description                                       |
| ---------------------------------------- | ------------------------------------------------- |
| `new NoSqlFactory(mongoClient, dbName)`  | Wrap MongoClient with thread-safe datastore       |
| `noSqlFactory.getThreadLocalDatastore()` | Return thread-local Morphia `Datastore`           |
| `mongoSteps.save(entity)`                | Insert one entity                                 |
| `mongoSteps.save(List<T> entities)`      | Insert a list of entities                         |
| `mongoSteps.update(entity, operators)`   | Apply update operators, return `UpdateResult`     |
| `mongoSteps.delete(entity)`              | Remove entity, return `DeleteResult`              |
| `mongoSteps.selectDsl(Entity.class)`     | Start a Morphia `Query` for the given entity type |
| `mongoSteps.selectQuery(QEntity)`        | Start a type-safe Querydsl `MorphiaQuery`         |
