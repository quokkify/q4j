# common-utils/jackson/yaml

Load YAML classpath resources into typed Java objects, lists, or key-ordered maps using SnakeYAML with safe construction.

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/q4j-jackson-yaml):

```kotlin
dependencies {
    testImplementation("dev.quokkify:q4j-jackson-yaml:0.2.3")
}
```

## Usage

No initialization required — all methods are static.

## Usage in tests

```java
// Load a list of test-data users from a YAML sequence file
List<User> users = YamlParser.loadListFromResources("data/users.yaml", User.class);

assertThat(users).hasSize(3);
assertThat(users.get(0).getEmail()).isEqualTo("alice@example.com");

// Load an environment config object from a YAML mapping
AppConfig cfg = YamlParser.loadAsObjectFromResources("config/settings.yaml", AppConfig.class);

assertThat(cfg.getBaseUrl()).startsWith("https://");

// Load a named map and iterate values in declared order
Map<String, User> byId = YamlParser.loadAsMapFromResources("data/users.yaml", User.class);
List<User> values       = YamlParser.loadValuesFromMapFromResources("data/users.yaml", User.class);
```

## Key API

| Method                                           | Description                                                 |
| ------------------------------------------------ | ----------------------------------------------------------- |
| `loadAsObjectFromResources(path, Class<T>)`      | Load YAML document as typed object                          |
| `loadListFromResources(path, Class<T>)`          | Load YAML sequence as `List<T>`                             |
| `loadAsMapFromResources(path, Class<T>)`         | Load YAML mapping as `Map<String, T>` (key order preserved) |
| `loadValuesFromMapFromResources(path, Class<T>)` | Load values of a YAML mapping as `List<T>`                  |
| `load(file)`                                     | Load YAML file as raw object                                |
