# Q4J TestRail TestNG

TestRail provider for the Q4J TMS entry point.

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/testrail-testng):

```kotlin
dependencies {
    testImplementation("dev.quokkify:testrail-testng:0.15.0")
}
```

Register `dev.quokkify.tms.listeners.TmsListener` in `META-INF/services/org.testng.ITestNGListener`. The module is a TMS provider (capability `dev.quokkify:tms-provider`), so it cannot be combined with `testplanit` or another provider. The published Gradle metadata also advertises the in-build capability `dev.quokkify.testrail:testng` (used for in-build identity); consumers should depend on `dev.quokkify:testrail-testng`.

Case ids come from `@TmsLink` (repeatable). `TestRailTestFilterRules.needRunTest` decides which tests run. Tests without `@TmsLink` run but are not reported.

## Migrating from the legacy listeners

`TestStatusTestRailListener`, `AddDisabledTestsToTestRailListener` and `TestRailInterceptor` are `@Deprecated(forRemoval = true)` and become no-ops with a `WARN` while `TmsListener` is active. Replace the three listener lines with `dev.quokkify.tms.listeners.TmsListener`, switch the Jira ticket-source module to `jira-tms`, and move custom `TicketSource` implementations to `dev.quokkify.tms.tickets`.

See the [Q4J module catalog](../../../README.md) for the complete list of modules and common setups.
