# common-utils/config

Type-safe Owner configuration backed by environment variables and classpath properties, with singleton caching and runtime overlay support.

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/q4j-config):

```kotlin
dependencies {
    testImplementation("dev.quokkify:q4j-config:0.5.1")
}
```

## Initialization in BaseTest

```java
@Config.Sources({"system:env", "classpath:app.properties"})
interface AppConfig extends Config {
    @Key("API_URL")   String apiUrl();
    @Key("API_TOKEN") String apiToken();
}

public abstract class BaseTest {
    protected static AppConfig config;

    @BeforeClass
    public static void initConfig() {
        config = ConfigRegistry.get(AppConfig.class);
    }
}
```

## Usage in tests

```java
public class OrderApiTest extends BaseTest {

    @Test
    public void createsOrderWithStagingOverride() {
        ConfigRegistry.overlay(config, Map.of("API_URL", "https://staging.example.com"));

        var client = new OrderClient(config.apiUrl(), config.apiToken());
        var order  = client.create(OrderRequest.defaultPayload());

        assertThat(order.getId()).isNotNull();
    }

    @Test
    public void readsBaseUrlFromConfig() {
        assertThat(config.apiUrl()).startsWith("https://");
    }
}
```

## Key API

| Method                                         | Description                           |
| ---------------------------------------------- | ------------------------------------- |
| `ConfigRegistry.get(MyConfig.class)`           | Cached read-only config singleton     |
| `ConfigRegistry.getMutable(MyConfig.class)`    | Cached mutable config singleton       |
| `ConfigRegistry.getReloadable(MyConfig.class)` | Cached mutable + reloadable singleton |
| `ConfigRegistry.overlay(cfg, Map.of("K","v"))` | Apply runtime overrides and reload    |
