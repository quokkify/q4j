# Single TMS entry point — design

Status: approved by the owner on 2026-11-13. Supersedes the first TestPlanIt pass on branch `feat/testplanit-integration` (config key `TMS`, `@TestPlanItCase`).

## Requirements

1. One abstract entry point for test management system (TMS) integration: a test project registers one listener, `dev.quokkify.tms.listeners.TmsListener`, and adds one provider dependency.
2. A test project connects exactly one TMS provider. Two or more is an error, enforced twice:
   - build time: every provider publishes the Gradle capability `dev.quokkify:tms-provider`; two providers fail dependency resolution;
   - run time: `TestManagementSystems.resolve()` throws when `ServiceLoader` finds more than one provider, before any test runs.
3. No custom annotations. Case ids come only from Allure `@TmsLink` / `@TmsLinks`, read with `getAnnotationsByType` on the method, then on the class. With one TMS per project, `@TmsLink("6")` always means case 6 in the connected system.

## Decisions

| Topic                     | Decision                                                                                                                                                                                                |
| ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Listener registration     | Explicit line in the consumer's `META-INF/services/org.testng.ITestNGListener`; providers never auto-register it                                                                                        |
| Zero providers            | One `WARN`, reporting is a no-op; `TMS_REQUIRED=true` turns it into an error                                                                                                                            |
| Provider not configured   | Same as zero providers                                                                                                                                                                                  |
| Selection key `TMS`       | Removed together with every `isSelected` gate (it never shipped in `main`)                                                                                                                              |
| Unlinked tests            | Run, not reported (all providers)                                                                                                                                                                       |
| Run filtering             | SPI `default boolean shouldRun(String caseId)`; TestRail delegates to `TestRailTestFilterRules.needRunTest`; a test runs if any of its case ids should run                                              |
| Disabled / excluded tests | Excluded methods that are disabled, or that have a ticket comment from a `TicketSource`, are reported as `BLOCKED`; the ticket comment wins over the "disabled" comment                                 |
| `TicketSource`            | Moves to `dev.quokkify.tms.tickets.TicketSource`; the old `dev.quokkify.testrail.tickets.TicketSource` is deleted                                                                                       |
| `jira-testrail`           | Renamed now to `jira-tms` (`:integrations:jira:tms`, package `dev.quokkify.jira.tms`), depends on `tms` only                                                                                            |
| Legacy TestRail listeners | `TestStatusTestRailListener`, `AddDisabledTestsToTestRailListener`, `TestRailInterceptor`: `@Deprecated(forRemoval = true)`, no-op with one `WARN` when `TmsListener` is active                         |
| TestRail Allure helpers   | `AllureLinkToTestRailRunListener`, `TestRailCaseStructureToAllureListener` stay; gates removed                                                                                                          |
| ReportPortal              | `TmsDescriptionProvider` SPI unchanged; `reportportal-testrail` depends on `testrail-testng` so it carries the capability                                                                               |
| Repeatable `@TmsLink`     | New `TestUtils.getTmsLinks(...)`; `CustomCasesInterceptor` switches to it in a separate `fix:` commit. Other single-value readers (Jira listeners, ReportPortal, legacy TestRail) are a known follow-up |
| Versioning                | Breaking (`jira-testrail` rename, `TicketSource` move): squash title `feat!: ...` with a migration section                                                                                              |

## Migration for TestRail consumers

Replace the three legacy listener lines with `dev.quokkify.tms.listeners.TmsListener`, replace `jira-testrail` with `jira-tms`, and switch custom `TicketSource` implementations to the new package. Annotations stay as they are. Under `TmsListener`, tests without `@TmsLink` run but are not reported (previously `TestRailInterceptor` skipped them).

Moving a project from one TMS to another is one change: swap the provider dependency, rewrite `@TmsLink` values with a mapping table, and change `allure.link.tms.pattern`.
