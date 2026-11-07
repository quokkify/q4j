# data-utils/nosql/mongodb

MongoDB helpers for test automation on the official sync driver, with type-safe [Querydsl](https://openfeign.github.io/querydsl/) queries and polling verification.

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/mongodb):

```kotlin
dependencies {
    testImplementation("dev.quokkify:mongodb:0.14.0")
    testAnnotationProcessor("io.github.openfeign.querydsl:querydsl-apt:7.7:general")
}
```

The annotation processor generates Q-classes (`QUser`, ...) for classes annotated with `@QueryEntity`.

## Entity

Every document class needs `@MongoDocument` with the collection name, `@QueryEntity`, and `@BsonId` on the id field:

```java
@MongoDocument("users")
@QueryEntity
public class User {

    @BsonId
    private ObjectId id;

    @BsonProperty("first_name")
    private String firstName;

    private int age;

    public User() {
    }

    // getters and setters
}
```

## Setup

```java
MongoFactory factory = new MongoFactory(MongoClients.create("mongodb://localhost:27017"), "db");
MongoDatabaseSteps steps = new MongoDatabaseSteps(factory);
```

## CRUD

```java
User user = new User();
user.setFirstName("Ann");

steps.save(user);                                        // the generated id is written back to the entity
steps.save(List.of(first, second));                      // an empty list does nothing
steps.update(user, Updates.set("age", 31));              // at least one update operation is required
steps.delete(user);                                      // deletes only this document
```

`Updates.set(...)` takes stored field names (for example `first_name`); only Querydsl paths are mapped from Java names.

`save` fails with `MongoWriteException` when a document with the same `_id` already exists. `update` and `delete` throw
`IllegalArgumentException` for an entity without `_id`.

## Type-safe queries

```java
QUser u = QUser.user;

List<User> adults = steps.selectQuery(u).where(u.age.goe(18)).fetch();
User ann = steps.selectQuery(u).where(u.firstName.eq("Ann")).fetchOne();
```

`fetchOne()` throws `NonUniqueResultException` when several documents match. Field names use the stored
`@BsonProperty` names, including fields of embedded objects. The no-arg `fetch()`, `fetchFirst()` and `fetchOne()` attach the query filter as JSON (`Mongo query`) to the
Allure report; other fetch variants are not instrumented. The attachment contains filter values, so avoid filtering on
secrets.

## Verification

```java
steps.verify()
    .withTimeout(Duration.ofSeconds(10))
    .hasDocument(() -> steps.selectQuery(u).fetch(), x -> "Ann".equals(x.getFirstName()))
    .doesNotHaveDocument(() -> steps.selectQuery(u).fetch(), x -> "Zed".equals(x.getFirstName()))
    .hasDocumentCount(() -> steps.selectQuery(u).fetch(), 1);
```

`hasDocumentCount` means at least that many documents. Polling a `selectQuery(...)` adds steps and attachments on every
poll; for long polls prefer `factory.collection(User.class).find(...)` inside the lambda.

## Limitations

- Annotations are supported on fields only.
- `PropertyNamingStrategy` is not supported.
- `DBRef` references are not supported.
- No aggregation or transactions.

## Migrating from morphia-legacy

See the [migration table](../morphia-legacy/README.md).
