# RabbitMQ integration

Provides a fluent verification API for asserting message presence and content in RabbitMQ queues,
with configurable polling timeout and Allure step reporting.

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/q4j-rabbitmq):

```kotlin
dependencies {
    testImplementation("dev.quokkify:q4j-rabbitmq:0.2.3")
}
```

---

## API overview

```java
rabbitSteps.verify()
    .hasMessage("orders")
    .hasMessageWithBody("orders", "order_created");
```

Adjust timing per-call:

```java
rabbitSteps.verify()
    .withTimeout(Duration.ofSeconds(30))
    .withPolling(Duration.ofMillis(500))
    .hasMessage("payments");
```

## Verification methods

| Method                                            | Description                                                          |
| ------------------------------------------------- | -------------------------------------------------------------------- |
| `hasMessage(String queue)`                        | Waits until the queue has at least one message                       |
| `hasMessage(String queue, Predicate<...> pred)`   | Waits until a message matching the predicate appears in the queue    |
| `hasMessageWithBody(String queue, String substr)` | Waits until a message whose body contains the substring is present   |
| `doesNotHaveMessage(String queue)`                | Asserts no message appears in the queue within the configured window |

Default timeout is 10 seconds with 500 ms polling.

---

## Run local RabbitMQ stack

```bash
./tools/environment/scripts/infra/run_app.sh rabbitmq
```

This command creates `tools/environment/.rabbitmq.env` with RabbitMQ connection details.
It also creates Owner-based test config:
`integrations/rabbitmq/src/test/resources/local_resources/rabbit.properties`.

Run integration test:

```bash
./gradlew :integrations:rabbitmq:test
```

CI-style run (same flow as workflow):

```bash
CI=true EXECUTION_MODE=CI ./tools/environment/scripts/infra/run_app.sh rabbitmq
set -a && source tools/environment/.rabbitmq.env && set +a
./gradlew :integrations:rabbitmq:check --no-daemon --console=plain --stacktrace
CI=true ./tools/environment/scripts/infra/stop_app.sh rabbitmq
```

Stop infrastructure:

```bash
./tools/environment/scripts/infra/stop_app.sh rabbitmq
```
