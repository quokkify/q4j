# `@Sequential` Annotation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `@Sequential` as the new name for `@SingleThread` in `testng-extensions`, keep `@SingleThread` working as deprecated-for-removal, and move the repository's own tests and README to the new name.

**Architecture:** `Sequential` is a marker annotation next to `SingleThread`. `SuiteListener` gets `isMethodSequential(Method)`, which is true for `@Sequential` or when the existing `isMethodSingleThread(Method)` is true, and `getXmlIncludeTests` switches to it. The old method keeps its body so subclass overrides still take effect.

**Tech Stack:** Java 21, Gradle, TestNG, AssertJ, Checkstyle, SpotBugs, ArchGate, Prettier for Markdown.

**Spec:** `docs/superpowers/specs/2026-11-15-sequential-annotation-design.md`

**Branch:** `feat/sequential-annotation` (from `main`; already holds the spec and this plan, uncommitted).

## Global Constraints

- Public API changes are only: new `dev.quokkify.annotation.Sequential`, new `SuiteListener.isMethodSequential(Method)`, deprecation of `SingleThread` and `SuiteListener.isMethodSingleThread(Method)`.
- Do not rename `SINGLE_THREAD_TESTS_IN_PARALLEL`, `TestNGExtension.singleThreadTestsInParallel()`, `SuiteListener.ThreadGroup` or its constants, `SINGLE_THREAD_COUNT`, or the generated test names `Concurrency` and `Sequential`.
- Do not edit `CHANGELOG.md` or existing files under `docs/superpowers/`.
- Deprecations: `@Deprecated(since = "<version>", forRemoval = true)` where `<version>` is the next minor after the version in the release manifest that `build.gradle` reads (`0.16.0` while the manifest says `0.15.x`); Javadoc `@deprecated` names the replacement with `{@link}`.
- Javadoc is built with `Xdoclint:all,-missing` and `failOnError = true`: every `{@link}` must resolve.
- Style: 2-space indent; imports grouped `java.*` / `dev.quokkify.*` / third-party; no star imports.
- Markdown: run `npx prettier --write` on every changed `.md` before commit (CI runs `prettier --check .`).
- Commits: Conventional Commits; each ends with `Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>`. No machine paths in committed files.
- The PR body fills the Highlight / Usage example / Migration sections used by the release-notes tooling.

## Review Focus

- A method carrying both `@Sequential` and `@SingleThread` must appear once in `Sequential` and not in `Concurrency`. Pinned in Task 1 (`both` fixture method).
- A `SuiteListener` subclass that overrides `isMethodSingleThread` must still move methods into `Sequential`. Pinned in Task 1 (`testLegacyOverrideIsHonoured`).
- A consumer that still uses `@SingleThread` must get the same placement as before. Pinned in Task 1 (`legacySingleThread` fixture method).
- The Javadoc task must pass with the new `@deprecated` tags and links. Pinned in Task 1 Step 6 (`:testng-extensions:javadoc`).
- No test in the repository may silently lose its sequential placement during the swap. Pinned in Task 2 Step 2 (annotation count before and after is equal).

---

### Task 1: `@Sequential`, deprecated `@SingleThread`, listener support

**Files:**

- Create: `testng-extensions/src/main/java/dev/quokkify/annotation/Sequential.java`
- Modify: `testng-extensions/src/main/java/dev/quokkify/annotation/SingleThread.java`
- Modify: `testng-extensions/src/main/java/dev/quokkify/listener/lifecycle/SuiteListener.java` (`getXmlIncludeTests`, `isMethodSingleThread`, class and `generateSuite` Javadoc)
- Test: `testng-extensions/src/test/java/dev/quokkify/test/SequentialAnnotationTest.java`

**Interfaces:**

- Produces: `@Retention(RUNTIME) @Target(METHOD) public @interface Sequential` in `dev.quokkify.annotation`; `protected boolean SuiteListener.isMethodSequential(Method method)`.

- [ ] **Step 1: Commit the spec and this plan**

```bash
npx prettier --write docs/superpowers/specs/2026-11-15-sequential-annotation-design.md docs/superpowers/plans/2026-11-15-sequential-annotation.md
git add docs/superpowers/specs/2026-11-15-sequential-annotation-design.md docs/superpowers/plans/2026-11-15-sequential-annotation.md
git commit -m "docs: add @Sequential annotation design and plan"
```

- [ ] **Step 2: Write the failing test `SequentialAnnotationTest`**

