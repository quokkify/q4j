# Selenide Flutter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or
> superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A new q4j module `integrations/selenide-flutter` that tests Flutter apps through Appium and Selenide and
makes every element search visible (per-find attempts, widget tree and near-miss suggestions on failure), plus a
Flutter test app, a one-command local run, an Android emulator CI job, and a Flutter-vs-native comparison.

**Architecture:** Tests use Selenide `$()` with `FlutterBy` locators (plain java-client `AppiumBy` Flutter
locators). `FlutterVisibility.install()` registers a Selenium `WebDriverListener` (`FindTracer`, via
`WebDriverRunner.addListener`) that records every find attempt, and a Selenide `LogEventListener` that groups the
attempts per Selenide command, attaches a summary, and on failure fetches `flutter: renderTree` and attaches the
tree and ranked near-miss suggestions. Pure logic (locator parsing, tree parsing/formatting, suggestion ranking,
attempt summaries, comparison report) is unit-tested; device scenarios live in a separate `deviceTest` source set.

**Tech Stack:** Java 17+, Gradle (Groovy DSL), Selenide 7.19.1 + `selenide-appium` 7.19.1 (java-client 11.0.0),
TestNG, Allure 3.1.1, Flutter + `appium_flutter_server` 0.0.33, Appium 3.x + `appium-flutter-integration-driver`
2.0.3 + UiAutomator2 driver, GitHub Actions + `reactivecircus/android-emulator-runner`.

**Spec:** `docs/superpowers/specs/2026-11-20-selenide-flutter-design.md`

## Global Constraints

- Module path `integrations/selenide-flutter`, Gradle path `:integrations:selenide-flutter`, base package
  `dev.quokkify.flutter`.
- `selenide-appium` comes from the catalog with `version.ref = "selenide"` (currently 7.19.1); no separate
  java-client version.
- Appium pinned to a 3.x version (not 4), `appium-flutter-integration-driver@2.0.3`, `appium_flutter_server: 0.0.33`.
  All tool versions live in one file: `tools/environment/scripts/flutter/versions.env`.
- The Flutter SDK version is pinned exactly in `test-app/pubspec.yaml` `environment.flutter`; CI reads it from there
  (`subosito/flutter-action` `flutter-version-file`), the local script checks against it.
- The test app is built debug with target `integration_test/appium_test.dart`.
- Selenide owns waiting; the driver's `flutterElementWaitTimeout` comes from `FLUTTER_DRIVER_FIND_TIMEOUT_MS`.
- `renderTree` is fetched only when a Selenide command on a Flutter locator fails, once per failed command.
- Diagnostics never replace or hide the original error.
- The module does not depend on `allure-selenide` outside tests; it only adds attachments
  (`allure-java-commons`).
- `publish: false` in `gradle/module-metadata.gradle`.
- GitHub Actions pinned by commit SHA with a version comment (reuse SHAs already in the repo where the same
  action exists).
- Public repo: no employer data, internal URLs, tokens or machine paths in any file.
- Markdown formatted with Prettier (CI runs `prettier --check .`); Java passes the module's `check` (Checkstyle,
  SpotBugs, archgate).

## Review Focus

1. `renderTree` slow, huge, or throwing → original Selenide error still reported; diagnostics error attached as text;
   tree truncated with a marker above the cap. Test: Task 5 `diagnosticsTimeoutKeepsOriginalError`,
   `hugeTreeIsTruncated`.
2. Key formats from the server (`[<'login'>]`, `[<login>]`, `null`, non-string keys) → normalized or left raw,
   never a crash. Test: Task 2 `normalizesFlutterKeyToString`.
3. A Selenide command fails when no driver is running, or in native mode → no `renderTree` call, attempt summary
   still attached. Test: Task 5 `nativeLocatorFailureSkipsRenderTree`, `noDriverSkipsDiagnostics`.
4. Two test threads → attempts from one thread never appear in the other's summary. Test: Task 4
   `attemptsAreThreadLocal`.
5. `install()` called twice → one listener, no duplicate attachments; `uninstall()` removes both listeners. Test:
   Task 5 `installIsIdempotent`.

---

## File Structure

```
integrations/selenide-flutter/
  build.gradle
  README.md
  src/main/java/dev/quokkify/flutter/
    FlutterBy.java                      locator factory (returns AppiumBy)
    locator/LocatorSpec.java            parsed locator (strategy, value, of, matching)
    locator/Strategy.java               enum of strategies with wire names
    locator/Locators.java               parse(By) and describe(By)
    tree/WidgetNode.java, tree/Rect.java
    tree/WidgetTreeParser.java          JSON / script result -> WidgetNode
    tree/WidgetTreeFormatter.java       indented text form
    suggest/NearMiss.java, suggest/NearMissFinder.java
    trace/FindAttempt.java, trace/Outcome.java
    trace/FindTracer.java               WebDriverListener, thread-local buffer
    trace/AttemptSummary.java
    diagnostics/Diagnostics.java, diagnostics/FailureDiagnostics.java
    report/DiagnosticsSink.java, report/AllureSink.java, report/LogSink.java
    FlutterVisibility.java              install()/uninstall(), LogEventListener
    config/FlutterConfiguration.java
    driver/FlutterDriverProvider.java
  src/test/java/dev/quokkify/flutter/... unit tests
  src/test/resources/trees/*.json       widget-tree fixtures
  src/deviceTest/java/dev/quokkify/flutter/device/...  device scenarios, modes, comparison
  src/deviceTest/resources/log4j2.xml, testng-*.xml
  test-app/                             Flutter project
tools/environment/scripts/flutter/
  versions.env, build_app.sh, run_device_tests.sh, stop_device_env.sh
.github/workflows/flutter-device-tests.yml
```

---

### Task 1: Module skeleton and `FlutterBy` with locator parsing

**Files:**

- Create: `integrations/selenide-flutter/build.gradle`
- Modify: `settings.gradle` (after the `selenide-grid` include line), `gradle/libs.versions.toml` (Selenide block),
  `gradle/module-metadata.gradle` (after `selenide-grid` entry), `.github/workflows/reusable-module-matrices.yml`
  (`integrationsBuildModules`), `.github/workflows/test.yml` (new `integrations-selenide-flutter` job copied from
  `integrations-selenide-proxy` with `run-tests: true`, no `profile`; add it to the final `needs` list)
- Create: `src/main/java/dev/quokkify/flutter/FlutterBy.java`, `locator/Strategy.java`, `locator/LocatorSpec.java`,
  `locator/Locators.java`
