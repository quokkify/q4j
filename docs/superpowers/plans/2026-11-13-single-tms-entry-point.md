# Single TMS Entry Point Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the first TestPlanIt pass into one TMS entry point: one `TmsListener`, exactly one provider on the classpath, and case ids only from Allure `@TmsLink`.

**Architecture:** `integrations/tms` owns the SPI, provider resolution, the `TicketSource` SPI and the TestNG listener. `testplanit` and `testrail-testng` are providers that publish the Gradle capability `dev.quokkify:tms-provider`. The TMS is chosen by which provider is on the classpath; there is no selection key.

**Tech Stack:** Java 21, Gradle 9.9, TestNG 7.12.0, Allure 3.1.1, Feign 13, AssertJ, Mockito.

**Spec:** `docs/superpowers/specs/2026-11-13-single-tms-entry-point-design.md`

## Global Constraints

- Worktree `~/Code/quokkify/.worktrees/q4j/feat-testplanit-integration`, branch `feat/testplanit-integration`. Re-check `git status --short --branch` before the first edit.
- The owner commits: do not run `git commit` unless the owner asks. Each task ends with a checkpoint that names the intended Conventional Commit message.
- No new dependencies. The repository is public: no employer details, internal URLs, tokens or machine paths in code or docs.
- The API token comes only from `TESTPLANIT_API_TOKEN` and never appears in code, config, logs or exception messages.
- Reporting errors never fail tests. Ambiguous provider setup (two or more providers, or zero with `TMS_REQUIRED=true`) must fail the run before any test executes.
- Per-module checks: `./gradlew :<module>:check` covers checkstyle, spotbugs (main and test) and tests. Format every changed Markdown file with `npx -y prettier@3 --write`.

## Review Focus

1. A test with two `@TmsLink` annotations, or one on the class: both case ids must be reported. Covered in Task 1 and Task 3.
2. A project with two providers on the runtime classpath but no Gradle capability (Maven, fat jar): the run must fail with both class names in the message. Covered in Task 2 and Task 3.
3. `TmsListener` registered twice (`META-INF` plus `@Listeners`): each result is reported once. Covered in Task 3.
4. A legacy TestRail listener left in `META-INF` next to `TmsListener`: no double report, one `WARN`. Covered in Task 5.
5. An excluded test that is enabled but has an open Jira ticket: reported `BLOCKED` with the ticket comment. Covered in Task 3.

---

### Task 1: Repeatable `@TmsLink` reader

**Files:**

- Modify: `testng-extensions/src/main/java/dev/quokkify/util/TestUtils.java`
- Modify: `testng-extensions/src/main/java/dev/quokkify/listener/extension/CustomCasesInterceptor.java:25-50`
- Test: `testng-extensions/src/test/java/dev/quokkify/test/TmsLinksTest.java`

**Interfaces:**

- Produces: `public static List<String> TestUtils.getTmsLinks(Method method)` and `public static List<String> TestUtils.getTmsLinks(ITestNGMethod method)`. Both return non-blank values of `method.getAnnotationsByType(TmsLink.class)`; if empty, the values of `method.getDeclaringClass().getAnnotationsByType(TmsLink.class)`. Values are trimmed, deduplicated and kept in declaration order. Never null. The `ITestNGMethod` overload returns `List.of()` when `getConstructorOrMethod().getMethod()` is null.

- [ ] **Step 1: Write failing tests** in `TmsLinksTest` (`@TmsLink("TNG_TMS_LINKS_n")` style, as in the module):
  - `twoMethodLinks`: a method with `@TmsLink("6") @TmsLink("7")` gives `containsExactly("6", "7")`;
  - `classLinkWhenMethodHasNone`: a class annotated `@TmsLink("9")` and a method without links gives `containsExactly("9")`;
  - `methodWinsOverClass`: a class `@TmsLink("9")` and a method `@TmsLink("6")` give `containsExactly("6")`;
  - `blankAndDuplicateIgnored`: `@TmsLink(" ") @TmsLink("6") @TmsLink("6")` gives `containsExactly("6")`;
  - `interceptorKeepsSecondLink`: `CustomCasesInterceptor.isTestRequired(List.of("6", "7"), List.of("7"))` returns `true`.
