# data-utils/nosql/morphia

Morphia-based MongoDB access utility for test automation with thread-safe datastore management.

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/q4j-morphia):

```kotlin
dependencies {
    testImplementation("dev.quokkify:q4j-morphia:0.6.0")
}
```

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

````

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
````

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