- Test: `src/test/java/dev/quokkify/flutter/locator/LocatorsTest.java`

**Interfaces:**

- Produces:
  - `FlutterBy.key(String)`, `text(String)`, `textContaining(String)`, `type(String)`, `semanticsLabel(String)`,
    `descendant(By of, By matching)`, `ancestor(By of, By matching)` — all `static By`, returning the
    `AppiumBy.flutter*` instance unchanged (no wrapper; the readable form lives in `Locators.describe`).
  - `enum Strategy { KEY("-flutter key"), TEXT("-flutter text"), TEXT_CONTAINING("-flutter text containing"),
TYPE("-flutter type"), SEMANTICS_LABEL("-flutter semantics label"), DESCENDANT("-flutter descendant"),
ANCESTOR("-flutter ancestor"), ID("id"), ACCESSIBILITY_ID("accessibility id"), OTHER(null); boolean isFlutter(); }`
  - `record LocatorSpec(Strategy strategy, String value, LocatorSpec of, LocatorSpec matching)` (`of`/`matching`
    non-null only for DESCENDANT/ANCESTOR).
  - `Locators.parse(By): LocatorSpec` (never throws; unknown → `OTHER` with `by.toString()` as value);
    `Locators.describe(By): String`.

- [ ] **Step 1: Wire the module.** `build.gradle`: description `"Selenide Appium support for Flutter apps with
    visible element search"`; deps `api project(":integrations:selenide")`, `api libs.selenide.appium`,
      `implementation project(":common-utils:config")`, `implementation project(":common-utils:core")`,
      `implementation project(":common-utils:jackson:json")`, `implementation libs.allure.java.commons`; test deps
      `libs.allure.testng`, `libs.allure.selenide`, `libs.mockito.core`, `org.assertj:assertj-core`. Catalog:
      `selenide-appium = { module = "com.codeborne:selenide-appium", version.ref = "selenide" }`. Metadata:
      `':integrations:selenide-flutter': [artifactId: 'selenide-flutter', displayName: 'Q4J Selenide Flutter',
description: 'Selenide and Appium support for Flutter apps with visible element search.', publish: false]`.
- [ ] **Step 2: Confirm wiring.** Run `./gradlew :integrations:selenide-flutter:dependencies --configuration
compileClasspath | grep java-client` → shows `io.appium:java-client:11.0.0`.
- [ ] **Step 3: Write failing tests** in `LocatorsTest`:

```java
@Test void describesFlutterKey() {
  assertThat(Locators.describe(FlutterBy.key("login_button"))).isEqualTo("flutter key \"login_button\"");
}
@Test void parsesEachFlutterStrategy() {
  assertThat(Locators.parse(FlutterBy.text("Sign in"))).isEqualTo(new LocatorSpec(Strategy.TEXT, "Sign in", null, null));
  assertThat(Locators.parse(FlutterBy.semanticsLabel("Settings icon")).strategy()).isEqualTo(Strategy.SEMANTICS_LABEL);
  assertThat(Locators.parse(FlutterBy.type("ElevatedButton")).value()).isEqualTo("ElevatedButton");
  assertThat(Locators.parse(FlutterBy.textContaining("Item")).strategy()).isEqualTo(Strategy.TEXT_CONTAINING);
}
@Test void parsesDescendantBothSides() {
  LocatorSpec spec = Locators.parse(FlutterBy.descendant(FlutterBy.key("card_b"), FlutterBy.text("Details")));
  assertThat(spec.strategy()).isEqualTo(Strategy.DESCENDANT);
  assertThat(spec.of()).isEqualTo(new LocatorSpec(Strategy.KEY, "card_b", null, null));
  assertThat(spec.matching()).isEqualTo(new LocatorSpec(Strategy.TEXT, "Details", null, null));
  assertThat(Locators.describe(FlutterBy.descendant(FlutterBy.key("card_b"), FlutterBy.text("Details"))))
      .isEqualTo("flutter descendant(of: flutter key \"card_b\", matching: flutter text \"Details\")");
}
@Test void nativeAndUnknownLocators() {
  assertThat(Locators.parse(AppiumBy.id("login_email")).strategy()).isEqualTo(Strategy.ID);
  assertThat(Locators.parse(AppiumBy.accessibilityId("Settings icon")).strategy()).isEqualTo(Strategy.ACCESSIBILITY_ID);
  assertThat(Locators.parse(By.xpath("//a")).strategy()).isEqualTo(Strategy.OTHER);
  assertThat(Locators.describe(By.xpath("//a"))).isEqualTo("By.xpath: //a");
}
```

- [ ] **Step 4: Run** `./gradlew :integrations:selenide-flutter:test --tests '*LocatorsTest'` → FAIL (classes
      missing).