- [ ] **Step 2: Run** `./gradlew :testng-extensions:test --tests 'dev.quokkify.test.TmsLinksTest'`. Expected: compilation FAIL (methods missing).
- [ ] **Step 3: Implement** both `getTmsLinks` overloads. In `CustomCasesInterceptor`, add package-private `static boolean isTestRequired(List<String> caseIds, List<String> requiredIds)` (true if any id is required) and use `TestUtils.getTmsLinks(methodInstance.getMethod())`; log every matched id. Leave `getTestAnnotation` unchanged.
- [ ] **Step 4: Run** `./gradlew :testng-extensions:check`. Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 5: Checkpoint** — `fix(testng): read repeatable @TmsLink annotations`.

### Task 2: TMS core — SPI, resolution, tickets

**Files:**

- Modify: `integrations/tms/src/main/java/dev/quokkify/tms/TestManagementSystem.java`
- Modify: `integrations/tms/src/main/java/dev/quokkify/tms/TestManagementSystems.java`
- Modify: `integrations/tms/src/main/java/dev/quokkify/tms/TmsConfiguration.java`
- Create: `integrations/tms/src/main/java/dev/quokkify/tms/TmsConfigurationException.java`
- Create: `integrations/tms/src/main/java/dev/quokkify/tms/tickets/TicketSource.java` (moved verbatim from `integrations/testrail/testng/src/main/java/dev/quokkify/testrail/tickets/TicketSource.java`, new package)
- Create: `integrations/tms/src/main/java/dev/quokkify/tms/tickets/TicketComments.java`
- Test: `integrations/tms/src/test/java/dev/quokkify/tms/TestManagementSystemsTest.java` (rewrite), `integrations/tms/src/test/java/dev/quokkify/tms/tickets/TicketCommentsTest.java`

**Interfaces:**

- Produces:
  - `TestManagementSystem`: remove `caseIds(Method)`; keep `id()`, `isConfigured()`, `startRun(Set<String>)`, `report(TmsResult)`, `finishRun()`; add `default boolean shouldRun(String caseId) { return true; }`.
  - `TmsConfiguration`: only `@Key("TMS_REQUIRED") @DefaultValue("false") Boolean required()`. The `TMS` key is gone.
  - `public final class TmsConfigurationException extends RuntimeException` with a `(String message)` constructor.
  - `TestManagementSystems.resolve(): Optional<TestManagementSystem>` uses `ServiceLoader` and `TmsConfiguration.required()`. Package-private `static Optional<TestManagementSystem> resolve(List<TestManagementSystem> providers, boolean required)`:
    - more than one provider: throw `TmsConfigurationException` whose message lists every provider class name and says "exactly one TMS provider";
    - zero providers, or one provider with `isConfigured() == false`: `WARN` and `Optional.empty()`; throw instead when `required`;
    - otherwise `Optional.of(provider)`.
    - `selected()`, `isSelected()`, `select()`, `DEFAULT_SYSTEM`, `NONE` are removed.
  - `TicketComments.byCaseId(List<TicketSource> sources): Map<String, String>`: the comment format of `AddDisabledTestsToTestRailListener.buildCommentsByTestCase` (`"%s is disabled via %s"`, segments `"**label:** links"` joined with `"; "`), moved here. `TicketComments.load(): List<TicketSource>` loads enabled sources via `ServiceLoader`.

- [ ] **Step 1: Write failing tests:**
  - `TestManagementSystemsTest`:
    - `singleConfiguredProviderIsUsed`;
    - `twoProvidersFail` asserts `TmsConfigurationException` with both stub class names;
    - `noProviderIsEmpty`;
    - `noProviderFailsWhenRequired`;
    - `notConfiguredProviderIsEmpty`;
    - `notConfiguredProviderFailsWhenRequired`.
  - `TicketCommentsTest`:
    - two sources with labels `Jira Issue(s)` and `Issue(s)` for case `6` give `"6 is disabled via **Jira Issue(s):** J-1; **Issue(s):** X-2"`;
    - a source with an empty ticket list contributes nothing.
- [ ] **Step 2: Run** `./gradlew :integrations:tms:test`. Expected: compilation FAIL.
- [ ] **Step 3: Implement** the interfaces above. Delete `TestManagementSystems.selectedId()`.
- [ ] **Step 4: Run** `./gradlew :integrations:tms:compileJava :integrations:tms:test --tests '*TestManagementSystemsTest' --tests '*TicketCommentsTest'`. Expected: PASS. `TmsListener` may still fail to compile; Task 3 fixes it. If so, run the tests after Task 3.
- [ ] **Step 5: Checkpoint** — part of `feat!(tms): single TMS provider entry point`.

