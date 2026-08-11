# ReportPortal integration

Run local ReportPortal stack:

```bash
./tools/environment/scripts/infra/run_app.sh reporting
```

This command creates `tools/environment/.reportportal.env` with connection details and token.
It also creates Owner-based test config:
`integrations/reportportal/testng/src/test/resources/local_resources/reportportal-test.properties`.

Run integration test:

```bash
./gradlew :integrations:reportportal:testng:test
```

CI-style run (same flow as workflow):

```bash
CI=true EXECUTION_MODE=CI ./tools/environment/scripts/infra/run_app.sh reporting
./gradlew :integrations:reportportal:testng:check --no-daemon --console=plain --stacktrace
CI=true ./tools/environment/scripts/infra/stop_app.sh reporting
```

Stop infrastructure:

```bash
./tools/environment/scripts/infra/stop_app.sh reporting
```

## TMS integration via SPI

`ParamOverrideTestNgService` enriches ReportPortal test and launch descriptions through the
`TmsDescriptionProvider` SPI. Providers are discovered at runtime via `ServiceLoader`.

### Built-in providers

| Module                  | Provider                      | When active                                                |
| ----------------------- | ----------------------------- | ---------------------------------------------------------- |
| `reportportal:testng`   | `NoOpTmsDescriptionProvider`  | fallback when no provider is enabled                       |
| `reportportal:testrail` | `TestRailDescriptionProvider` | when `IS_TESTRAIL_DISABLED=false` and `TESTRAIL_ID` is set |

### Adding a custom provider

1. Implement `dev.quokkify.reportportal.spi.TmsDescriptionProvider`.
2. Register it in `src/main/resources/META-INF/services/dev.quokkify.reportportal.spi.TmsDescriptionProvider`.
3. Add your module as a runtime dependency alongside `reportportal:testng`.

### Enabling the TestRail provider

Add `:integrations:reportportal:testrail` to your module's dependencies:

```groovy
testImplementation project(":integrations:reportportal:testrail")
```

Set the required properties (via environment variables or `.properties` file):

```
IS_TESTRAIL_DISABLED=false
TESTRAIL_ID=<plan-or-run-id>
TESTRAIL_CASE_URL=https://your-instance.testrail.io/index.php?/cases/view/%s
TESTRAIL_TESTPLAN_URL=https://your-instance.testrail.io/index.php?/plans/%s
TESTRAIL_TESTRUN_URL=https://your-instance.testrail.io/index.php?/runs/%s
```