Follow the style of `SuiteListenerGroupsTest` in the same package (build an `XmlSuite` with one `XmlTest` holding the fixture class, call `alter`, read `XmlInclude` names per generated `XmlTest`). Fixture methods are `@Test(enabled = false)` so the outer run skips them.

```java
public static class Fixture {
  @Test(enabled = false) public void concurrent() { }
  @Sequential @Test(enabled = false) public void sequential() { }
  @SingleThread @Test(enabled = false) public void legacySingleThread() { }
  @Sequential @SingleThread @Test(enabled = false) public void both() { }
}

public static class LegacyOverrideListener extends SuiteListener {
  @Override
  protected boolean isMethodSingleThread(Method method) {
    return "concurrent".equals(method.getName());
  }
}
```

Tests (helper `includedMethods(List<XmlTest> generated, String testName)` returns the include names of the generated test with that name):

```java
@Test(description = "Regression guard: @Sequential and deprecated @SingleThread both go to the Sequential test")
public void testBothAnnotationsLandInSequential() {
  List<XmlTest> generated = alter(new SuiteListener());
  assertThat(generated).extracting(XmlTest::getName).containsExactly("Concurrency", "Sequential");
  assertThat(includedMethods(generated, "Concurrency")).containsExactly("concurrent");
  assertThat(includedMethods(generated, "Sequential"))
      .containsExactlyInAnyOrder("sequential", "legacySingleThread", "both");
}

@Test(description = "Regression guard: an overridden isMethodSingleThread still moves methods to Sequential")
public void testLegacyOverrideIsHonoured() {
  List<XmlTest> generated = alter(new LegacyOverrideListener());
  assertThat(includedMethods(generated, "Concurrency")).isEmpty();
  assertThat(includedMethods(generated, "Sequential"))
      .containsExactlyInAnyOrder("concurrent", "sequential", "legacySingleThread", "both");
}
```