### Task 3: `TmsListener` as the single entry point

**Files:**

- Modify: `integrations/tms/src/main/java/dev/quokkify/tms/listeners/TmsListener.java`
- Test: `integrations/tms/src/test/java/dev/quokkify/tms/listeners/TmsListenerTest.java` (update), `integrations/tms/src/test/java/dev/quokkify/tms/listeners/TmsListenerTestNgRunTest.java` (new)

**Interfaces:**

- Consumes: `TestUtils.getTmsLinks(ITestNGMethod)` (Task 1); `TestManagementSystems.resolve()`, `TicketComments`, `TmsConfigurationException` (Task 2).
- Produces:
  - `TmsListener implements IExecutionListener, ISuiteListener, ITestListener, IMethodInterceptor`.
  - `public static boolean TmsListener.isActive()` is true from the first `onExecutionStart` until the matching `onExecutionFinish` (static `AtomicInteger` of active instances).
  - Constructor `TmsListener(Supplier<Optional<TestManagementSystem>> resolver, Supplier<List<TicketSource>> tickets)` is package-private; the public no-arg one uses `TestManagementSystems::resolve` and `TicketComments::load`.
- Behaviour:
  - Resolution happens once, in `onExecutionStart`, and is NOT wrapped in try/catch: `TmsConfigurationException` must abort the run.
  - `intercept` keeps a method if it has no case ids or any `shouldRun(caseId)` is true.
  - `reportDisabled(Collection<ITestNGMethod> excluded)` reports `BLOCKED` for excluded methods that are disabled or have a ticket comment. The comment is the ticket comment if present, otherwise `"<id> is disabled via 'enabled' attribute."`.
  - Report errors stay logged and swallowed. Keep the retry, `successPercentage` and `runAvailable` behaviour from the current file.

- [ ] **Step 1: Update `TmsListenerTest`.**
  - Replace the `@CaseIds` fixture with real `@TmsLink`, including a class-level link and a method with two links.
  - Keep every existing test except 11 with the same assertions.
  - Drop test 11; selection errors now propagate.
  - Add:
    - `excludedEnabledTestWithTicketIsBlocked`: comment equals the `TicketComments` text;
    - `interceptorDropsTestsTheSystemRejects`: a stub with `shouldRun("3") == false` keeps `linkedToTwoCases` and drops `linkedToOneCase`;
    - `activeOnlyDuringExecution`: `isActive()` is false before `onExecutionStart`, true inside, false after `onExecutionFinish`.
- [ ] **Step 2: Write `TmsListenerTestNgRunTest`.** It runs TestNG programmatically (`new TestNG(false)`, `setTestClasses`, `addListener`, `run`) over a private static sample class with one `@TmsLink("1")` test:
  - `ambiguousProvidersAbortRunBeforeTests`: the resolver throws `TmsConfigurationException`; assert that `run()` propagates it (or `getStatus() != 0`), and that the sample test body counter stays `0`;
  - `listenerRegisteredTwiceReportsOnce`: add two `TmsListener` instances sharing one recording stub; assert one `PASSED` report for case `1`.
- [ ] **Step 3: Run** `./gradlew :integrations:tms:test`. Expected: FAIL on the new tests.
- [ ] **Step 4: Implement the listener.**
  - **Dedup:** if TestNG does not deduplicate listener instances of one class (Step 2 tells you), make every instance after the first one active in this JVM a no-op. Use the static counter from `isActive()`; the instance that incremented it from 0 is the reporter.
  - **Fail-fast:** if TestNG swallows exceptions from `onExecutionStart`, move the resolution to the first `intercept` call and throw there.
  - In either case, record in the class Javadoc which mechanism the test proved.
- [ ] **Step 5: Run** `./gradlew :integrations:tms:check`. Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 6: Checkpoint** — `feat!(tms): single TMS provider entry point`.

### Task 4: TestPlanIt provider without a custom annotation

**Files:**

- Delete: `integrations/testplanit/src/main/java/dev/quokkify/testplanit/annotations/TestPlanItCase.java`
- Modify: `integrations/testplanit/src/main/java/dev/quokkify/testplanit/TestPlanItManagementSystem.java` (remove `caseIds`)
- Modify: `integrations/testplanit/build.gradle` (capability)
- Test: `integrations/testplanit/src/test/java/dev/quokkify/testplanit/TestPlanItManagementSystemTest.java`

