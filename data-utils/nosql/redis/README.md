# data-utils/nosql/redis

Redis access utility for test automation, built on [Redisson](https://github.com/redisson/redisson),
with a fluent verification API, configurable polling timeout, and Allure step reporting.

---

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/q4j-redis):

```kotlin
dependencies {
    testImplementation("dev.quokkify:q4j-redis:0.6.1")
}
```

---

## Environment variables

| Variable              | Default     | Description                                       |
| --------------------- | ----------- | ------------------------------------------------- |
| `REDIS_HOST`          | `127.0.0.1` | Redis host                                        |
| `REDIS_PORT`          | `6379`      | Redis port                                        |
| `REDIS_PASSWORD`      | —           | Redis password (optional)                         |
| `REDIS_CLUSTER_NODES` | —           | Comma-separated `host:port` list for cluster mode |

---

## Initialization in BaseTest

```java
public abstract class BaseTest {

    protected RedisSteps redisSteps;

    @BeforeClass(alwaysRun = true)
    public void initRedis() {
        String host = System.getenv().getOrDefault("REDIS_HOST", "127.0.0.1");
        String port = System.getenv().getOrDefault("REDIS_PORT", "6379");
        String password = System.getenv().getOrDefault("REDIS_PASSWORD", "");

        Config config = new Config();
        config.setCodec(StringCodec.INSTANCE);
        config.useSingleServer()
            .setAddress("redis://" + host + ":" + port)
            .setPassword(password.isBlank() ? null : password);

        RedissonClient client = Redisson.create(config);
        redisSteps = new RedisSteps(client);
    }
}
```

---

## API overview

```java
redisSteps.verify()
    .hasKey("orders:123")
    .hasValue("orders:123", "pending")
    .hasMapEntry("user:42", "status", "active")
    .hasSetMember("online-users", "user-99");
```

Adjust timing per-call:

```java
redisSteps.verify()
    .withTimeout(Duration.ofSeconds(30))
    .withPolling(Duration.ofMillis(500))
    .hasKey("session:abc");
```

Default timeout is 10 seconds with 500 ms polling.

---

## Verification methods

| Method                                                   | Description                                                  |
| -------------------------------------------------------- | ------------------------------------------------------------ |
| `hasKey(String key)`                                     | Waits until the key exists in Redis                          |
| `doesNotHaveKey(String key)`                             | Asserts the key does not appear within the configured window |
| `hasValue(String key, String expectedValue)`             | Waits until the bucket at `key` equals `expectedValue`       |
| `hasMapEntry(String mapKey, String field, String value)` | Waits until the map field equals the expected value          |
| `hasSetMember(String setKey, String member)`             | Waits until the set contains `member`                        |

---

## Extending with domain-specific assertions

```java
public class CacheVerification extends BaseRedisVerification<CacheVerification> {

    public CacheVerification(RedissonClient client) {
        super(client);
    }

    @Override
    protected CacheVerification self() {
        return this;
    }

    @Step("Cache: session '{sessionId}' is active")
    public CacheVerification hasActiveSession(String sessionId) {
        return hasKey("session:" + sessionId);
    }
}
```

---

## Run local Redis stack

```bash
./tools/environment/scripts/infra/run_app.sh redis
```

Stop infrastructure:

```bash
./tools/environment/scripts/infra/stop_app.sh redis
```
