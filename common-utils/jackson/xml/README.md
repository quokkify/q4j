# common-utils/jackson/xml

Parse XML classpath resources, input streams, and strings into typed Java objects using Jackson XmlMapper.

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/q4j-jackson-xml):

```kotlin
dependencies {
    testImplementation("dev.quokkify:q4j-jackson-xml:0.6.0")
}
```

## Usage

No initialization required — all methods are static.

## Usage in tests

```java
// Parse an XML test fixture from the classpath
Order order = XmlParser.parse("responses/order.xml", Order.class);

assertThat(order.getId()).isEqualTo("ORD-001");
assertThat(order.getStatus()).isEqualTo("CONFIRMED");

// Parse an XML response body received as a string
Order parsed = XmlConverter.fromString(httpResponseBody, Order.class);

assertThat(parsed.getTotalAmount()).isGreaterThan(BigDecimal.ZERO);

// Parse from an InputStream (caller is responsible for closing the stream)
try (InputStream is = getClass().getResourceAsStream("/fixtures/catalog.xml")) {
    Catalog catalog = XmlParser.parse(is, Catalog.class);
    assertThat(catalog.getItems()).isNotEmpty();
}
```

## Key API

| Method                                         | Description                               |
| ---------------------------------------------- | ----------------------------------------- |
| `XmlParser.parse(resourcePath, Class<T>)`      | Parse XML classpath resource to type      |
| `XmlParser.parse(inputStream, Class<T>)`       | Parse XML from stream (stream not closed) |
| `XmlConverter.fromString(xmlString, Class<T>)` | Deserialize XML string to type            |
