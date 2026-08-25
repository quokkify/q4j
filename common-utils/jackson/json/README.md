# common-utils/jackson/json

Jackson-based JSON utility with module auto-discovery, null handling control, generic type support, and file deserialization.

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/q4j-jackson-json):

```kotlin
dependencies {
    testImplementation("dev.quokkify:q4j-jackson-json:0.6.0")
}
```

## Usage

No initialization required — all methods are static.

## Usage in tests

```java
// Deserialize a plain API response body
User user = JsonConverter.fromString(responseBody, User.class);

// Deserialize a generic wrapper returned by the API
ApiResponse<Order> response =
    JsonConverter.fromStringParametric(responseBody, ApiResponse.class, Order.class);

// Serialize a request payload, omitting null fields
String body = JsonConverter.toJsonIgnoreNulls(new CreateOrderRequest(sku, null));

// Round-trip convert a Map fixture to a typed object
User converted = JsonConverter.fromObject(fixtureMap, User.class);

// Load a large JSON fixture from a test resource file
List<Product> catalog =
    JsonConverter.fromString(Files.readString(fixturePath), new TypeReference<>() {});
```

## Key API

| Method                                        | Description                               |
| --------------------------------------------- | ----------------------------------------- |
| `fromString(json, Class<T>)`                  | Deserialize JSON string to type           |
| `fromString(json, TypeReference<T>)`          | Deserialize to generic / collection type  |
| `fromObject(obj, Class<T>)`                   | Convert object via JSON round-trip        |
| `fromStringParametric(json, Class, Class...)` | Deserialize parameterized generic wrapper |
| `toJson(object)`                              | Serialize to JSON, nulls included         |
| `toJsonIgnoreNulls(object)`                   | Serialize to JSON, null fields omitted    |
| `fromFile(file, Class<T>)`                    | Deserialize JSON file to type             |