**Interfaces:**

- Consumes: the Task 2 SPI.
- Produces: capability declaration, to be repeated in Tasks 5 and 6. In the provider `build.gradle`, for both `apiElements` and `runtimeElements`, call `outgoing.capability("dev.quokkify:tms-provider:${project.version}")` and re-declare the module's own default capability `"${project.group}:${project.ext.artifactId}:${project.version}"`. Read `ext.artifactId` inside `afterEvaluate` or a provider, because `gradle/module-metadata.gradle` sets it.

- [ ] **Step 1: Update the test.** Drop the annotation test and keep `requiresUrlTokenAndProject`. Add `idIsTestplanit` and `nonNumericCaseIdIsNotReported`. The second one asserts that `TestPlanItService` skips a `"JWT_ID_1"` result without calling the client; reuse `FakeTestPlanItClient`.
- [ ] **Step 2: Implement** the deletions and the capability.
- [ ] **Step 3: Verify** with `./gradlew :integrations:testplanit:check`, then `./gradlew :integrations:testplanit:outgoingVariants | grep -A2 Capabilities`. Expected: `dev.quokkify:tms-provider` and `dev.quokkify:testplanit` are listed for `apiElements` and `runtimeElements`.
- [ ] **Step 4: Checkpoint** — part of `feat!(tms)`.

### Task 5: TestRail provider, legacy listeners, ReportPortal

**Files:**

- Modify: `integrations/testrail/testng/src/main/java/dev/quokkify/testrail/tms/TestRailManagementSystem.java`
- Modify: the five listeners in `integrations/testrail/testng/src/main/java/dev/quokkify/testrail/listeners/`
- Delete: `integrations/testrail/testng/src/main/java/dev/quokkify/testrail/tickets/TicketSource.java`
- Modify: `integrations/testrail/testng/build.gradle` (capability as in Task 4)
- Modify: `integrations/reportportal/testrail/build.gradle`, `integrations/reportportal/testrail/src/main/java/dev/quokkify/reportportal/testrail/TestRailDescriptionProvider.java`
- Test: `integrations/testrail/testng/src/test/java/dev/quokkify/testrail/tms/TestRailManagementSystemTest.java`, `integrations/testrail/testng/src/test/java/dev/quokkify/testrail/listeners/LegacyListenersTest.java`

**Interfaces:**

- Consumes: Task 2 SPI, `TicketSource` and `TicketComments`; `TmsListener.isActive()` (Task 3).
- Produces: `TestRailManagementSystem.shouldRun(String)` delegates to `TestRailTestFilterRules.needRunTest`, and `caseIds` is removed.

- [ ] **Step 1: Write failing tests:**
  - `TestRailManagementSystemTest`: keep `configuredLikeTestRailListeners` and drop the `@TmsLink` reader test.
  - `LegacyListenersTest.statusListenerIsNoOpWhileTmsListenerActive`:
    - in `@BeforeClass`, before `TestStatusTestRailListener` is loaded, set system properties `TESTRAIL_ID=1` and `TESTRAIL_URL=http://127.0.0.1:9` (unreachable), so its `IS_TESTRAIL_ENABLED` is true;
    - activate `TmsListener` (`onExecutionStart` on an instance built with `Optional::empty`);
    - assert that `onTestSuccess` on a `@TmsLink("1")` result completes without an exception. Without the no-op it reaches `TestRailHelper` and fails on the unreachable host, so this test must fail before Step 2;
    - clear the properties in `@AfterClass`.
- [ ] **Step 2: Implement:**
  - remove every `TestManagementSystems.isSelected(...)` gate and the `TESTRAIL_ID` constant in `TestRailDescriptionProvider`;
  - in `TestStatusTestRailListener`, `AddDisabledTestsToTestRailListener` and `TestRailInterceptor`, add `@Deprecated(forRemoval = true)` with Javadoc "use `dev.quokkify.tms.listeners.TmsListener`". At the top of each callback, return early when `TmsListener.isActive()`, logging one `WARN` per listener class (static `AtomicBoolean`);
  - make `AddDisabledTestsToTestRailListener` use `TicketComments`;
  - in `integrations/reportportal/testrail/build.gradle`, replace `implementation project(":integrations:testrail:testrail-core")` with `api project(":integrations:testrail:testng")` and drop `:integrations:tms`.
