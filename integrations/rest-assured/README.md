# integrations/rest-assured

REST-Assured based HTTP client wrapper for test automation with fluent request building,
JSON schema validation, and Allure step reporting.

---

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/rest-assured):

```kotlin
dependencies {
    testImplementation("dev.quokkify:rest-assured:0.14.1")
}
```

The module brings `dev.quokkify:testng` (`AbstractSteps`) transitively; do not declare it separately.

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

`dev.quokkify.arch.HttpStatusConstantRule` is an [ArchGate](https://github.com/quokkify/archgate) rule for projects
that use this module. It ships in [`dev.quokkify:archgate-rules`](../../archgate-rules/README.md), not in this
module. In API tests it reports every call to a method whose name ends in `Status` or `StatusCode`, such as
`verifyResponseStatusCode`, that passes an integer literal between 100 and 599 as a direct argument. The report names
the `org.apache.http.HttpStatus` constant to use:

```text
com/example/test/api/HealthTest.java line 14: verifyResponseStatusCode(...) passes 200; use HttpStatus.SC_OK
```

The compiler inlines `HttpStatus.SC_OK` into `200`, so the rule reads the test sources through ArchGate's
JavaParser backend.

An API test is a test source in a package with a segment named after the marker, such as `com.example.test.api`,
or a class or method annotated with `@Test(groups = ...)` whose groups include the marker as a string literal. The
marker defaults to `api`; set the `archgate.api.package` system property or the `ARCHGATE_API_PACKAGE` environment
variable to change it, or subclass the rule with a public no-arg constructor that calls
`super("<marker>")` and register the subclass. If test sources exist but none matches the marker, the gate reports
that it is unable to verify and fails.

Add the rules artifact next to the ArchGate plugin and register the rule in
`src/test/resources/META-INF/services/dev.quokkify.archgate.api.ArchitectureRule`:

```kotlin
dependencies {
    testImplementation("dev.quokkify:archgate-rules:<version>")
}
```

```text
dev.quokkify.arch.HttpStatusConstantRule
```

The test runtime classpath must hold `org.apache.http.HttpStatus`, which this module brings through REST Assured.
Q4J registers the rule for its own API tests in this module, marked with `@Test(groups = "api")`.

---

## Verification methods

| Method                                           | Description                                          |
| ------------------------------------------------ | ---------------------------------------------------- |
| `verifyResponseStatusCode(response, int)`        | Asserts a single response has the expected status    |
| `verifyResponseStatusCode(List<response>, int)`  | Soft-asserts all responses have the expected status  |
| `verifyResponseBody(response, String)`           | Asserts the response body equals the expected string |
| `verifyResponseBody(List<response>, String)`     | Soft-asserts all response bodies match               |
| `verifyResponseSchema(response, JsonValidation)` | Validates the response body against a JSON schema    |
