# Q4J TMS Core

Test management system (TMS) provider contract, ticket sources and result model for Q4J. The module has no TestNG dependency; the listener lives in [`tms-testng`](../testng/README.md).

## Installation

The `testplanit` provider and the `jira-tms` ticket source depend on this module, so you rarely add it yourself. Add it directly when you implement a provider or a `TicketSource`:

```kotlin
dependencies {
    implementation("dev.quokkify:tms-core:0.15.0")
}
```

## Provider contract

A provider implements `TestManagementSystem` and registers it through `ServiceLoader`. `shouldRun(String caseId)` lets a provider filter tests; a test runs when any of its case ids should run. `TestManagementSystems.resolve()` returns the single provider on the classpath and throws `TmsConfigurationException` when more than one is present.

Every provider publishes the Gradle capability `dev.quokkify:tms-provider` (see `gradle/tms-provider.gradle`), so a second provider fails dependency resolution. Neither `tms-core` nor `tms-testng` carries the capability.

## Ticket sources

`dev.quokkify.tms.tickets.TicketSource` maps a test to a ticket comment, for example from Jira through `jira-tms`. `TicketComments` collects the comments of all registered sources. Excluded methods that have a ticket comment are reported as `BLOCKED`.

## Result model

`TmsResult` carries the case id, the `TmsStatus` and the comment that a provider sends to its system.

## Configuration

Values are read from system properties, environment variables or `tms-config.properties` on the classpath.

| Key            | Default | Description                                                          |
| -------------- | ------- | -------------------------------------------------------------------- |
| `TMS_REQUIRED` | `false` | `true` fails the run when no configured provider is on the classpath |

See the [Q4J module catalog](../../../README.md) for the complete list of modules and common setups.
