# TMS: One Result per Case for Repeated Invocations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A case linked to a data-provider or `invocationCount` test gets one TMS result per execution with the worst status of its invocations.

**Architecture:** `TmsListener` stops reporting repeated methods per invocation. A package-private `InvocationCollector` keeps their invocations per test method and case id; the listener drains it when the enclosing `<test>` finishes and again at execution finish, and reports one aggregated `TmsResult` per case. Providers are not touched.

**Tech Stack:** Java, TestNG 7.12.0, AssertJ, Mockito, Gradle.

**Spec:** `docs/superpowers/specs/2026-11-15-tms-data-provider-status-design.md`

## Global Constraints

- Change only `integrations/tms/testng` sources and tests, plus `integrations/tms/testng/README.md` and `integrations/testplanit/README.md`.
- A method is _repeated_ when `ITestNGMethod.isDataDriven()` is true or `getInvocationCount() > 1`. Any other method is reported immediately, exactly as today.
- Status precedence: `FAILED` > `SKIPPED` > `PASSED`.
- `elapsed` of the aggregated result is the sum of the invocations.
- Retries stay as they are: `onTestSkipped` with `wasRetried()` is ignored and never collected.
- Reporting errors are logged and never fail the tests (existing `report(...)` behaviour).
- The temporary repro files in the worktree (`integrations/testplanit/src/test/java/dev/quokkify/testplanit/repro/`, the `TEMP repro scaffolding` block at the end of `integrations/testplanit/build.gradle`) are never committed.
- Markdown must pass `npx prettier --check .`.

## Review Focus

- Parallel rows (`@DataProvider(parallel = true)`): every row is collected, none lost, one result. Test in Task 1.
- A repeated method with two `@TmsLink`: one aggregated result per case id. Test in Task 1.
- Mixed `FAILED` and `SKIPPED` invocations: the result is `FAILED`. Test in Task 1.
- `null` or very long parameter values: rendered as `null`, parameter list truncated to 200 characters. Test in Task 1.
- Run start failed (`runAvailable == false`): nothing is reported at flush and nothing throws. Test in Task 1.

---

### Task 1: Aggregate repeated invocations in `TmsListener`

**Files:**

- Create: `integrations/tms/testng/src/main/java/dev/quokkify/tms/listeners/InvocationCollector.java`
- Modify: `integrations/tms/testng/src/main/java/dev/quokkify/tms/listeners/TmsListener.java`
- Create: `integrations/tms/testng/src/test/java/dev/quokkify/tms/listeners/InvocationCollectorTest.java`
- Modify: `integrations/tms/testng/src/test/java/dev/quokkify/tms/listeners/TmsListenerTestNgRunTest.java`
- Modify: `integrations/tms/testng/src/test/java/dev/quokkify/tms/listeners/TmsListenerTest.java`
- Modify: `integrations/tms/testng/README.md`, `integrations/testplanit/README.md`

**Interfaces:**

- Produces (package-private, `dev.quokkify.tms.listeners`):

  ```java
  final class InvocationCollector {
    static final int MAX_PARAMETERS_LENGTH = 200;

    record Invocation(TmsStatus status, String comment, Duration elapsed, Object[] parameters) { }

    void add(ITestNGMethod method, String caseId, Invocation invocation);

    /** Removes and aggregates everything collected for the given methods. */
    List<TmsResult> drain(Collection<ITestNGMethod> methods);

    /** Removes and aggregates everything collected. */
    List<TmsResult> drainAll();
  }
  ```

- [ ] **Step 1: Write the failing regression test** in `TmsListenerTestNgRunTest`. Label it as a regression guard in the description. Change the `testNg()` helper to `testNg(Class<?>... testClasses)` and pass `SampleTest.class` from the two existing tests. Add a sample class and the test:

  ```java
  private static final class DataDrivenSample {

    @DataProvider
    public Object[][] rows() {
      return new Object[][] {{"row-1", false}, {"row-2", false}, {"row-3", true}};
    }

    @TmsLink("9")
    @Test(dataProvider = "rows")
    public void parameterized(String row, boolean passes) {
      assertThat(passes).as(row).isTrue();
    }
  }

  @TmsLink("TMS_LISTENER_RUN_3")
  @SingleThread
  @Test(description = "Regression guard: a passing last data provider row does not hide earlier failures")
  public void dataProviderRowsBecomeOneWorstStatusResult() {
    RecordingSystem system = new RecordingSystem();
    TestNG testNg = testNg(DataDrivenSample.class);
    testNg.addListener(new TmsListener(() -> Optional.of(system), List::of));

    testNg.run();

    assertThat(system.reported).singleElement().satisfies(result -> {
      assertThat(result.caseId()).isEqualTo("9");
      assertThat(result.status()).isEqualTo(TmsStatus.FAILED);
      assertThat(result.comment().lines()).first().isEqualTo("3 invocations: 2 failed, 1 passed");
      assertThat(result.comment()).contains("[1] FAILED (row-1, false): ", "[2] FAILED (row-2, false): ");
      assertThat(result.comment()).doesNotContain("row-3");
    });
  }
  ```

- [ ] **Step 2: Run it and confirm it fails**

  Run: `./gradlew :integrations:tms:tms-testng:test --tests '*TmsListenerTestNgRunTest'`
  Expected: `dataProviderRowsBecomeOneWorstStatusResult` FAILS with three reported results instead of one.