- [ ] **Step 5: Implement.** `Locators.parse` reads `((Remotable) by).getRemoteParameters()` (`using`, `value`) and
      maps `using` through `Strategy`; for DESCENDANT/ANCESTOR first print `getRemoteParameters().value()` in a test to
      see how java-client 11 serializes the pair, then parse that form (it carries both sides' `using`/`value`). Native
      `AppiumBy.id` reports `using = "id"`.
- [ ] **Step 6: Run** the same test → PASS; then `./gradlew :integrations:selenide-flutter:check` → BUILD SUCCESSFUL.
- [ ] **Step 7: Commit** `feat(selenide-flutter): add module and FlutterBy locators`.

### Task 2: Widget tree model, parser and formatter

**Files:**

- Create: `tree/WidgetNode.java`, `tree/Rect.java`, `tree/WidgetTreeParser.java`, `tree/WidgetTreeFormatter.java`
- Create fixtures: `src/test/resources/trees/login-screen.json` (hand-written in the driver's `renderTree` schema:
  root `MaterialApp` → `Scaffold` → `TextField` key `[<'login_email'>]`, `TextField` key `[<'login_password'>]`,
  `ElevatedButton` key `[<'login_button'>]` with `state.enabled=false`, `Offstage` subtree with `Text` key
  `[<'hidden_banner'>]` `visible=false`, a `Text` "Sign in" with no key, an `Icon` with `semanticsLabel`
  "Settings icon" and zero-size rect)
- Test: `tree/WidgetTreeParserTest.java`, `tree/WidgetTreeFormatterTest.java`

**Interfaces:**

- Produces:
  - `record Rect(double x, double y, double width, double height) { boolean isEmpty(); }`
  - `record WidgetNode(String type, String elementType, String description, int depth, String key, String text,
String semanticsLabel, String tooltip, String hintText, boolean visible, boolean enabled, boolean focused,
Rect rect, List<WidgetNode> children) { Stream<WidgetNode> flatten(); }` (pre-order)
  - `WidgetTreeParser.parse(String json): WidgetNode`; `WidgetTreeParser.fromScriptResult(Object result):
WidgetNode` (accepts `String`, `Map`, or a single-element `List` as returned by `executeScript`)
  - `WidgetTreeFormatter.format(WidgetNode root): String`

- [ ] **Step 1: Write failing tests:**

```java
@Test void parsesFixtureTree() {
  WidgetNode root = WidgetTreeParser.parse(resource("trees/login-screen.json"));
  assertThat(root.type()).isEqualTo("MaterialApp");
  assertThat(root.flatten().map(WidgetNode::key)).contains("login_email", "login_password", "login_button", "hidden_banner");
  assertThat(root.flatten().filter(n -> "login_button".equals(n.key())).findFirst().orElseThrow().enabled()).isFalse();
}
@Test void normalizesFlutterKeyToString() {
  assertThat(WidgetTreeParser.normalizeKey("[<'login'>]")).isEqualTo("login");
  assertThat(WidgetTreeParser.normalizeKey("[<login>]")).isEqualTo("login");
  assertThat(WidgetTreeParser.normalizeKey(null)).isNull();
  assertThat(WidgetTreeParser.normalizeKey("[GlobalKey#1a2b3]")).isEqualTo("[GlobalKey#1a2b3]");
}
@Test void acceptsScriptResultShapes() {
  Map<String, Object> map = new ObjectMapper().readValue(resource("trees/login-screen.json"), Map.class);
  assertThat(WidgetTreeParser.fromScriptResult(map).type()).isEqualTo("MaterialApp");
  assertThat(WidgetTreeParser.fromScriptResult(List.of(map)).type()).isEqualTo("MaterialApp");
}
@Test void formatsOneLinePerNodeWithState() {
  String text = WidgetTreeFormatter.format(WidgetTreeParser.parse(resource("trees/login-screen.json")));
  assertThat(text).contains("  ElevatedButton key=\"login_button\" [disabled]");
  assertThat(text).contains("Text key=\"hidden_banner\" text=\"Hidden banner\" [hidden]");
  assertThat(text).contains("Icon label=\"Settings icon\" [zero-size]");
}
```

- [ ] **Step 2: Run** `./gradlew :integrations:selenide-flutter:test --tests '*WidgetTree*'` → FAIL.
- [ ] **Step 3: Implement.** Jackson tree reading via the `:common-utils:jackson:json` dependency; missing fields →
      `null`/`false`/empty list; `normalizeKey` strips `[<'…'>]` and `[<…>]` (ValueKey toString forms) and leaves
      everything else raw. Formatter line: two spaces per depth, `Type`, then `key="…"`, `text="…"`, `label="…"` when
      present, then flags `[hidden]` (`!visible`), `[disabled]`, `[zero-size]` (`rect.isEmpty()`), then `@x,y wxh`
      (integers).
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(selenide-flutter): parse and format the Flutter widget tree`.

### Task 3: Near-miss suggestions

**Files:**

- Create: `suggest/NearMiss.java`, `suggest/NearMissFinder.java`
- Test: `suggest/NearMissFinderTest.java` (uses the Task 2 fixture)

**Interfaces:**

- Consumes: `LocatorSpec`, `Strategy`, `WidgetNode` (Tasks 1–2).
- Produces: `record NearMiss(WidgetNode node, double score, List<String> reasons)`;
  `NearMissFinder.find(LocatorSpec spec, WidgetNode root, int limit): List<NearMiss>` (sorted by score desc;
  `NearMissFinder.DEFAULT_LIMIT = 5`).

Scoring (decided here): the compared field is `key` for KEY, `text` for TEXT/TEXT_CONTAINING, `semanticsLabel` for
SEMANTICS_LABEL, simple `type` for TYPE.

- Exact match on the field but the node is not hittable: score `1.0`, reason `"exact <field> match but <hidden |
disabled | zero-size>"` (all that apply).
- Equal ignoring case: `0.95`, `"differs only in case"`. Equal after trim: `0.9`, `"differs only in whitespace"`.
- One contains the other: `0.7`, `"partial match"`.
- Otherwise `1 - levenshtein/maxLen` (commons-text `LevenshteinDistance`, already on the classpath through
  `common-utils:core`), reason `"similar <field> (distance N)"`, kept only when ≥ `0.5`.
- For TEXT and KEY, a node whose `semanticsLabel` equals the wanted value exactly gets `0.8`,
  `"value found as semantics label; try FlutterBy.semanticsLabel"`.
- DESCENDANT/ANCESTOR: if `of` has no exact hittable match, return `of`'s suggestions with reasons prefixed `"of: "`;
  else search `matching` only inside the matched `of` subtrees (DESCENDANT) or along their ancestors (ANCESTOR),
  prefixing `"matching: "`.
- Non-Flutter strategies → empty list.

- [ ] **Step 1: Write failing tests:**

```java
@Test void typoInKeySuggestsRealKey() {
  List<NearMiss> m = NearMissFinder.find(Locators.parse(FlutterBy.key("login_btn")), tree, 5);
  assertThat(m.get(0).node().key()).isEqualTo("login_button");
  assertThat(m.get(0).reasons()).anyMatch(r -> r.startsWith("similar key"));
}
@Test void exactButHiddenExplainsWhy() {
  NearMiss top = NearMissFinder.find(Locators.parse(FlutterBy.key("hidden_banner")), tree, 5).get(0);
  assertThat(top.score()).isEqualTo(1.0);
  assertThat(top.reasons()).containsExactly("exact key match but hidden");
}
@Test void caseOnlyDifference() {
  assertThat(NearMissFinder.find(Locators.parse(FlutterBy.text("sign in")), tree, 5).get(0).reasons())
      .containsExactly("differs only in case");
}
@Test void textFoundAsSemanticsLabel() {
  assertThat(NearMissFinder.find(Locators.parse(FlutterBy.text("Settings icon")), tree, 5).get(0).reasons())
      .contains("value found as semantics label; try FlutterBy.semanticsLabel");
}
@Test void descendantReportsFailingSide() {
  List<NearMiss> m = NearMissFinder.find(
      Locators.parse(FlutterBy.descendant(FlutterBy.key("login_from"), FlutterBy.text("Sign in"))), tree, 5);
  assertThat(m.get(0).reasons().get(0)).startsWith("of: ");
}
@Test void respectsLimitAndThreshold() {
  assertThat(NearMissFinder.find(Locators.parse(FlutterBy.key("zzzzzzzzzz")), tree, 5)).isEmpty();
  assertThat(NearMissFinder.find(Locators.parse(FlutterBy.key("login")), tree, 2)).hasSizeLessThanOrEqualTo(2);
}
@Test void nativeStrategyHasNoSuggestions() {
  assertThat(NearMissFinder.find(Locators.parse(AppiumBy.id("login_btn")), tree, 5)).isEmpty();
}
```

- [ ] **Step 2: Run** `--tests '*NearMissFinderTest'` → FAIL. **Step 3:** implement. **Step 4:** run → PASS.
- [ ] **Step 5: Commit** `feat(selenide-flutter): rank near-miss widgets for a failed locator`.

### Task 4: Find tracing

**Files:**

- Create: `trace/Outcome.java`, `trace/FindAttempt.java`, `trace/FindTracer.java`, `trace/AttemptSummary.java`
- Test: `trace/FindTracerTest.java`, `trace/AttemptSummaryTest.java`

**Interfaces:**

- Consumes: `Locators.describe(By)`.
- Produces:
  - `enum Outcome { FOUND, NOT_FOUND, ERROR }`
  - `record FindAttempt(By by, String locator, Instant start, Duration duration, Outcome outcome, String detail)`
  - `final class FindTracer implements WebDriverListener` — constructor `FindTracer(Clock clock)`, plus
    `FindTracer()` using `Clock.systemUTC()`; overrides `beforeFindElement`/`afterFindElement`/
    `beforeFindElements`/`afterFindElements` for both `WebDriver` and `WebElement` targets, and `onError` (only for
    methods named `findElement`/`findElements`: `NoSuchElementException` → NOT_FOUND, anything else → ERROR with
    the exception's first line as `detail`); `afterFindElements` with an empty list → NOT_FOUND.
    `List<FindAttempt> drain()` returns and clears the current thread's attempts; `long totalAttempts()` returns
    the current thread's count since the last `resetTotal()`; `void resetTotal()`.
  - `record AttemptSummary(int count, Duration first, Duration last, Duration total, Outcome finalOutcome,
List<String> lines) { static AttemptSummary of(List<FindAttempt>); String render(); }` — `lines` collapse
    consecutive attempts with the same locator and outcome into `"<n>× <locator> → <OUTCOME> (avg <ms> ms)"`;
    `render()` starts with `"<count> find attempt(s), total <ms> ms, result <OUTCOME>"`.

- [ ] **Step 1: Write failing tests** (drive the listener directly with a mock `WebDriver` and a mutable test
      clock):

```java
@Test void recordsNotFoundThenFound() throws Exception {
  By by = FlutterBy.key("delayed_result");
  tracer.beforeFindElement(driver, by); clock.advance(40);
  tracer.onError(driver, findElementMethod, new Object[]{by},
      new InvocationTargetException(new NoSuchElementException("Unable to locate element")));
  tracer.beforeFindElement(driver, by); clock.advance(30);
  tracer.afterFindElement(driver, by, element);
  List<FindAttempt> attempts = tracer.drain();
  assertThat(attempts).extracting(FindAttempt::outcome).containsExactly(Outcome.NOT_FOUND, Outcome.FOUND);
  assertThat(attempts.get(0).duration()).isEqualTo(Duration.ofMillis(40));
  assertThat(tracer.drain()).isEmpty();
}
@Test void emptyFindElementsIsNotFound() { /* afterFindElements(driver, by, List.of()) → NOT_FOUND */ }
@Test void attemptsAreThreadLocal() throws Exception {
  tracer.beforeFindElement(driver, by); tracer.afterFindElement(driver, by, element);
  List<FindAttempt> other = CompletableFuture.supplyAsync(tracer::drain).get();
  assertThat(other).isEmpty();
  assertThat(tracer.drain()).hasSize(1);
}
@Test void summaryCollapsesRepeats() {
  AttemptSummary s = AttemptSummary.of(List.of(nf(40), nf(60), found(30)));
  assertThat(s.lines()).containsExactly(
      "2× flutter key \"delayed_result\" → NOT_FOUND (avg 50 ms)",
      "1× flutter key \"delayed_result\" → FOUND (avg 30 ms)");
  assertThat(s.render()).startsWith("3 find attempt(s), total 130 ms, result FOUND");
}
```

- [ ] **Step 2: Run** `--tests '*trace*'` → FAIL. **Step 3:** implement (`ThreadLocal<ArrayDeque<…>>` for pending
      starts keyed by `By`, `ThreadLocal<List<FindAttempt>>` for finished). **Step 4:** run → PASS.
- [ ] **Step 5: Commit** `feat(selenide-flutter): trace every element find attempt`.

### Task 5: Failure diagnostics and `FlutterVisibility`

**Files:**

- Create: `diagnostics/Diagnostics.java`, `diagnostics/FailureDiagnostics.java`, `report/DiagnosticsSink.java`,
  `report/AllureSink.java`, `report/LogSink.java`, `FlutterVisibility.java`
- Test: `diagnostics/FailureDiagnosticsTest.java`, `FlutterVisibilityTest.java`

**Interfaces:**

- Consumes: Tasks 1–4.
- Produces:
  - `record Diagnostics(String treeJson, String treeText, List<NearMiss> suggestions, String error, boolean
truncated)` (null when absent).
  - `final class FailureDiagnostics` — `FailureDiagnostics(Duration timeout, int maxChars)`; defaults
    `DEFAULT_TIMEOUT = Duration.ofSeconds(10)`, `DEFAULT_MAX_CHARS = 2_000_000`;
    `Diagnostics collect(JavascriptExecutor driver, By failed)`: if `Locators.parse(failed).strategy().isFlutter()`
    is false → `Diagnostics` with everything null and empty suggestions, no script call; otherwise
    `executeScript("flutter: renderTree", Map.of())` on a single-thread executor bounded by `timeout`, then
    parse, format, `NearMissFinder.find(..., DEFAULT_LIMIT)`; JSON longer than `maxChars` is cut and
    `truncated = true` (the tree text and suggestions are still built from the full tree when parsing succeeded);
    any exception or timeout → `error = "<ExceptionClass>: <message>"`, never thrown.
  - `interface DiagnosticsSink { void attach(String name, String mimeType, String content); }`;
    `AllureSink` attaches only when `Allure.getLifecycle().getCurrentTestCaseOrStep()` is present;
    `LogSink` logs at INFO (summaries) / WARN (failure diagnostics) via SLF4J.
  - `FlutterVisibility.install()` / `install(FailureDiagnostics, List<DiagnosticsSink>)` / `uninstall()` /
    `FindTracer tracer()`. `install` is idempotent: `SelenideLogger.addListener("FlutterVisibility", listener)` and
    `WebDriverRunner.addListener(tracer)` once (must run before the driver is created; README says so).
  - Attachment names (exact): `"Flutter: element search"` (text/plain, `AttemptSummary.render()` + lines),
    `"Flutter: widget tree (json)"` (application/json), `"Flutter: widget tree"` (text/plain),
    `"Flutter: suggestions"` (text/plain, one line per `NearMiss`: `"<score> <node line> — <reasons joined by "; ">"`),
    `"Flutter: diagnostics error"` (text/plain).

Listener behaviour: `beforeEvent` drains and discards leftovers; `afterEvent` drains, attaches the element-search
summary if there were attempts, and if `event.getStatus() == LogEvent.EventStatus.FAIL` and
`WebDriverRunner.hasWebDriverStarted()`, runs `collect` with the last attempt's `By` on the
`WebDriverRunner.getWebDriver()` cast to `JavascriptExecutor`, attaching what is present. Every sink call is wrapped
so a sink failure is logged and never thrown. Attachments may land on the test case rather than the step if
`AllureSelenide` closed the step first; that is acceptable.

- [ ] **Step 1: Write failing tests** (mock `JavascriptExecutor`, a recording `DiagnosticsSink`):

```java
@Test void collectsTreeAndSuggestions() {
  when(js.executeScript("flutter: renderTree", Map.of())).thenReturn(fixtureMap);
  Diagnostics d = new FailureDiagnostics(Duration.ofSeconds(1), 1_000_000).collect(js, FlutterBy.key("login_btn"));
  assertThat(d.treeText()).contains("login_button");
  assertThat(d.suggestions().get(0).node().key()).isEqualTo("login_button");
  assertThat(d.error()).isNull();
}
@Test void diagnosticsTimeoutKeepsOriginalError() {
  when(js.executeScript(anyString(), any())).thenAnswer(i -> { Thread.sleep(2_000); return fixtureMap; });
  Diagnostics d = new FailureDiagnostics(Duration.ofMillis(100), 1_000_000).collect(js, FlutterBy.key("x"));
  assertThat(d.error()).startsWith("TimeoutException");
  assertThat(d.treeJson()).isNull();
}
@Test void hugeTreeIsTruncated() {
  Diagnostics d = new FailureDiagnostics(Duration.ofSeconds(1), 200).collect(js, FlutterBy.key("login_btn"));
  assertThat(d.truncated()).isTrue();
  assertThat(d.treeJson()).hasSizeLessThanOrEqualTo(200 + "\n…[truncated]".length()).endsWith("…[truncated]");
}
@Test void nativeLocatorFailureSkipsRenderTree() {
  new FailureDiagnostics(Duration.ofSeconds(1), 1000).collect(js, AppiumBy.id("login_btn"));
  verifyNoInteractions(js);
}
@Test void failedCommandAttachesSearchTreeAndSuggestions() { /* install with recording sink, feed tracer attempts,
  call listener.afterEvent(failedEvent) with a started mock driver → sink names contain all four Flutter
  attachments except diagnostics error */ }
@Test void noDriverSkipsDiagnostics() { /* hasWebDriverStarted false → only "Flutter: element search" */ }
@Test void installIsIdempotent() {
  FlutterVisibility.install(); FlutterVisibility.install();
  assertThat(SelenideLogger.hasListener("FlutterVisibility")).isTrue();
  /* one failed event → each attachment name recorded exactly once */
  FlutterVisibility.uninstall();
  assertThat(SelenideLogger.hasListener("FlutterVisibility")).isFalse();
}
```

Static Selenide state (`WebDriverRunner`) is reached through a small package-private seam in `FlutterVisibility`
(`Supplier<Optional<WebDriver>> currentDriver`) so tests do not start a driver.

- [ ] **Step 2: Run** `--tests '*FailureDiagnosticsTest' --tests '*FlutterVisibilityTest'` → FAIL.
- [ ] **Step 3: Implement.** **Step 4:** run → PASS; `./gradlew :integrations:selenide-flutter:check` → SUCCESS.
- [ ] **Step 5: Commit** `feat(selenide-flutter): attach widget tree and suggestions on failure`.

### Task 6: Configuration and `FlutterDriverProvider`

**Files:**

- Create: `config/FlutterConfiguration.java`, `driver/FlutterDriverProvider.java`
- Test: `driver/FlutterDriverProviderTest.java`

**Interfaces:**

- Produces:
  - `FlutterConfiguration extends org.aeonbits.owner.Config`, `@LoadPolicy(MERGE)`,
    `@Sources({"system:properties", "system:env", "classpath:flutter.properties"})`, loaded through
    `ConfigRegistry.get(FlutterConfiguration.class)`. Keys and defaults: `APPIUM_URL` = `http://127.0.0.1:4723`;
    `FLUTTER_APP_PATH` = `test-app/build/app/outputs/flutter-apk/app-debug.apk` (relative to the module dir, the
    Gradle test working dir); `FLUTTER_DEVICE_NAME` = `Android Emulator`; `FLUTTER_DRIVER_FIND_TIMEOUT_MS` = `0`;
    `FLUTTER_SERVER_LAUNCH_TIMEOUT_MS` = `120000`.
  - `public class FlutterDriverProvider implements WebDriverProvider` — `createDriver(Capabilities)` returns
    `new FlutterAndroidDriver(new URL(appiumUrl), options(config).merge(capabilities))`;
    `static FlutterDriverOptions options(FlutterConfiguration config)`: automation name `FlutterIntegration`,
    platform `Android`, device name, absolute app path, element wait timeout, server launch timeout (use the setters
    java-client 11's `FlutterDriverOptions` actually has; check its source).

- [ ] **Step 1: Write failing test:**

```java
@Test void buildsFlutterIntegrationOptions() {
  System.setProperty("FLUTTER_APP_PATH", "/apps/demo.apk");
  FlutterDriverOptions o = FlutterDriverProvider.options(ConfigFactory.create(FlutterConfiguration.class));
  assertThat(o.getCapability("appium:automationName")).isEqualTo("FlutterIntegration");
  assertThat(o.getCapability("platformName").toString()).isEqualToIgnoringCase("android");
  assertThat(o.getCapability("appium:app")).isEqualTo("/apps/demo.apk");
  assertThat(o.getCapability("appium:flutterElementWaitTimeout")).isEqualTo(0L);
}
```

(Clear the property in `@AfterMethod`.)

- [ ] **Step 2: Run** → FAIL. **Step 3:** implement. **Step 4:** run → PASS.
- [ ] **Step 5: Commit** `feat(selenide-flutter): add Flutter driver provider and configuration`.

### Task 7: Flutter test app and build script

**Files:**

- Create: `integrations/selenide-flutter/test-app/` — `pubspec.yaml`, `lib/main.dart`, `lib/tagged.dart`,
  `lib/screens/{login,delayed,long_list,duplicates,label_only,offstage}.dart`, `integration_test/appium_test.dart`,
  generated `android/` platform folder (`flutter create --platforms=android .`), `.gitignore` entries for
  `build/`, `.dart_tool/`
- Create: `tools/environment/scripts/flutter/versions.env`, `tools/environment/scripts/flutter/build_app.sh`
- Modify: `.prettierignore` if Prettier picks up generated files

**Interfaces:**

- Produces: the app's widget ids (the Semantics identifier equals the key string; device tests rely on these exact
  values):
  - home: `home_login`, `home_delayed`, `home_list`, `home_duplicates`, `home_label`, `home_offstage`
  - login: `login_email`, `login_password`, `login_button` (disabled until the email contains `@` and the password
    has ≥ 6 chars), `login_welcome` (text `"Welcome, <email>"`)
  - delayed: `delayed_load`, `delayed_result` (appears 2 s after tapping, text `"Loaded"`)
  - long list: `list_view`, `list_item_<0..99>` (text `"Item <n>"`), `list_selected` (text `"Selected <n>"`)
  - duplicates: `card_a`, `card_b`, each with an untagged button text `"Details"`; `details_result` (text
    `"Opened A"` / `"Opened B"`)
  - label only: an `IconButton` with `Semantics(label: "Settings icon")` and no key/identifier; `label_result`
    (text `"Settings opened"`)
  - offstage: `Offstage(offstage: true)` around tagged `hidden_banner` (text `"Hidden banner"`); `covered_button`
    under an opaque full-size `Container`
  - `Tagged(String id, Widget child)` wraps `child` in `KeyedSubtree(key: ValueKey(id))` and
    `Semantics(identifier: id)`.
- `versions.env` keys: `APPIUM_VERSION` (latest 3.x), `UIAUTOMATOR2_DRIVER_VERSION` (latest whose peer range
  includes Appium 3), `FLUTTER_INTEGRATION_DRIVER_VERSION=2.0.3`, `ANDROID_API_LEVEL=34`,
  `ANDROID_SYSTEM_IMAGE="system-images;android-34;google_apis;<arch>"` (arch chosen at run time: `arm64-v8a` on
  Apple Silicon, `x86_64` otherwise), `AVD_NAME=q4j-flutter`.
- `build_app.sh`: checks `flutter --version` equals the pinned `environment.flutter` in `pubspec.yaml` (warn, not
  fail, on mismatch); runs `flutter pub get` and `flutter build apk --debug -t integration_test/appium_test.dart`
  in `test-app/`; skips the build when the apk exists and nothing under `lib/`, `integration_test/`, `android/`,
  `pubspec.yaml` is newer (`find -newer`); prints the apk path.

- [ ] **Step 1:** Create the app; pin `environment.flutter` to the exact current stable from `flutter --version`
      and `appium_flutter_server: 0.0.33` under `dev_dependencies` (plus `integration_test: sdk: flutter`).
      `appium_test.dart` calls `initializeTest(app: const TestApp())`.
- [ ] **Step 2: Verify** `flutter analyze` in `test-app/` → `No issues found!`.
- [ ] **Step 3: Verify** `tools/environment/scripts/flutter/build_app.sh` → prints
      `…/test-app/build/app/outputs/flutter-apk/app-debug.apk`; a second run prints `apk is up to date`.
- [ ] **Step 4:** `npx prettier --check .` → no new warnings.
- [ ] **Step 5: Commit** `feat(selenide-flutter): add Flutter test app`.

### Task 8: One-command local environment

**Files:**

- Create: `tools/environment/scripts/flutter/run_device_tests.sh`, `tools/environment/scripts/flutter/stop_device_env.sh`

**Interfaces:**

- Consumes: `versions.env`, `build_app.sh` (Task 7).
- Produces: `run_device_tests.sh [gradle-task]` (default `flutterDeviceTest`; accepts `flutterComparison`,
  `flutterFailureDemo`), run from the repo root. State under `build/flutter-env/`: `appium.pid`, `appium.log`,
  `emulator.pid` (only for processes the script started), `appium-home/` (`APPIUM_HOME`), `npm/` (local Appium
  install prefix).

Behaviour, in order: (1) check `java`, `node`, `npm`, `flutter`, `adb`, `emulator`, `sdkmanager`/`avdmanager` and
print each missing one with an install hint, exit 1 if any missing; (2) install the pinned Appium into
`build/flutter-env/npm` and the two pinned drivers into `APPIUM_HOME` if `appium driver list --installed --json`
lacks them; (3) `build_app.sh`; (4) if `adb devices` shows no emulator, boot `AVD_NAME` headless-capable
(`-no-snapshot-save`), and if the AVD does not exist print the exact `sdkmanager` + `avdmanager create avd` commands
and exit 1; wait for `sys.boot_completed`; (5) if nothing listens on 4723, start Appium with
`--log build/flutter-env/appium.log`; wait for `/status`; (6) run `./gradlew :integrations:selenide-flutter:<task>`
and exit with its code. `stop_device_env.sh` kills only processes recorded in the pid files. POSIX-friendly bash,
works on macOS and Linux (`CI=true` skips step 4 because the emulator action boots it).

- [ ] **Step 1:** Write both scripts; `shellcheck` them → no errors.
- [ ] **Step 2: Verify** on a machine without an AVD: the script prints the create-AVD commands and exits 1.
- [ ] **Step 3: Verify** with the AVD present: `run_device_tests.sh` reaches the Gradle step (the task may not exist
      yet; `Task 'flutterDeviceTest' not found` is the expected result until Task 9). `stop_device_env.sh` leaves no
      Appium/emulator process started by the script (`pgrep -f appium` empty).
- [ ] **Step 4: Commit** `feat(selenide-flutter): one-command local device environment`.

### Task 9: Device scenarios in both modes and Gradle tasks

**Files:**

- Modify: `integrations/selenide-flutter/build.gradle` (`deviceTest` source set and tasks)
- Create under `src/deviceTest/java/dev/quokkify/flutter/device/`: `MobileMode.java`, `ModeLocators.java`,
  `NotReachableInModeException.java`, `NativeAndroidDriverProvider.java`, `BaseMobileTest.java`,
  `screens/{HomeScreen,LoginScreen,DelayedScreen,LongListScreen,DuplicatesScreen,LabelOnlyScreen,OffstageScreen}.java`
  (each implements `dev.quokkify.impl.Page`, no `@PageUrl`), steps/verification for Login in the q4j
  `PageSteps`/`Verification` style, `scenarios/*Test.java`, `FailureDemoTest.java`
- Create: `src/test/resources/trees/recorded-login.json` (recorded from the running app in Step 5) and a parser test
  case using it in `WidgetTreeParserTest`

**Interfaces:**

- Consumes: Tasks 1–8.
- Produces:
  - `enum MobileMode { FLUTTER, NATIVE; static List<MobileMode> fromProperty() }` reading system property
    `mobile.modes` (comma list, default `flutter`).
  - `ModeLocators(MobileMode)` with `By id(String)` (flutter `FlutterBy.key`, native `AppiumBy.id`),
    `By label(String)` (flutter `semanticsLabel`, native `accessibilityId`), `By text(String)` (flutter
    `FlutterBy.text`, native `AppiumBy.androidUIAutomator("new UiSelector().description(\"…\")")` or `.text(…)`,
    whichever matches Flutter's Android semantics; verify on the device), `By descendant(By of, By matching)`
    (native throws `NotReachableInModeException`).
  - `BaseMobileTest`: constructor takes `MobileMode`; `@Factory`-friendly; `@BeforeSuite` installs
    `AllureSelenide` (`screenshots(true).savePageSource(true)`) and `FlutterVisibility.install()`;
    `@BeforeClass` sets `Configuration.browser` to `FlutterDriverProvider` or `NativeAndroidDriverProvider` class
    name and `Configuration.timeout = 10_000`, closing the driver if the mode changed; `@AfterClass` closes the
    driver; a TestNG `IHookable`/listener turns `NotReachableInModeException` into a skip with reason
    `"not reachable in <mode> mode: <detail>"` and records it for the comparison.
  - Scenario classes (each a `@Factory` over `MobileMode.fromProperty()`): `LoginTest` (fills form, button disabled
    before valid, enabled after, welcome text), `DelayedWidgetTest` (result `"Loaded"` appears), `LongListTest`
    (scrolls to `list_item_80`: flutter via `flutter: scrollTillVisible`, native via `UiScrollable`; tap → `"Selected
80"`), `DuplicatesTest` (tap `Details` inside `card_b` → `"Opened B"`), `LabelOnlyTest` (tap `"Settings icon"` →
    `"Settings opened"`), `OffstageTest` (`hidden_banner` and `covered_button` are not visible / not clickable).
  - `FailureDemoTest` (both modes): `tapsMistypedKey` (`id("login_btn")`), `tapsOffstageBanner`
    (`id("hidden_banner").click()`), both expected to fail.
  - Gradle: source set `deviceTest` (classpath = main + test runtime); tasks `flutterDeviceTest` (scenarios,
    `mobile.modes=flutter`, excludes `FailureDemoTest`), `flutterFailureDemo` (only `FailureDemoTest`,
    `mobile.modes=flutter,native`), `flutterComparison` (everything, `mobile.modes=flutter,native`,
    `ignoreFailures = true`; verdict in Task 10). All three `outputs.upToDateWhen { false }`, `maxParallelForks = 1`,
    TestNG `group-by-instances`. `check` does not depend on them.

- [ ] **Step 1:** Add the source set and tasks; `./gradlew :integrations:selenide-flutter:tasks --group verification`
      lists the three tasks.
- [ ] **Step 2:** Write screens, `ModeLocators`, `BaseMobileTest`, `NativeAndroidDriverProvider` (UiAutomator2,
      same apk) and the scenario classes.
- [ ] **Step 3: Verify flutter mode:** `tools/environment/scripts/flutter/run_device_tests.sh` → all scenarios
      PASS.
- [ ] **Step 4: Verify the driver timeout:** in the Allure result of `FailureDemoTest.tapsMistypedKey` (run
      `run_device_tests.sh flutterFailureDemo`), each NOT_FOUND attempt in `"Flutter: element search"` is under
      1000 ms. If attempts take ~5000 ms, `0` means "driver default": change the `FLUTTER_DRIVER_FIND_TIMEOUT_MS`
      default to `1` in Task 6's interface and test, and re-run.
- [ ] **Step 5: Verify the diagnostics on a real tree:** the same result has `"Flutter: widget tree"` and
      `"Flutter: suggestions"`, whose first line names `login_button`. Save the attached JSON as
      `src/test/resources/trees/recorded-login.json`, add `parsesRecordedTree` to `WidgetTreeParserTest` (asserts the
      recorded keys normalize to `login_email`, `login_password`, `login_button`), adjust `normalizeKey` if the real
      key format differs from the hand-written fixture, and run the unit tests → PASS.
- [ ] **Step 6: Verify native mode:** `./gradlew :integrations:selenide-flutter:flutterDeviceTest
-Dmobile.modes=native` (pass the property through in the task) → scenarios PASS or SKIP with
      `not reachable in native mode`; no other failures.
- [ ] **Step 7: Commit** `test(selenide-flutter): device scenarios in flutter and native modes`.

### Task 10: Comparison summary and verdict

**Files:**

- Create: `src/test/java/dev/quokkify/flutter/comparison/{Status,ScenarioResult,ComparisonReport}.java` (pure; in
  the `test` source set so they are unit-tested and visible to `deviceTest`, but not shipped in the module jar)
- Create: `src/deviceTest/java/dev/quokkify/flutter/device/ComparisonRecorder.java`
- Modify: `build.gradle` (`flutterComparison` verdict `doLast`)
- Test: `src/test/java/dev/quokkify/flutter/comparison/ComparisonReportTest.java`

**Interfaces:**

- Consumes: `FindTracer.totalAttempts()/resetTotal()`, `MobileMode`, `NotReachableInModeException` handling.
- Produces:
  - `enum Status { PASSED, FAILED, NOT_REACHABLE }`; `record ScenarioResult(String scenario, String mode, Status
status, long durationMs, long findAttempts, boolean expectedToFail, String diagnosticsExcerpt)`.
  - `ComparisonReport.render(List<ScenarioResult>): String` — Markdown: a table with one row per scenario and one
    column group per mode (`status`, `ms`, `finds`), then a "Failure demo" section showing each mode's
    `diagnosticsExcerpt` (first 15 lines of the Flutter suggestions, or of the native page source) side by side.
  - `ComparisonReport.verdict(List<ScenarioResult>): Optional<String>` — empty when every non-demo result is PASSED
    or NOT_REACHABLE and every demo result is FAILED in both modes; otherwise a one-line reason.
  - `ComparisonRecorder implements ITestListener, ISuiteListener` (registered on the deviceTest tasks): records a
    `ScenarioResult` per test method; `onFinish(ISuite)` writes `build/flutter-comparison/summary.md`,
    `build/flutter-comparison/results.json` and `build/flutter-comparison/verdict.txt` (`OK` or the reason), and
    attaches `summary.md` to Allure as `"Flutter vs native comparison"` (text/markdown).
  - `flutterComparison` `doLast`: reads `verdict.txt`; anything but `OK` → `GradleException` with that text.

- [ ] **Step 1: Write failing tests:**

```java
@Test void rendersBothModesPerScenario() {
  String md = ComparisonReport.render(List.of(
      new ScenarioResult("LoginTest.logsIn", "flutter", PASSED, 4200, 12, false, null),
      new ScenarioResult("LoginTest.logsIn", "native", PASSED, 3900, 9, false, null),
      new ScenarioResult("DuplicatesTest.opensCardB", "native", NOT_REACHABLE, 0, 0, false, null)));
  assertThat(md).contains("| LoginTest.logsIn | PASSED | 4200 | 12 | PASSED | 3900 | 9 |");
  assertThat(md).contains("NOT_REACHABLE");
}
@Test void verdictRequiresDemoToFailInBothModes() {
  assertThat(ComparisonReport.verdict(List.of(demo("flutter", FAILED), demo("native", FAILED)))).isEmpty();
  assertThat(ComparisonReport.verdict(List.of(demo("flutter", FAILED), demo("native", PASSED))))
      .contains("failure demo passed in native mode");
  assertThat(ComparisonReport.verdict(List.of(scenario("flutter", FAILED)))).isPresent();
}
```

- [ ] **Step 2: Run** → FAIL. **Step 3:** implement. **Step 4:** run → PASS.
- [ ] **Step 5: Verify end to end:** `tools/environment/scripts/flutter/run_device_tests.sh flutterComparison` →
      BUILD SUCCESSFUL, `build/flutter-comparison/verdict.txt` is `OK`, `summary.md` has both modes and the demo
      section.
- [ ] **Step 6: Commit** `test(selenide-flutter): compare flutter and native modes`.

### Task 11: CI workflow

**Files:**

- Create: `.github/workflows/flutter-device-tests.yml`

**Interfaces:**

- Consumes: `versions.env`, `build_app.sh`, `run_device_tests.sh` (with `CI=true`), Gradle tasks from Tasks 9–10.

Workflow (decided): name `Flutter device tests`; triggers `pull_request` (branches `main`, paths
`integrations/selenide-flutter/**`, `tools/environment/scripts/flutter/**`,
`.github/workflows/flutter-device-tests.yml`, `gradle/libs.versions.toml`), `schedule` (`cron: "30 2 * * *"`),
`workflow_dispatch`; `permissions: contents: read`; `concurrency` per ref with cancel-in-progress; one job on
`ubuntu-latest`, `timeout-minutes: 45`. Steps: checkout; enable KVM (udev rule
`KERNEL=="kvm", GROUP="kvm", MODE="0666"` + `udevadm trigger`); `quokkify/project-toolkit/actions/setup-java-gradle`
(same SHA as `build.yml`); `subosito/flutter-action` with `flutter-version-file:
integrations/selenide-flutter/test-app/pubspec.yaml`, `channel: stable`, cache on; `actions/setup-node`; source
`versions.env` into `$GITHUB_ENV`; `build_app.sh`; `reactivecircus/android-emulator-runner` with
`api-level: ${{ env.ANDROID_API_LEVEL }}`, `arch: x86_64`, `target: google_apis`, `disable-animations: true`,
`emulator-options: -no-window -no-snapshot-save -no-audio -no-boot-anim`, `script:
tools/environment/scripts/flutter/run_device_tests.sh ${{ github.event_name == 'pull_request' &&
'flutterDeviceTest' || 'flutterComparison' }}`; always upload `integrations/selenide-flutter/build/allure-results`,
`build/flutter-env/appium.log` and (non-PR) `integrations/selenide-flutter/build/flutter-comparison` with
`actions/upload-artifact` (repo SHA), retention 14 days; on non-PR runs append `summary.md` to
`$GITHUB_STEP_SUMMARY`. Not added to any required-check list.

- [ ] **Step 1:** Write the workflow; pin every action by SHA with a version comment.
- [ ] **Step 2: Verify syntax:** `actionlint .github/workflows/flutter-device-tests.yml` → no output (or the repo's
      lint workflow equivalent).
- [ ] **Step 3: Commit** `ci(selenide-flutter): run device tests on an Android emulator`.
- [ ] **Step 4: Verify on GitHub** (after the owner allows pushing the branch): the PR run executes
      `flutterDeviceTest` and is green; a `workflow_dispatch` run on the branch executes `flutterComparison`, is green,
      and its summary shows the comparison table.

### Task 12: README

**Files:**

- Create: `integrations/selenide-flutter/README.md`
- Modify: root `README.md` module list (if it lists modules)

Content (decided): what the module does and why (the black-box find problem); prerequisites; the one command
(`run_device_tests.sh` with its three task names) and `stop_device_env.sh`; instrumenting your own app
(`appium_flutter_server`, `integration_test/appium_test.dart`, debug build, `Tagged`-style key + Semantics
identifier convention); configuration keys table (Task 6); base-test setup (`AllureSelenide` +
`FlutterVisibility.install()` before the first driver); a `FlutterBy` + `Page`/`PageSteps` example taken from
`LoginScreen`; an excerpt of the four failure attachments from the real `tapsMistypedKey` run; the comparison task
and where its summary lands; known limits (Android only, Appium 3 pin, debug builds, `hitTestable`).

- [ ] **Step 1:** Write it. **Step 2:** `npx prettier --check integrations/selenide-flutter/README.md` → passes.
- [ ] **Step 3: Commit** `docs(selenide-flutter): add module README`.
