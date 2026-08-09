# common-utils/awaitility

Fluent wrapper around Awaitility for polling assertions and conditions in tests, with preset timeout and interval constants.

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/q4j-awaitility):

```kotlin
dependencies {
    testImplementation("dev.quokkify:q4j-awaitility:0.2.2")
}
```

## Usage

Poll until an AssertJ assertion passes (60s timeout, 5s poll):

```java
Waiter.awaitAssertion(() -> assertThat(order.getStatus()).isEqualTo("CONFIRMED"));
```

Quick check with a custom timeout (500ms poll):

```java
Waiter.awaitQuickAssertion(() -> assertTrue(cache.containsKey("session-123")), Timeout.SECONDS_10);
```

Wait for a supplier value to match a Hamcrest matcher — using `Duration` directly:

```java
Waiter.awaitCondition(
        () -> fetchJobStatus(),
        Matchers.equalTo("COMPLETED"),
        "Job never reached COMPLETED",
        Duration.ofSeconds(30),
        Duration.ofMillis(1000)
);
```

Assert a flag never flips to true (e.g. no error popup appears for 10 seconds):

```java
Waiter.assertNeverTrue(
        () -> errorPopup.isDisplayed(),
        Duration.ofSeconds(10),
        Duration.ofMillis(500),
        "Error popup appeared unexpectedly"
);
```

Assert a condition holds true for the full window (e.g. status stays ACTIVE):

```java
Waiter.assertAlwaysTrue(
        () -> "ACTIVE".equals(fetchStatus()),
        Duration.ofSeconds(30),
        Duration.ofMillis(1000),
        "Status dropped from ACTIVE before expected"
);
```

> **Enum overloads**: `Timeout` and `PollingInterval` enum overloads remain available as convenience wrappers
> (e.g. `Timeout.SECONDS_30`, `PollingInterval.MILLIS_1000`) and delegate to the `Duration` forms internally.
> Prefer `Duration` for new code.

## Key API

| Method                                                          | Timeout | Poll    | Notes                                    |
| --------------------------------------------------------------- | ------- | ------- | ---------------------------------------- |
| `awaitAssertion(assertion)`                                     | 60s     | 5s      | AssertJ / TestNG assertion               |
| `awaitQuickAssertion(assertion)`                                | 5s      | 500ms   | fast path, no overrides                  |
| `awaitQuickAssertion(assertion, timeout)`                       | custom  | 500ms   | custom `Timeout` constant                |
| `awaitCondition(callable, message, timeout, interval)`          | custom  | custom  | boolean `Callable`; accepts `Duration`   |
| `awaitCondition(supplier, matcher, message, timeout, interval)` | custom  | custom  | Hamcrest `Matcher`; accepts `Duration`   |
| `awaitConditionWithAction(condition, action, message)`          | default | default | runs `action` each tick                  |
| `threadSleep(millis)`                                           | —       | —       | safe sleep, handles interrupt            |
| `waitForNextSecond()`                                           | —       | —       | waits until clock ticks                  |
| `assertNeverTrue(condition, timeout, interval, message)`        | custom  | custom  | fails if condition ever becomes `true`   |
| `assertNeverTrue(condition, message)`                           | 60s     | 1s      | short form with defaults                 |
| `assertAlwaysTrue(condition, timeout, interval, message)`       | custom  | custom  | fails if condition ever drops to `false` |
| `assertAlwaysTrue(condition, message)`                          | 60s     | 1s      | short form with defaults                 |