- [ ] **Step 3: Write `InvocationCollectorTest`** (mock `ITestNGMethod`, `@TmsLink("TMS_COLLECTOR_n")` on each test, as in the neighbouring tests). Cases and exact expectations:
  - `failed, skipped, passed` → status `FAILED`, first comment line `3 invocations: 1 failed, 1 skipped, 1 passed`, lines `[1] FAILED ...` and `[2] SKIPPED ...`.
  - `skipped, passed` → status `SKIPPED`.
  - all passed, three invocations → status `PASSED`, comment exactly `3 invocations: 3 passed`.
  - elapsed `1s + 2s`, one `null` elapsed → `Duration.ofSeconds(3)`; all `null` → `null`.
  - no parameters (empty array or `null`) → line `[2] FAILED: boom` without parentheses.
  - parameters `{null, "x".repeat(500)}` → the rendered list starts with `null, ` and is exactly `MAX_PARAMETERS_LENGTH` characters ending with `…`.
  - one method, case ids `1` and `2` → two results, one per case id.
  - `drain(List.of(methodA))` leaves `methodB` entries; a following `drainAll()` returns only `methodB`; a second `drainAll()` returns an empty list.
  - 50 `add` calls from a thread pool → the summary line says `50 invocations`.

- [ ] **Step 4: Implement `InvocationCollector`.** Entries are keyed by `(ITestNGMethod, caseId)` and keep invocations in arrival order; `[n]` is the 1-based arrival index. Summary line lists only non-zero counts in the order failed, skipped, passed. Detail lines only for non-passed invocations: `[n] STATUS (p1, p2): comment`, parameters rendered with `String.valueOf` and joined with `, `. All methods are thread-safe.

- [ ] **Step 5: Wire the collector into `TmsListener`.**
  - `reportResult`: when the method is repeated (see Global Constraints), `add` one `Invocation` per case id instead of calling `report`.
  - Add `@Override public void onFinish(ITestContext context)`: report every result of `collector.drain(List.of(context.getAllTestMethods()))` through the existing `report(tms, result)`.
  - `onExecutionFinish`: report `collector.drainAll()` before `tms.finishRun()`.
  - Update the class Javadoc: repeated methods produce one result per case when their `<test>` finishes.

- [ ] **Step 6: Add the remaining run tests** to `TmsListenerTestNgRunTest`, each through a nested TestNG run with `RecordingSystem`:
  - data provider with all rows passing → single `PASSED` result, comment `3 invocations: 3 passed`;
  - `@Test(invocationCount = 3)` failing on the second invocation → single `FAILED` result, comment contains `[2] FAILED: `;
  - `@DataProvider(parallel = true)` with 20 rows, one failing → single `FAILED` result, first line `20 invocations: 1 failed, 19 passed`;
  - data-driven method with `@TmsLink("9")` and `@TmsLink("10")` → exactly two results, case ids `9` and `10`;
  - plain `SampleTest` → still exactly one `PASSED` result with a `null` comment.

  And to `TmsListenerTest`: a repeated method (mock `isDataDriven()` → `true`) after a failed `startRun` → `onTestFailure`, `onFinish(context)` and `onExecutionFinish()` report nothing and do not throw.

- [ ] **Step 7: Run the module checks**

  Run: `./gradlew :integrations:tms:tms-testng:check :integrations:testplanit:compileTestJava`
  Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 8: Update the READMEs.**
  - `integrations/tms/testng/README.md`: add a short "Repeated tests" section: data provider rows and `invocationCount` invocations of one method produce one result per case, worst status wins (`FAILED` > `SKIPPED` > `PASSED`), reported when the `<test>` finishes, with the comment format from the spec.
  - `integrations/testplanit/README.md`, "Run behaviour": replace the sentence about data providers and `invocationCount` becoming separate attempts with the aggregated behaviour; keep the statement that shards sharing `TESTPLANIT_RUN_ID` add one attempt each and the last one received sets the status.

  Run: `npx prettier --check integrations/tms/testng/README.md integrations/testplanit/README.md docs/superpowers`
  Expected: all files use Prettier code style.

- [ ] **Step 9: Commit**

  ```bash
  git add integrations/tms/testng integrations/testplanit/README.md docs/superpowers
  git commit -m "fix(tms): report one worst-status result per case for repeated tests"
  ```

  `integrations/testplanit/build.gradle` and the `repro` test folder must stay out of the commit.

### Task 2: Verify on the live TestPlanIt and remove the repro scaffolding

**Files:**

- Delete: `integrations/testplanit/src/test/java/dev/quokkify/testplanit/repro/DataProviderReproTest.java`
- Modify: `integrations/testplanit/build.gradle` (drop the `TEMP repro scaffolding` block)

**Interfaces:**

- Consumes: the aggregated reporting from Task 1.

- [ ] **Step 1: Run the repro test against the local TestPlanIt** (`http://localhost:30000`, project `1`, case `9`; the API token is passed through the environment and never written to a tracked file):

  ```bash
  TESTPLANIT_API_URL=http://localhost:30000 TESTPLANIT_API_TOKEN=<token> TESTPLANIT_PROJECT_ID=1 \
    TESTPLANIT_RUN_NAME="q4j repro: data provider aggregated" \
    ./gradlew :integrations:testplanit:test --tests '*DataProviderReproTest'
  ```

- [ ] **Step 2: Read the run case back** with `GET /api/model/testRunCases/findMany` filtered by the run name, selecting `status.systemName` and `results`.

  Expected: run case status `failed`; exactly one result, attempt `1`, status `failed`, notes starting with `3 invocations: 2 failed, 1 passed` and naming `row-1` and `row-2`.

- [ ] **Step 3: Remove the scaffolding.** Delete the `repro` folder and the `TEMP repro scaffolding` block.

  Run: `git status --short`
  Expected: clean working tree.
