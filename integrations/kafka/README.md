# Kafka integration

Provides a fluent verification API for asserting message presence and count in Kafka topics,
with configurable polling timeout and Allure step reporting.

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/q4j-kafka):

```kotlin
dependencies {
    testImplementation("dev.quokkify:q4j-kafka:0.5.1")
}
```

---

## API overview

Extend `BaseKafkaSteps` with a typed verifier to build a domain-specific API:

```java
public class OrderKafkaSteps extends BaseKafkaSteps<OrderKafkaSteps, OrderMessage, KafkaVerifier<OrderMessage>> {

    @Override
    public KafkaVerifier<OrderMessage> verify() {
        return new KafkaVerifier<>(this::readMessageValues);
    }
}
```

Use in tests:

```java
kafkaSteps.verify()
    .containsMessage(msg -> msg.getOrderId().equals("order-99"))
    .hasMessageCount(1, msg -> msg.getStatus().equals("CREATED"));
```

Adjust timing per-call:

```java
kafkaSteps.verify()
    .withTimeout(Duration.ofSeconds(60))
    .withPolling(Duration.ofMillis(2000))
    .containsMessage(msg -> msg.getEventType().equals("ORDER_SHIPPED"));
```

## Verification methods

| Method                                | Description                                                           |
| ------------------------------------- | --------------------------------------------------------------------- |
| `containsMessage(Predicate<M>)`       | Waits until at least one consumed message matches the predicate       |
| `doesNotContainMessage(Predicate<M>)` | Asserts no matching message appears within the configured window      |
| `hasMessageCount(int, Predicate<M>)`  | Waits until at least N messages matching the predicate have been seen |

Default timeout is 30 seconds with 1 second polling.

---

## Run local Kafka stack

```bash
./tools/environment/scripts/infra/run_app.sh messaging
```

This command creates `tools/environment/.kafka.env` with Kafka bootstrap server and Kafka UI URL.

Run integration test:

```bash
./gradlew :integrations:kafka:test
```

CI-style run (same flow as workflow):

```bash
CI=true EXECUTION_MODE=CI ./tools/environment/scripts/infra/run_app.sh messaging
set -a && source tools/environment/.kafka.env && set +a
export KAFKA_SERVER_ADDRESS="${KAFKA_BOOTSTRAP_SERVERS}"
./gradlew :integrations:kafka:check --no-daemon --console=plain --stacktrace
CI=true ./tools/environment/scripts/infra/stop_app.sh messaging
```

Stop infrastructure:

```bash
./tools/environment/scripts/infra/stop_app.sh messaging
```
