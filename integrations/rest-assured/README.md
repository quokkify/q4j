# integrations/rest-assured

REST-Assured based HTTP client wrapper for test automation with fluent request building,
JSON schema validation, and Allure step reporting.

---

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/rest-assured):

```kotlin
dependencies {
    testImplementation("dev.quokkify:rest-assured:0.7.0")
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

## Architecture rule: HTTP status constants

`dev.quokkify.architecture.rules.HttpStatusConstantRule` is an
[architecture](../../architecture/README.md) rule for projects that use this module. In API tests it reports
every call to a method whose name contains `status`, such as `verifyResponseStatusCode`, that passes an
integer literal between 100 and 599. The report names the `org.apache.http.HttpStatus` constant to use:

```text
src/test/java/com/example/test/api/HealthTest.java line 14: verifyResponseStatusCode(...) passes 200; use HttpStatus.SC_OK
```

The compiler inlines `HttpStatus.SC_OK` into `200`, so the rule reads the test sources. The runner therefore
needs `architecture.test.sources`.

An API test is a test source in a package with a segment named after the marker, such as `com.example.test.api`,
or a method annotated with `@Test(groups = ...)` whose groups include the marker. The marker defaults to `api`;
set `-Darchitecture.api.package` on the runner or the `ARCHITECTURE_API_PACKAGE` environment variable to change
it. If test sources exist but none matches the marker, the run aborts instead of passing.

Register the rule next to the other rules of the project:

```text
# META-INF/services/dev.quokkify.architecture.contract.ArchitectureRule
dev.quokkify.architecture.rules.HttpStatusConstantRule
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
