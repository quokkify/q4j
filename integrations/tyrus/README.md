# Tyrus WebSocket integration

WebSocket client wrapper for test automation built on [Jakarta WebSocket (JSR-356)](https://jakarta.ee/specifications/websocket/)
via [Eclipse Tyrus 2.x](https://eclipse-ee4j.github.io/tyrus/).

Provides a fluent API for connecting, sending messages, and asserting received messages with
built-in polling, timeout configuration, and Allure step reporting.

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/q4j-tyrus):

```kotlin
dependencies {
    testImplementation("dev.quokkify:q4j-tyrus:0.6.1")
}
```

---

## API overview

### Generic steps

```java
WsSteps wsSteps = new WsSteps();

wsSteps.connect("ws://localhost:8787/ws")
    .sendMessage("{\"action\":\"subscribe\",\"channel\":\"orders\"}")
    .verify()
    .containsMessage("order_created")
    .hasJsonField("type", "order_created")
    .hasMessageCount(1);
```

### Typed steps (domain-specific)

Extend `AbstractWsSteps` and `BaseWsVerification` to build a fully typed API for your service:

```java
// 1. Typed verifier
public class OrderWsVerification extends BaseWsVerification<OrderWsVerification> {

    public OrderWsVerification(WsClient client) {
        super(client);
    }

    @Override
    protected OrderWsVerification self() {
        return this;
    }

    @Step("Verify order created: {orderId}")
    public OrderWsVerification verifyOrderCreated(String orderId) {
        return hasJsonField("orderId", orderId)
            .hasJsonField("type", "order_created");
    }
}

// 2. Typed steps
public class OrderWsSteps extends AbstractWsSteps<OrderWsVerification> {

    @Step("Connect to Orders WebSocket")
    public OrderWsSteps connect() {
        WsClient client = WsClient.connect();
        this.verification = new OrderWsVerification(client);
        return this;
    }

    @Step("Subscribe to order updates: {userId}")
    public OrderWsSteps subscribeToOrders(String userId) {
        // access client via verification.client (protected field)
        verification.client.sendMessage("{\"action\":\"subscribe\",\"userId\":\"" + userId + "\"}");
        return this;
    }
}

// 3. Usage in test
orderWsSteps.connect()
    .subscribeToOrders("user-42")
    .verify()
    .withTimeout(Duration.ofSeconds(15))
    .verifyOrderCreated("order-99")
    .hasMessageCount(1);
```

---

## Verification methods

| Method                                  | Description                                                        |
| --------------------------------------- | ------------------------------------------------------------------ |
| `containsMessage(String)`               | Waits until any message payload contains the substring             |
| `containsMessage(Predicate<WsMessage>)` | Waits until any message matches the predicate                      |
| `doesNotContainMessage(String)`         | Asserts the substring never appears within the configured timeout  |
| `hasJsonField(String, String)`          | Waits until any message has the JSON field with the expected value |
| `hasMessageCount(int)`                  | Waits until at least N messages have been received                 |
| `messagesInOrder(String...)`            | Waits until substrings appear across messages in the given order   |

### Timeout configuration

Default timeout is 10 seconds with 500 ms polling. Override per-assertion:

```java
wsSteps.verify()
    .withTimeout(Duration.ofSeconds(30))
    .withPolling(Duration.ofMillis(100))
    .containsMessage("slow_event");
```

---

## Configuration

| Property | Environment variable | Default                  |
| -------- | -------------------- | ------------------------ |
| `WS_URL` | `WS_URL`             | `ws://localhost:8787/ws` |

Override via system property, environment variable, or:

```
src/test/resources/local_resources/websockets.properties
```

---

## Run tests

```bash
./gradlew :integrations:tyrus:test
```

CI-style run:

```bash
./gradlew :integrations:tyrus:check --no-daemon --console=plain --stacktrace
```
