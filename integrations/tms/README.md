# Q4J TMS

Common test management system (TMS) contract and the single TestNG entry point, `TmsListener`, that reports results to the connected TMS provider.

## Installation

Add this module together with exactly one provider, for example `testplanit`:

```kotlin
dependencies {
    testImplementation("dev.quokkify:testplanit:0.15.0")
}
```

Register the listener in `META-INF/services/org.testng.ITestNGListener` of the test project. Providers never register it themselves:

```text
dev.quokkify.tms.listeners.TmsListener
```

## One provider per project

A test project connects exactly one TMS provider (`testplanit` or `testrail-testng`).

- Build time: every provider publishes the Gradle capability `dev.quokkify:tms-provider`, so a second provider fails dependency resolution.
- Run time: `TestManagementSystems.resolve()` throws `TmsConfigurationException` when more than one provider is on the classpath, before any test runs.
- No provider, or a provider that is not configured: one `WARN` and reporting does nothing. Set `TMS_REQUIRED=true` to fail the run instead.

There is no selection key. The provider on the classpath is the selection.

## Linking tests to cases

Case ids come only from Allure `@TmsLink`. The annotation is repeatable, and a method-level link wins over a class-level one:

```java
@TmsLink("42")
@TmsLink("43")
@Test
public void userCanLogIn() { }
```

Tests without `@TmsLink` run but are not reported. Link values are plain case ids of the connected system. To render them as URLs in Allure, set `allure.link.tms.pattern` in `allure.properties`, for example `allure.link.tms.pattern=https://tms.example.com/cases/{}`.

## Configuration

Values are read from system properties, environment variables or `tms-config.properties` on the classpath.

| Key            | Default | Description                                                          |
| -------------- | ------- | -------------------------------------------------------------------- |
| `TMS_REQUIRED` | `false` | `true` fails the run when no configured provider is on the classpath |

## Provider contract

A provider implements `TestManagementSystem` and registers it through `ServiceLoader`. `shouldRun(String caseId)` lets a provider filter tests; a test runs when any of its case ids should run. Excluded methods that are disabled, or that have a ticket comment from a `TicketSource`, are reported as `BLOCKED`; the ticket comment wins over the "disabled" comment.

## Migrating from TestRail listeners

- Replace `TestStatusTestRailListener`, `AddDisabledTestsToTestRailListener` and `TestRailInterceptor` with `dev.quokkify.tms.listeners.TmsListener`. The legacy listeners are `@Deprecated(forRemoval = true)` and become no-ops with a `WARN` while `TmsListener` is active.
- Switch the Jira ticket-source module to `jira-tms`, and move custom `TicketSource` implementations to the `dev.quokkify.tms.tickets` package.
- Annotations stay as they are. Under `TmsListener`, tests without `@TmsLink` run but are not reported; previously `TestRailInterceptor` skipped them.

Moving a project from one TMS to another is one change: swap the provider dependency, rewrite `@TmsLink` values with a mapping table, and change `allure.link.tms.pattern`.

See the [Q4J module catalog](../../README.md) for the complete list of modules and common setups.
