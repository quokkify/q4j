# Q4J ReportPortal TestRail

TestRail description provider for Q4J ReportPortal reporting.

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/reportportal-testrail):

```kotlin
dependencies {
    testImplementation("dev.quokkify:reportportal-testrail:0.15.0")
}
```

This module depends on `testrail-testng`, so it carries the TMS provider capability `dev.quokkify:tms-provider` and cannot be combined with `testplanit`.

See the [Q4J module catalog](../../../README.md) for the complete list of modules and common setups.
