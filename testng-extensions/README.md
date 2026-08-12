# testng-extensions

TestNG extensions providing retry logic, group filtering, lifecycle listeners, and soft-assertion step chains for test automation.

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/q4j-testng):

```kotlin
dependencies {
    testImplementation("dev.quokkify:q4j-testng:0.4.0")
}
```

## Environment variables

| Variable            | Default         | Description                                       |
| ------------------- | --------------- | ------------------------------------------------- |
| `RETRY_COUNT`       | `2`             | Number of retries for failed tests                |
| `TEST_THREAD_COUNT` | `5`             | Parallel thread count                             |
| `TEST_GROUP`        | —               | Group name filter (used by `SingleGroupListener`) |
| `SUITE_NAME`        | `Default suite` | TestNG suite name                                 |
| `EXECUTION_MODE`    | `LOCAL`         | Execution environment: `LOCAL`/`CI`/`DIND`        |

Config is read from environment variables or `testng.properties`:

```properties
RETRY_COUNT=3
TEST_THREAD_COUNT=10
EXECUTION_MODE=CI
```

## Initialization in BaseTest

```java
@Listeners({RetryListener.class, SuiteListener.class, SingleGroupListener.class})
public abstract class BaseTest {

    protected static final TestNGExtension CONFIG =
        ConfigRegistry.getReloadable(TestNGExtension.class);
}
```

## SPI-based listener loading

TestNG supports Java's [Service Provider Interface (SPI)](https://docs.oracle.com/javase/tutorial/ext/basics/spi.html)
to auto-discover and register listeners without `@Listeners` annotation.

### How it works

Create a plain text file at:

```
src/test/resources/META-INF/services/org.testng.ITestNGListener
```

List one listener class per line:

```
dev.quokkify.listener.lifecycle.SuiteListener
dev.quokkify.listener.retry.RetryListener
```

TestNG reads this file at runtime and automatically activates every listener in the list — no
`@Listeners` annotation or `testng.xml` configuration needed.

### What `testng-extensions` registers

The module itself ships with:

```
src/test/resources/META-INF/services/org.testng.ITestNGListener
```

containing:

```
dev.quokkify.listener.lifecycle.SuiteListener
```

Because `testng-extensions` is added as `testImplementation` to every module via
`gradle/dependencies.gradle`, `SuiteListener` is active in all modules automatically — no extra
setup required.

### Adding listeners in your module

To register additional listeners in a specific module, create your own SPI file under
`src/test/resources/META-INF/services/org.testng.ITestNGListener` and list the
listeners you need:

```
dev.quokkify.listener.lifecycle.SuiteListener
dev.quokkify.listener.retry.RetryListener
dev.quokkify.listener.extension.SingleGroupListener
```

### SPI vs `@Listeners`

|                 | SPI file                                       | `@Listeners`                                     |
| --------------- | ---------------------------------------------- | ------------------------------------------------ |
| Scope           | all tests in the module                        | only the annotated class and subclasses          |
| Config location | `src/test/resources`                           | source code                                      |
| Use when        | activating suite-wide infrastructure listeners | attaching listeners to a specific test hierarchy |

## Usage in tests

Define a step class with soft-assertion verification:

```java
public class UserSteps extends AbstractSteps<UserVerification> {

    @Override
    public UserVerification verify() {
        return new UserVerification();
    }
}
```

Use `verifySoftly` to batch multiple assertions without early failure:

```java
@Listeners({RetryListener.class, SuiteListener.class})
public class UserTest extends BaseTest {

    private final UserSteps userSteps = new UserSteps();

    @TestGroup("smoke")
    @Test
    public void checkUserProfile() {
        userSteps.verifySoftly(
            v -> v.checkStatus("active"),
            v -> v.checkName("Alice")
        );
    }

    @SingleThread
    @Test
    public void checkSequentialOperation() {
        userSteps.verifySoftly(
            v -> v.checkStatus("active")
        );
    }
}
```

## Key API

| Class / Annotation                                    | Description                                                                            |
| ----------------------------------------------------- | -------------------------------------------------------------------------------------- |
| `TestNGExtension`                                     | Owner config interface; read via `ConfigRegistry.getReloadable(TestNGExtension.class)` |
| `AbstractSteps<V>`                                    | Base step class with `verify()` and `verifySoftly(consumers...)` for soft assertions   |
| `RetryListener` + `CustomRetryAnalyzer`               | Re-run failed tests up to `RETRY_COUNT` times                                          |
| `SingleGroupListener`                                 | Run only tests tagged with `TEST_GROUP`                                                |
| `SuiteListener` / `TestListener` / `StepTestListener` | Lifecycle hooks for suite/test/step events                                             |
| `CustomCasesInterceptor`                              | Filter test cases before execution                                                     |
| `@TestGroup("name")`                                  | Tag a test method with a group name                                                    |
| `@SingleThread`                                       | Force single-thread execution for annotated test                                       |
