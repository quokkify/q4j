# data-utils/sql

JPA/Hibernate + QueryDSL utilities for SQL database access in test automation.

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/sql):

```kotlin
dependencies {
    testImplementation("dev.quokkify:sql:0.14.1")
}
```

The module brings `dev.quokkify:testng` (`AbstractSteps`) transitively; do not declare it separately.

[Querydsl](https://openfeign.github.io/querydsl/) (`io.github.openfeign.querydsl:querydsl-jpa`) also comes
transitively. To generate Q-classes (`QUser`, ...) for your own entities, add the annotation processor:

```kotlin
dependencies {
    testAnnotationProcessor("io.github.openfeign.querydsl:querydsl-apt:7.7:jpa")
}
```

The `jpa` classifier registers `JPAAnnotationProcessor` and brings `jakarta.persistence-api` with it.

### Migrating from `com.querydsl`

The module uses the maintained OpenFeign fork of Querydsl instead of `com.querydsl` 5.x, which has had no
release since January 2024. The fork keeps the `com.querydsl.*` packages, so query code does not change, but
the Maven coordinates do:

- Replace the `com.querydsl` group ID with `io.github.openfeign.querydsl`.
- Drop the `:jakarta` classifier from `querydsl-jpa`; on `querydsl-apt` use the `jpa` classifier.
- Remove any remaining `com.querydsl:*` dependency. Both artifacts contain the same `com.querydsl.*` classes,
  and Gradle and Maven do not treat them as one library, so keeping both puts two copies on the classpath
  without any resolution error, and whichever copy loads first wins at runtime.

See the [Querydsl migration guide](https://openfeign.github.io/querydsl/migration) for details.

## Environment variables

| Variable                 | Default                 | Description                              |
| ------------------------ | ----------------------- | ---------------------------------------- |
| `SQL_DATABASE_URL`       | —                       | JDBC URL                                 |
| `SQL_DATABASE_USER`      | —                       | DB username                              |
| `SQL_DATABASE_PASSWORD`  | —                       | DB password                              |
| `SQL_DATABASE_DRIVER`    | `org.postgresql.Driver` | JDBC driver class                        |
| `SQL_DATABASE_POOL_SIZE` | `100`                   | Maximum connections in the pool (Agroal) |

## Initialization in BaseTest

There are 3 ways to create a `SqlFactory`. Pick one depending on where your DB config lives.

### Variant 1 — persistence.xml

Connection is defined in `META-INF/persistence.xml` under a named persistence unit.

```java
SqlFactory sqlFactory = DatabaseService.getInstance().createSqlQuery("my-persistence-unit");
```

### Variant 2 — properties file via DatabaseStage

Connection is read from a `.properties` file on the classpath (e.g. `db.properties` with `SQL_DATABASE_URL`, `SQL_DATABASE_USER`, etc.). Implement `DatabaseStage` to point to that file:

```java
DatabaseStage stage = new DatabaseStage() {
    public String getProjectName()          { return "my-project"; }
    public String getPersistenceName()      { return "local"; }
    public String getPersistencePropertyPath() { return "db.properties"; }
};

HibernatePersistenceConfiguration configuration = PersistenceItemProvider.getPersistenceConfiguration(stage);
SqlFactory sqlFactory = DatabaseService.getInstance().createSqlQuery(configuration);
```

### Variant 3 — manual properties map

Use the built-in `DatabaseConfig` (Owner interface, reads `SQL_DATABASE_*` env vars) to populate the map:

```java
DatabaseConfig dbConfig = ConfigRegistry.get(DatabaseConfig.class);

HibernatePersistenceConfiguration configuration = PersistenceConfigurationFactory.create(
    "my-persistence-unit",
    Map.of(
        AvailableSettings.JAKARTA_JDBC_URL,      dbConfig.url(),
        AvailableSettings.JAKARTA_JDBC_USER,     dbConfig.user(),
        AvailableSettings.JAKARTA_JDBC_PASSWORD, dbConfig.password()
    )
);
SqlFactory sqlFactory = DatabaseService.getInstance().createSqlQuery(configuration);
```

Entities implementing `SqlEntityInterface` are discovered in any package; add more with `.managedClasses(...)` on the returned (fresh) configuration, or pass an exact set with `PersistenceConfigurationFactory.create(name, properties, entityClasses)`, which does no classpath scan and replaces overriding `getManagedClassNames()`.

Running `:data-utils:sql:test` locally needs PostgreSQL: `docker compose -f tools/environment/docker/docker-compose.yml -f tools/environment/docker/docker-compose.local.yml --profile sql up -d`.

> `PersistenceItem` and `PersistenceItemProvider.getPersistenceItem` are deprecated for removal; use the `PersistenceConfiguration` variants above.

> **Alternative** (without Owner): pass values directly via `System.getenv("SQL_DATABASE_URL")`, etc.

### BaseTest wiring

```java
public abstract class BaseTest {

    protected SqlDatabaseSteps databaseSteps;

    @BeforeClass(alwaysRun = true)
    public void initDatabase() {
        // use any variant above to get sqlFactory
        databaseSteps = new SqlDatabaseSteps(sqlFactory);
    }

    @AfterClass(alwaysRun = true)
    public void closeDatabase() {
        databaseSteps.closeConnection();
    }
}
```

## Custom steps class

Extend `AbstractDatabaseSteps` to add domain-specific queries:

```java
public class UserDbSteps extends AbstractDatabaseSteps {

    private final SqlDatabaseSteps databaseSteps;

    public UserDbSteps(SqlDatabaseSteps databaseSteps) {
        this.databaseSteps = databaseSteps;
    }

    @Override
    protected SqlDatabaseSteps getDatabaseSteps() {
        return databaseSteps;
    }

    public User getUserByEmail(String email) {
        QUser user = QUser.user;
        return fetchOne(steps -> steps.selectDsl(user).where(user.email.eq(email)));
    }

    public User waitForUser(String email) {
        QUser user = QUser.user;
        return waitUntilAppear(steps -> steps.selectDsl(user).where(user.email.eq(email)));
    }
}
```

## Usage in tests

```java
public class UserTest extends BaseTest {

    private UserDbSteps userSteps;

    @BeforeClass(alwaysRun = true)
    public void init() {
        userSteps = new UserDbSteps(databaseSteps);
    }

    @Test
    public void checkUserCreated() {
        User user = userSteps.waitForUser("alice@example.com");
        Assertions.assertThat(user.getStatus()).isEqualTo("active");
    }

    @Test
    public void checkSaveAndSelect() {
        User newUser = new User("bob@example.com");
        databaseSteps.save(newUser);

        QUser q = QUser.user;
        User found = databaseSteps.selectDsl(q)
            .where(q.email.eq("bob@example.com"))
            .fetchOne();

        Assertions.assertThat(found).isNotNull();
    }
}
```

## Fluent verification chain

Use `verify()` to assert database state with polling and configurable timeouts:

```java
QUser user = QUser.user;

databaseSteps.verify()
    .hasRecord(
        () -> databaseSteps.selectDsl(user).where(user.email.eq("alice@example.com")).fetch(),
        u -> u.getStatus().equals("active")
    );
```

Negative assertion — record must not appear within the window:

```java
databaseSteps.verify()
    .doesNotHaveRecord(
        () -> databaseSteps.selectDsl(user).where(user.email.eq("deleted@example.com")).fetch(),
        u -> u.getStatus().equals("active")
    );
```

Override timing per-call:

```java
databaseSteps.verify()
    .withTimeout(Duration.ofSeconds(30))
    .withPolling(Duration.ofMillis(1000))
    .hasRecord(query, predicate);
```

### Verification methods

| Method                                               | Description                                                              |
| ---------------------------------------------------- | ------------------------------------------------------------------------ |
| `hasRecord(Callable<List<E>>, Predicate<E>)`         | Waits until the query returns at least one record matching the predicate |
| `doesNotHaveRecord(Callable<List<E>>, Predicate<E>)` | Asserts no matching record appears within the configured window          |
| `hasRecordCount(Callable<List<E>>, int)`             | Waits until the query returns at least N records                         |

Default timeout is 60 seconds with 5 second polling.

---

## Key API

| Method                             | Description                          |
| ---------------------------------- | ------------------------------------ |
| `databaseSteps.save(entity)`       | Insert one or a list of entities     |
| `databaseSteps.update(entity)`     | Merge entity changes                 |
| `databaseSteps.delete(entity)`     | Remove entity                        |
| `databaseSteps.selectDsl(QEntity)` | Start a QueryDSL select query        |
| `fetchOne(fn)`                     | Execute query, throw if null         |
| `fetchFirst(fn)`                   | First result or throw                |
| `fetch(fn)`                        | Fetch list or throw                  |
| `waitUntilAppear(fn)`              | Poll until result appears (60s / 5s) |
