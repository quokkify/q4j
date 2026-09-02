# integrations/rest-assured

REST-Assured based HTTP client wrapper for test automation with fluent request building,
JSON schema validation, and Allure step reporting.

---

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/q4j-rest-assured):

```kotlin
dependencies {
    testImplementation("dev.quokkify:q4j-rest-assured:0.6.2")
}
```

---

## API overview

### Status code

```java
apiSteps.verify()
    .verifyResponseStatusCode(response, 200);
```

Multiple responses:

```java
apiSteps.verify()
    .verifyResponseStatusCode(responses, 200);
```

### Body

```java
apiSteps.verify()
    .verifyResponseBody(response, "{\"status\":\"ok\"}");
```

### JSON schema

```java
apiSteps.verify()
    .verifyResponseSchema(response, MyJsonSchema.SCHEMA);
```

### Timeout configuration

The verification object carries `timeout` and `pollingInterval` fields for use by subclasses
that add polling assertions. Override per-call:

```java
apiSteps.verify()
    .withTimeout(Duration.ofSeconds(30))
    .withPolling(Duration.ofMillis(1000))
    .verifyResponseStatusCode(response, 200);
```

Default timeout is 10 seconds with 500 ms polling.

---

## Extending with domain-specific assertions

```java
public class OrderApiVerification extends BaseApiVerification<OrderApiVerification> {

    @Override
    protected OrderApiVerification self() {
        return this;
    }

    @Step("Order status is {expectedStatus}")
    public OrderApiVerification hasOrderStatus(ValidatableResponse response, String expectedStatus) {
        response.body("status", Matchers.equalTo(expectedStatus));
        return self();
    }
}
```

---

## Verification methods

| Method                                           | Description                                          |
| ------------------------------------------------ | ---------------------------------------------------- |
| `verifyResponseStatusCode(response, int)`        | Asserts a single response has the expected status    |
| `verifyResponseStatusCode(List<response>, int)`  | Soft-asserts all responses have the expected status  |
| `verifyResponseBody(response, String)`           | Asserts the response body equals the expected string |
| `verifyResponseBody(List<response>, String)`     | Soft-asserts all response bodies match               |
| `verifyResponseSchema(response, JsonValidation)` | Validates the response body against a JSON schema    |
