# common-utils/html

Parse raw HTML strings and extract elements by XPath — useful for validating HTML embedded in API responses.

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/q4j-html):

```kotlin
dependencies {
    testImplementation("dev.quokkify:q4j-html:0.5.1")
}
```

## Usage

Extract a node from an HTML response body and assert its text:

```java
Node titleNode = HtmlParser.getHtmlNode(response.getBody(), "//p[@class='title']");
assertThat(titleNode.getTextContent()).isEqualTo("Order Summary");
```

Round-trip HTML entity escaping:

```java
String escaped = HtmlParser.escapeHtml("<b>Hello & World</b>");
// "&lt;b&gt;Hello &amp; World&lt;/b&gt;"
String restored = HtmlParser.unescapeHtml(escaped);
// "<b>Hello & World</b>"
```

## Key API

| Method                          | Returns  | Notes                           |
| ------------------------------- | -------- | ------------------------------- |
| `getHtmlNode(outerHtml, xpath)` | `Node`   | `null` if XPath yields no match |
| `escapeHtml(text)`              | `String` | encodes `<`, `>`, `&`, `"`      |
| `unescapeHtml(text)`            | `String` | decodes HTML entities           |