- [ ] **Step 3: Run** `./gradlew :integrations:testrail:testng:check :integrations:reportportal:testrail:check`. Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 4: Checkpoint** — part of `feat!(tms)`.

### Task 6: Rename `jira-testrail` to `jira-tms`

**Files:**

- Move: `integrations/jira/testrail/` → `integrations/jira/tms/` (`git mv`); package `dev.quokkify.jira.testrail` → `dev.quokkify.jira.tms`.
- Move: `META-INF/services/dev.quokkify.testrail.tickets.TicketSource` → `META-INF/services/dev.quokkify.tms.tickets.TicketSource` (content `dev.quokkify.jira.tms.JiraTicketSource`).
- Modify: `integrations/jira/tms/build.gradle`, `settings.gradle:21-22`, `gradle/module-metadata.gradle:13`, `.github/workflows/test.yml:370-381,600`, `.github/workflows/reusable-module-matrices.yml:70`, `README.md:136`, `integrations/jira/tms/README.md`.
- Test: `integrations/jira/tms/src/test/java/dev/quokkify/jira/tms/JiraTicketSourceDiscoveryTest.java`

**Interfaces:**

- Module `:integrations:jira:tms`, artifactId `jira-tms`, display name `Q4J Jira TMS`, description `Jira ticket source for Q4J test management system integrations.`.
- Dependencies: `implementation project(":integrations:jira:jira-core")` and `api project(":integrations:tms")`. No dependency on any TestRail module.
- CI slug: `integrations-jira-tms`.

- [ ] **Step 1: Write the failing test.** `JiraTicketSourceDiscoveryTest` asserts that `ServiceLoader.load(TicketSource.class)` contains an instance of `JiraTicketSource`. It does not call Jira.
- [ ] **Step 2: Do the move and the edits.**
- [ ] **Step 3: Run** `./gradlew :integrations:jira:tms:check` and `python3 tools/release_contract.py`. Expected: `BUILD SUCCESSFUL`, `release contract OK`.
- [ ] **Step 4: Checkpoint** — part of `feat!(tms)`.

### Task 8: `testplanit` docker-compose profile with CI bootstrap

Owner request, added mid-execution. Scope: start TestPlanIt in CI and bootstrap credentials for it. No listener integration test (explicitly out of scope).

**Files:**

- Modify: `tools/environment/docker/docker-compose.yml` (services under `profiles: [testplanit]`)
- Modify: `tools/environment/docker/docker-compose.ci.yml`, `tools/environment/docker/docker-compose.local.yml` only if the existing profiles need per-environment overrides there for the same kind of service (follow the `reporting` pattern)
- Create: `tools/environment/scripts/infra/hooks/testplanit.sh`, `tools/environment/scripts/infra/modules/testplanit/bootstrap_testplanit.sh`
- Modify: `tools/environment/scripts/infra/after_health.sh` (case `testplanit`), `tools/scripts/ci/with-test-env.sh` (source `tools/environment/.testplanit.env`)
- Modify: `.github/workflows/test.yml` (job `integrations-testplanit`: `run-tests: true`, `profile: testplanit`)

**Interfaces:**

- Services: `testplanit-postgres` (postgres:18-alpine, no published port), `testplanit-valkey` (valkey/valkey:8-alpine, no published port), `testplanit` (image `ghcr.io/testplanit/testplanit-selfhost`, pinned to an existing release tag that matches app v1.1.22, plus a digest the same way other images in the file are pinned; publish container port 3000 the way other services publish theirs). Workers, MinIO and Elasticsearch are not started. Required app env (`DATABASE_URL`, `NEXTAUTH_URL`, `NEXTAUTH_SECRET`, `ENCRYPTION_KEY`, `VALKEY_URL`, `ADMIN_EMAIL`, `ADMIN_NAME`, `ADMIN_PASSWORD`) uses CI-only throwaway defaults overridable by env, like `REPORTPORTAL_ADMIN_PASSWORD:-erebus`.
- Bootstrap (`bootstrap_testplanit.sh`, bash + curl, same helpers as `modules/reportportal/bootstrap_reportportal.sh`: `resolve_published_port`, `select_runtime_host_for_port`, `wait_until`, `info`/`error`):
  1. wait until `GET /api/health` (or the endpoint the image actually exposes) is healthy;
  2. log in as the admin via NextAuth credentials (`/api/auth/csrf` then `POST /api/auth/callback/credentials`, cookie jar in a temp dir);
  3. create an API token via `POST /api/api-tokens` (`{"name":"q4j-ci"}`);
  4. ensure a project `q4j` exists (create via the API if missing) and resolve its id;
  5. write `tools/environment/.testplanit.env` with `TESTPLANIT_API_URL`, `TESTPLANIT_API_TOKEN`, `TESTPLANIT_PROJECT_ID`. The token is never echoed; the file is created with mode 600 and is git-ignored (check `.gitignore` covers `tools/environment/.*.env`; add it if not).