Use `Assertions.assertThat` unqualified-by-class as the neighbouring tests do (`Assertions.assertThat(...)`), and add `@SuppressWarnings("removal")` on the test class for its use of `SingleThread`.

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :testng-extensions:test --tests 'dev.quokkify.test.SequentialAnnotationTest'`
Expected: compilation FAILS with `cannot find symbol ... class Sequential`.

- [ ] **Step 4: Create `Sequential` and deprecate `SingleThread`**

`Sequential.java`: same meta-annotations as `SingleThread`. Javadoc: the test is placed in the `Sequential` block that `SuiteListener` generates and runs one at a time, not in parallel with other `@Sequential` tests; link `SuiteListener`.

`SingleThread.java`: add `@Deprecated(since = "<version>", forRemoval = true)` and Javadoc `@deprecated use {@link Sequential} instead`.

- [ ] **Step 5: Update `SuiteListener`**

- Add `protected boolean isMethodSequential(Method method)`: true when the method has `@Sequential`, or when `isMethodSingleThread(method)` returns true. Javadoc in the style of the neighbouring methods.
- `isMethodSingleThread(Method)`: body unchanged; add `@Deprecated(since = "<version>", forRemoval = true)` and Javadoc `@deprecated override {@link #isMethodSequential(Method)} instead`.
- `getXmlIncludeTests`: replace both `isMethodSingleThread` references with `isMethodSequential`.
- Silence the listener's own use of the deprecated annotation and method with `@SuppressWarnings("removal")` on the narrowest element that compiles without the warning.
- Class and `generateSuite` Javadoc: where the text says tests are selected by the single-thread annotation, name `@Sequential`. Leave the option name `SINGLE_THREAD_TESTS_IN_PARALLEL` as is.

- [ ] **Step 6: Run the tests and module checks**

Run: `./gradlew :testng-extensions:test --tests 'dev.quokkify.test.SequentialAnnotationTest'`
Expected: 2 tests PASS.

Run: `./gradlew :testng-extensions:check :testng-extensions:javadoc`
Expected: BUILD SUCCESSFUL (existing `SuiteListenerGroupsTest` and `SingleThreadInParallelTest` still pass; no Javadoc errors).

- [ ] **Step 7: Commit**

```bash
git add testng-extensions/src
git commit -m "feat(testng-extensions): add @Sequential and deprecate @SingleThread"
```

### Task 2: Move the repository's tests and README to `@Sequential`

**Files:**

- Modify (swap the import `dev.quokkify.annotation.SingleThread` → `dev.quokkify.annotation.Sequential` and every `@SingleThread` → `@Sequential`):
  - `data-utils/sql/src/test/java/dev/quokkify/test/{ConnectionProviderTest,ConnectionPoolTest,SqlConverterTest}.java`
  - `integrations/selenide/src/test/java/dev/quokkify/test/{TableModelContractTest,UiProxyTest,UiFileDownloadTest}.java`
  - `integrations/rest-assured/src/test/java/dev/quokkify/test/{CustomExternalServiceLogFilterTest,ConnectionTimeoutTest}.java`
  - `integrations/tms/testng/src/test/java/dev/quokkify/tms/listeners/{TmsListenerTestNgRunTest,TmsListenerTest}.java`
  - `integrations/testrail/testng/src/test/java/dev/quokkify/testrail/listeners/LegacyListenersTest.java`
  - `integrations/tyrus/src/test/java/dev/quokkify/tyrus/client/{WsClientTest,WsClientIntegrationTest}.java`
  - `testng-extensions/src/test/java/dev/quokkify/test/{VerifySoftlyTest,SuiteListenerGroupsTest,SingleThreadInParallelTest}.java`
- Modify: `testng-extensions/README.md`

**Interfaces:**

- Consumes: `dev.quokkify.annotation.Sequential` from Task 1.

- [ ] **Step 1: Record the annotation count before the swap**

Run: `grep -rIE "^\s*@SingleThread\b" --include="*.java" . --exclude-dir=build | grep -v SequentialAnnotationTest | wc -l`
Note the number N.

- [ ] **Step 2: Swap the annotation in the listed test files**

Keep the import in its current group and alphabetical position (`Sequential` sorts where `SingleThread` did relative to the other `dev.quokkify.*` imports; re-sort if Checkstyle complains). Do not rename test classes, fixture methods (`apiSingleThread`, `plainSingleThread`) or assertion strings.

Run: `grep -rIE "^\s*@Sequential\b" --include="*.java" . --exclude-dir=build | grep -v SequentialAnnotationTest | wc -l`
Expected: N.

Run: `grep -rIlE "SingleThread\b" --include="*.java" . --exclude-dir=build | grep -v "/build/"`
Expected: only `SingleThread.java`, `SuiteListener.java`, `SequentialAnnotationTest.java`, plus files that mention it only in identifiers or text (`SuiteListenerGroupsTest`, `SingleThreadInParallelTest`, `CommandLockManagerTest`, `SshUtilsCommandLockTest`). No file imports `dev.quokkify.annotation.SingleThread` except `SuiteListener` and `SequentialAnnotationTest`.

- [ ] **Step 3: Update `testng-extensions/README.md`**

- Configuration table row for `SINGLE_THREAD_TESTS_IN_PARALLEL`: description says `@Sequential` tests.
- Section heading and body "Running `@SingleThread` tests alongside parallel tests": use `@Sequential` throughout; the option name stays.
- Usage example: `@Sequential` on `checkSequentialOperation`, and its import if the example shows imports.
- Key API table: replace the `@SingleThread` row with `` `@Sequential` `` — "Run the annotated test in the `Sequential` block, one test at a time" — and add a row `` `@SingleThread` `` — "Deprecated, use `@Sequential`".

Run: `npx prettier --write testng-extensions/README.md && npx prettier --check testng-extensions/README.md docs/superpowers`
Expected: all files formatted.

- [ ] **Step 4: Compile and check every touched module**

Run:

```bash
./gradlew :testng-extensions:check \
  :data-utils:sql:compileTestJava :data-utils:sql:checkstyleTest \
  :integrations:selenide:compileTestJava :integrations:selenide:checkstyleTest \
  :integrations:rest-assured:compileTestJava :integrations:rest-assured:checkstyleTest \
  :integrations:tms:tms-testng:compileTestJava :integrations:tms:tms-testng:checkstyleTest \
  :integrations:testrail:testng:compileTestJava :integrations:testrail:testng:checkstyleTest \
  :integrations:tyrus:compileTestJava :integrations:tyrus:checkstyleTest
```

Expected: BUILD SUCCESSFUL, and no `[removal]` warning for `SingleThread` outside `testng-extensions` main sources. Take project paths from `settings.gradle` if one differs. Suites that need Docker (SQL pool, Selenium grid) run in CI; additionally run locally the ones that do not: `./gradlew :integrations:tms:tms-testng:test :integrations:testrail:testng:test`, expected PASS.

- [ ] **Step 5: Commit**

```bash
git add data-utils integrations testng-extensions
git commit -m "refactor: use @Sequential in tests and testng-extensions docs"
```
