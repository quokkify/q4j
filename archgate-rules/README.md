# Q4J ArchGate Rules

Q4J-specific architecture rules for [ArchGate](https://github.com/quokkify/archgate), and the place where Q4J
documents how its own architecture gate is wired.

## The gate in Q4J

`gradle/archgate.gradle` applies the `dev.quokkify.archgate` plugin to every module and sets
`archgate { rootPackage = 'dev.quokkify' }`. The task `archgateCheck` runs in `check`:

```bash
./gradlew archgateCheck
```

- Every module with Java code runs its own gate. Only modules without any Java code (structural parents such as
  `:common-utils:jackson:parent`) and `:archgate-rules` itself are skipped.
- Only `ERROR` findings fail the build; `WARNING` findings are reported. Pass `-Parchgate.failOn=WARNING` to fail on
  warnings too, or `-Parchgate.failOn=NEVER` to only report.
- The report is printed when the task runs and written to `build/reports/archgate/report.txt`.

### Which rules run

A module runs the rules listed in its test resources at
`META-INF/services/dev.quokkify.archgate.api.ArchitectureRule`. Two shared lists are added to the test resources
from `gradle/archgate.gradle`, so they are kept once instead of copied into each module:

| List                                                                                                         | Used by                                |
| ------------------------------------------------------------------------------------------------------------ | -------------------------------------- |
| [`tools/archgate`](../tools/archgate/META-INF/services/dev.quokkify.archgate.api.ArchitectureRule)           | modules with tests                     |
| [`tools/archgate-main`](../tools/archgate-main/META-INF/services/dev.quokkify.archgate.api.ArchitectureRule) | modules with main classes but no tests |

`tools/archgate-main` holds the rules that run on main classes alone: everything in `tools/archgate` except
`TestClassNamingRule` (test only).

A module that needs a different list keeps its own
`src/test/resources/META-INF/services/dev.quokkify.archgate.api.ArchitectureRule`; no shared list is then added.
Two modules do this:

| Module                        | Difference from the shared list                                         |
| ----------------------------- | ----------------------------------------------------------------------- |
| `:integrations:rest-assured`  | adds `HttpStatusConstantRule` for its API tests                         |
| `:integrations:selenide-grid` | has no main classes, so it leaves out the rules that check main only    |

Every list other than `tools/archgate` must name each rule of `tools/archgate` or omit it with a line
`# omitted: <rule class> <reason>`. The task `verifyArchgateRuleList`, part of `check`, fails otherwise, so a rule
added to the shared list is not silently missing from a module's own list.

The shared list:

| Rule                         | Source      | Severity | Checks      |
| ---------------------------- | ----------- | -------- | ----------- |
| `HashCodeEqualsTogetherRule` | ArchGate    | ERROR    | main        |
| `LoggerFieldNameRule`        | ArchGate    | ERROR    | main        |
| `NoConsoleOutputRule`        | ArchGate    | ERROR    | main        |
| `NoDeprecatedApiUsageRule`   | ArchGate    | WARNING  | main + test |
| `ServiceRegistrationRule`    | ArchGate    | ERROR    | main + test |
| `TestClassNamingRule`        | ArchGate    | ERROR    | test        |
| `NoInterfaceIPrefixRule`     | this module | ERROR    | main + test |
| `PackageNamingRule`          | this module | ERROR    | main + test |
| `SerialVersionUidRule`       | this module | ERROR    | main + test |

The ArchGate rules are documented in the
[ArchGate rule catalog](https://github.com/quokkify/archgate#built-in-rules).

## Rules in this module

All rules are in the package `dev.quokkify.arch`.

| Rule                     | Backend    | Enforces                                                                                        |
| ------------------------ | ---------- | ----------------------------------------------------------------------------------------------- |
| `HttpStatusConstantRule` | JavaParser | API tests pass `org.apache.http.HttpStatus` constants, not literals 100-599, to `*Status` calls |
| `SerialVersionUidRule`   | ArchUnit   | a `serialVersionUID` field is `static final long`                                               |
| `PackageNamingRule`      | ArchUnit   | package segments are lower case letters and digits, start with a letter, have no underscore     |
| `NoInterfaceIPrefixRule` | ArchUnit   | interface names do not start with `I` followed by an upper-case letter                          |

`HttpStatusConstantRule` is described in the [REST Assured module](../integrations/rest-assured/README.md#architecture-rule-http-status-constants).
Register it only in modules whose tests use REST Assured and contain API tests: a module without API tests
cannot be verified, and the gate fails.

## Use in another project

```kotlin
plugins {
    id("dev.quokkify.archgate") version "<archgate version>"
}

dependencies {
    testImplementation(platform("dev.quokkify.archgate:archgate-bom:<archgate version>"))
    testImplementation("dev.quokkify:archgate-rules:<q4j version>")
}
```

Then list the rules in `src/test/resources/META-INF/services/dev.quokkify.archgate.api.ArchitectureRule`. The jar
declares the `archgate-api` version it was built against in its `Archgate-Api-Version` manifest attribute, so an
older ArchGate plugin fails with a clear message instead of a linkage error.

## Adding a rule

1. Implement `dev.quokkify.archgate.api.ArchitectureRule` in `src/main/java/dev/quokkify/arch`, following ArchGate's
   [extension contract](https://github.com/quokkify/archgate/blob/main/docs/extending.md): keep no state in fields,
   and throw `UnableToVerifyError` instead of passing when there is nothing to check.
2. Add a test with fixtures: violating and clean classes in `src/test/java/archfixtures`, or sources in
   `src/test/resources`, and an unable-to-verify case.
3. Register the rule in `tools/archgate`, and add it to or omit it from `tools/archgate-main` and every
   module-specific list; `verifyArchgateRuleList` reports a list that does neither.

This module's own `archgateCheck` is skipped and its rules are verified by the fixture tests instead: ArchGate's
registration check would require every rule of this module in its own list, and `HttpStatusConstantRule` cannot be
verified here because the module has no API tests.