- [ ] **Step 1:** Implement the profile, hook and bootstrap.
- [ ] **Step 2: Verify locally** with the repo's own runner, `tools/environment/scripts/infra/run_app.sh` with profile `testplanit` (read how `run_app.sh` takes profiles). A TestPlanIt stack may already run on this machine on ports 30000, 5432, 6379 and 9000: the profile must not collide (no published DB/cache ports; app published on a free port). Expected: bootstrap exits 0, `.testplanit.env` exists with three keys, and `curl -H "Authorization: Bearer $TESTPLANIT_API_TOKEN" "$TESTPLANIT_API_URL/api/model/projects/findMany"` returns the `q4j` project. Then stop with `tools/environment/scripts/infra/stop_app.sh`.
- [ ] **Step 3:** `bash -n` and `shellcheck` (if installed) on the new scripts; `npx -y prettier@3 --check` on changed YAML.
- [ ] **Step 4: Checkpoint** — part of `feat!: single TMS entry point with TestPlanIt provider` (or a separate `ci(testplanit): ...` commit if the owner prefers).

### Task 7: Pilot, docs and end-to-end verification

**Files:**

- Modify: `common-utils/jwt/src/test/java/dev/quokkify/test/JwtTest.java` (`@TmsLink("JWT_ID_1|2|3")` → `@TmsLink("6"|"7"|"8")`; remove the `TestPlanItCase` import and annotations)
- Modify: `integrations/tms/README.md`, `integrations/testplanit/README.md`, `integrations/testrail/testng/README.md`, `README.md` (catalog row `jira-tms`, one-provider rule)

- [ ] **Step 1: Rewrite the READMEs.**
  - Remove the `TMS` key everywhere.
  - Document the one-provider rule, `TMS_REQUIRED`, `@TmsLink` usage (repeatable, class-level) and `allure.link.tms.pattern`.
  - Add the TestRail migration from the spec.
  - Run `npx -y prettier@3 --write` on every changed `.md`.
- [ ] **Step 2: Check that no old names are left.** Run `git grep -nE "TestPlanItCase|isSelected|TmsConfiguration\(\)\.tms|jira-testrail|dev\.quokkify\.testrail\.tickets"`. Expected: matches only in `CHANGELOG.md` and in `docs/superpowers/`.
- [ ] **Step 3: Verify the capability conflict.** Temporarily add `testImplementation project(":integrations:testrail:testng")` to `common-utils/jwt/build.gradle` and run `./gradlew :common-utils:jwt:dependencies --configuration testRuntimeClasspath`. Expected: a failure that mentions the capability `dev.quokkify:tms-provider`. Then revert the line.
- [ ] **Step 4: Run the affected checks:** `./gradlew :testng-extensions:check :integrations:tms:check :integrations:testplanit:check :integrations:testrail:testrail-core:check :integrations:testrail:testng:check :integrations:jira:tms:check :integrations:reportportal:testrail:check :common-utils:jwt:check`. Then run `./gradlew build -x test`, `python3 tools/release_contract.py` and `npx -y prettier@3 --check .`. Expected: all green. Do not run root `check`: `data-utils:sql` needs PostgreSQL.
- [ ] **Step 5: Run the live pilot.**
  - Load the token: `source <scratchpad>/tpi.env`.
  - Run `TESTPLANIT_PROJECT_ID=1 TESTPLANIT_RUN_NAME="q4j pilot: JWT #4" ./gradlew :common-utils:jwt:test --rerun-tasks`. Expected: a new run with cases 6/7/8 passed, attempt 1.
  - Repeat with `TESTPLANIT_RUN_ID=<new id>`. Expected: the same 3 run cases with attempt 2, and the project case count unchanged.
- [ ] **Step 6: Checkpoint** — squash title `feat!: single TMS entry point with TestPlanIt provider`. Fill the PR release-notes sections (Highlight, Usage example, Migration).
