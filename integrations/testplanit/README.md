# Q4J TestPlanIt

[TestPlanIt](https://github.com/testplanit/testplanit) integration for Q4J: reports TestNG results to a TestPlanIt run through the API.

## Installation

```kotlin
dependencies {
    testImplementation("dev.quokkify:testplanit:0.15.0")
}
```

Register the common listener in `src/test/resources/META-INF/services/org.testng.ITestNGListener`:

```text
dev.quokkify.tms.listeners.TmsListener
```

TestPlanIt is the only TMS provider a project may connect: it cannot be combined with `testrail-testng`, and Gradle fails dependency resolution on the `dev.quokkify:tms-provider` capability conflict. Without the API settings below the integration logs one `WARN` and reports nothing, unless `TMS_REQUIRED=true`.

## Linking tests to cases

```java
@TmsLink("42")             // TestPlanIt case id; repeatable, class-level links apply to every method
@Test
public void userCanLogIn() { }
```

A test can carry several `@TmsLink` annotations. Tests without `@TmsLink` run but are not reported. Non-numeric ids are skipped with a warning.
Cases are never created by the integration: an id that does not exist in the project is skipped with a warning.

## Configuration

Values are read from system properties, environment variables or `testplanit-config.properties` on the classpath.

| Key                       | Required | Description                                                      |
| ------------------------- | -------- | ---------------------------------------------------------------- |
| `TESTPLANIT_API_URL`      | yes      | TestPlanIt base URL                                              |
| `TESTPLANIT_API_TOKEN`    | yes      | API token `tpi_...`; pass it through the environment only        |
| `TESTPLANIT_PROJECT_ID`   | yes      | Project that holds the cases                                     |
| `TESTPLANIT_RUN_ID`       | no       | Existing run to append results to; a new run is created if empty |
| `TESTPLANIT_RUN_NAME`     | no       | Name of the new run, `q4j <timestamp>` by default                |
| `TESTPLANIT_CONFIG_ID`    | no       | Configuration of the new run                                     |
| `TESTPLANIT_MILESTONE_ID` | no       | Milestone of the new run                                         |
| `TESTPLANIT_COMPLETE_RUN` | no       | `true` completes the run after all suites, `false` by default    |
| `IS_TESTPLANIT_DISABLED`  | no       | `true` disables the integration                                  |

## Run behaviour

- The run and its cases are prepared when a suite starts; each case is added to the run once.
- Every result is submitted through `POST /api/test-runs/submit-result`, so the run case status follows the latest result. A repeated execution of the same case adds a new attempt instead of a new case.
- Status mapping: passed → `passed`, failed → `failed`, skipped → `skipped`, disabled (`@Test(enabled = false)`) → `blocked`. Retried attempts are not reported, only the final one.
- The failure message is stored in the result notes; the execution time goes to the result `elapsed` field in whole seconds (a sub-second test counts as 1 second).
- Results within `successPercentage` are reported as passed. Several results of one case in one execution (data providers, `invocationCount`, shards sharing `TESTPLANIT_RUN_ID`) become separate attempts; the run case shows the status of the last one received.

## Local and CI environment

The docker-compose profile `testplanit` starts TestPlanIt and a bootstrap step that creates an admin user, a project and an API token, and writes them to `tools/environment/.testplanit.env` (`TESTPLANIT_API_URL`, `TESTPLANIT_API_TOKEN`, `TESTPLANIT_PROJECT_ID`). The CI job for `:integrations:testplanit` uses this profile.

See the [Q4J module catalog](../../README.md) for the complete list of modules and common setups.
