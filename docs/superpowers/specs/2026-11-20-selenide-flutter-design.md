# Selenide Flutter: Appium tests for Flutter apps with visible element search

Module: `integrations/selenide-flutter` (new), package `dev.quokkify.flutter`. Test app:
`integrations/selenide-flutter/test-app/` (new Flutter project).

## Intent

Testing a Flutter app through Appium's `appium-flutter-integration-driver` works, but an earlier attempt by the
owner was abandoned because element search was a black box at run time: the report did not show which locator
was used, how the search went, or why it failed. The driver itself answers a failed find with a bare
`"Unable to locate element"` (HTTP 404 / W3C `no such element`): no locator, no timing, no candidates.

This module lets q4j users write Flutter tests with ordinary Selenide (`$(FlutterBy.key("login"))`) and the
existing q4j `PageSteps` / `Verification` model, and makes every element search visible in the Allure report and
the log: the locator, every find attempt with its duration and outcome, and, on failure, the Flutter widget tree
plus near-miss suggestions ("no key `login_btn`; found key `login_button`, hidden").

It is an experiment. Success means: one command builds the test app and runs its tests locally on an Android
emulator, the same tests run in GitHub Actions on `ubuntu-latest`, a deliberately failing test produces a
report that explains the failure without opening the Appium server log, and a side-by-side run of the same
scenarios through the Flutter driver and through plain native automation shows whether the Flutter path is worth
it (see Flutter vs native comparison).

## Goals

- A Selenide-native way to find Flutter widgets by key, text, text-containing, widget type, semantics label, and
  descendant/ancestor combinations.
- Run-time visibility of element search:
  - every Selenide command on a Flutter element is an Allure step that lists each underlying find attempt
    (strategy, value, duration, found / not found / error);
  - on failure, attachments with the widget tree (raw JSON and a readable indented text form) and a ranked list of
    near-miss candidates, with the reasons each one is close and why it did not match (different key, hidden,
    offstage, zero size, disabled);
  - the same information in the SLF4J log, so it is visible without Allure.
- A small Flutter test app in the repo that exercises the hard cases (see Test app).
- One command to build the app, bring up the emulator and Appium if needed, and run the device tests locally.
- A GitHub Actions job that runs the device tests on an Android emulator on `ubuntu-latest`.
- Unit tests for everything that does not need a device (locator factory, suggestion ranking on recorded tree
  fixtures, tree formatting), running in the normal per-module CI matrix on every PR.

## Non-goals

- iOS. It comes in a later iteration if Android works; the design must not block it (no Android-only types in the
  public API beyond the driver provider).
- A supported no-instrumentation mode in the module's API (plain UiAutomator2 with Flutter
  `Semantics(identifier:)`). Native automation appears only as the comparison baseline in this module's own tests
  (see Flutter vs native comparison); turning it into a product mode is a later decision informed by that
  comparison.
- Cloud device farms (BrowserStack, Sauce Labs). There is no account.
- An authoring-time inspector (a UI or CLI to browse the live tree). The failure-time tree dump covers part of
  that need.
- Changing Selenide's own exception messages. Diagnostics go to Allure attachments and the log.
- Publishing the module to Maven Central in this iteration.

## Anti-goals

- Diagnostics that hide or replace the original failure. If collecting the tree or suggestions fails, the
  original Selenide error is still what the test reports, and the diagnostic failure is logged and attached as
  text.
- Fetching the widget tree on every step. It is expensive on large apps; it is fetched only on failure.
- Two competing wait loops. Selenide owns waiting (`Configuration.timeout`); the driver's own server-side wait
  is set as low as the driver allows, so one Selenide poll is one quick find.
- A device job so slow or flaky that people learn to ignore it.

## Constraints

- `main` uses Selenide 7.19.1. `com.codeborne:selenide-appium:7.19.1` brings `io.appium:java-client` 11.0.0 and
  Selenium 4.51; add it to `gradle/libs.versions.toml` with `version.ref = "selenide"` so Renovate moves it with
  Selenide.
- java-client 11 already has the Flutter API: `io.appium.java_client.flutter.FlutterDriverOptions`,
  `android.FlutterAndroidDriver`, `AppiumBy.flutterKey/flutterText/flutterTextContaining/flutterType/
flutterSemanticsLabel/flutterDescendant/flutterAncestor`, and `flutter:` script commands (`waitForVisible`,
  `scrollTillVisible`, `renderTree`, gestures).
- `appium-flutter-integration-driver` 2.0.3 (2025-10-17) declares `appium ^3`. Appium 4 became npm `latest` on
  2026-11-15. Pin Appium 3.x and driver 2.0.3 in the scripts and CI; do not float to `latest`.
- The app must be built in debug or profile mode with the Dart dev dependency `appium_flutter_server` (0.0.33)
  and an `integration_test/appium_test.dart` entry point that calls `initializeTest(app: ...)`. Android build:
  `./gradlew app:assembleDebug -Ptarget=integration_test/appium_test.dart` (or the `flutter build apk` equivalent).
- The driver's find uses `hitTestable()`: offstage or covered widgets are "not found". Suggestions must say so
  when a matching widget exists in the tree but is not hit-testable.
- `flutter: renderTree` with no `widgetType` returns the whole tree as JSON. Each node has `type`,
  `elementType`, `description`, `depth`, `key`, `attributes{text, semanticsLabel, tooltip, hintText}`,
  `visual{...}`, `state{enabled, focused, visible}`, `rect{x, y, width, height}`, `children`. `getPageSource`
  returns native XML, not this tree.
- The Android emulator needs KVM. On macOS (Apple Silicon) it runs natively, not in Docker. On `ubuntu-latest`
  KVM must be enabled in the job.
- Repo conventions: actions pinned by commit SHA with a version comment; Prettier checks Markdown; Checkstyle and
  SpotBugs apply to Java; the repo is public, so no employer data, internal URLs, tokens or machine paths.

## Approaches

The comparison of existing options (2026-11-20) found no Java solution that shows per-find locator, timing and a
widget-tree dump on failure. SHAFT Engine is the closest (Java, TestNG, Allure, Flutter integration driver
locators) but is not Selenide-based; its failure-time tree dump was not confirmed. Maestro, Patrol, WebdriverIO
and plain `integration_test` are other stacks. The driver's own errors carry no context. So a Selenide module with
a visibility layer adds value.

- **A. Visibility layer over `selenide-appium` (chosen).** Tests use Selenide's `$()` with `FlutterBy` locators.
  A Selenium `WebDriverListener` records each find attempt; a Selenide `LogEventListener` groups attempts under
  the Selenide command and, on failure, fetches `renderTree` and attaches the tree and suggestions. Keeps the
  whole Selenide API and q4j page model. Depends on how Selenide polls, and the tree is expensive (fetched only on
  failure).
- **B. A plus a no-instrumentation mode** (UiAutomator2 + `Semantics(identifier:)`). Wider reach, about twice the
  work, weaker diagnostics (no widget tree). Deferred.
- **C. Own `FlutterElement` with waits through `flutter: waitForVisible`, no `SelenideElement`.** Full control of
  the search, but leaves Selenide and re-implements what it already does. Rejected: works against the reason for
  the module.

The owner picked A.

## Decided with the owner

- Approach A; Android only; iOS later if this works.
- Focus: run-time visibility of element search (in the report), not an authoring-time inspector.
- The Flutter test app lives in this repo.
- One command for local runs; device tests also in CI on a GitHub-hosted `ubuntu-latest` emulator (no cloud farm,
  no self-hosted runner).
- Start with the simplest working variant; extend later.
- The tests check the same scenarios both through the Flutter driver and natively (Android now, iOS with the iOS
  iteration), to see whether the Flutter approach pays off.
- CI: PRs that touch the module run the flutter mode only; nightly and manual runs do the full comparison with the
  failure demo, and the demo does not turn the job red.

## Design

### Module and dependencies

`integrations/selenide-flutter`, registered in `settings.gradle` next to the other Selenide modules.

- `api project(":integrations:selenide")` (the `Page` / `PageSteps` / `Verification` model).
- `api libs.selenide.appium` (new catalog entry).
- `implementation project(":common-utils:config")` for configuration, like `BrowserConfiguration`.
- Allure attachments through `io.qameta.allure:allure-java-commons` (`Allure.addAttachment`, step lifecycle);
  version from the existing `allure` catalog entry.
- Test: `testng-extensions`, `allure-testng`, `allure-selenide`, AssertJ, Mockito, like `integrations/selenide`.

### Locators: `FlutterBy`

Static factories that return the java-client `AppiumBy` Flutter locators, wrapped only to give a readable
`toString()` (for example `flutter key "login_button"`, `flutter text "Sign in"`, `flutter descendant(of: key
"list", matching: text "Item 3")`). The wrapper must stay a `Remotable` `By` that the driver receives unchanged
(delegate `getRemoteParameters`); if wrapping breaks remoting, fall back to returning `AppiumBy` directly and
render the readable form in the listener instead.

### Driver: `FlutterDriverProvider`

A Selenide `WebDriverProvider` that builds `FlutterAndroidDriver` with `FlutterDriverOptions`
(`automationName = FlutterIntegration`, UiAutomator2 underneath). Configuration through an `owner` interface
`FlutterConfiguration` (`@Config.Sources({"system:env", "classpath:flutter.properties"})`, like
`BrowserConfiguration`):

- `APPIUM_URL` (default `http://127.0.0.1:4723`);
- `FLUTTER_APP_PATH` (path to the built apk);
- `FLUTTER_DEVICE_NAME` (default `Android Emulator`);
- `FLUTTER_DRIVER_FIND_TIMEOUT_MS` (the driver's `flutterElementWaitTimeout`, default as low as the driver
  accepts; see Builder's choices);
- server launch timeout.

Local and CI use the same provider; only the environment differs.

### Turning the visibility layer on (decided by the designer)

One explicit call, `FlutterVisibility.install()`, made once in the user's base test (the README shows it next to
`SelenideLogger.addListener("AllureSelenide", new AllureSelenide())`). It registers the Selenide
`LogEventListener` and makes the find-tracing `WebDriverListener` available to any driver provider; both the
Flutter provider and the native-mode provider in this module's tests apply it. The module does not create Allure
steps itself and does not depend on `allure-selenide` at compile or run time: steps come from `AllureSelenide`,
which the user registers as in `integrations/selenide`'s `BaseTest`; the module only adds attachments to the
current step. Without an Allure lifecycle (or without `AllureSelenide`), the same information goes to the SLF4J
log only. `install()` is idempotent, so calling it twice does not duplicate attachments.

### Find tracing

- A Selenium `WebDriverListener` (driver wrapped with `EventFiringDecorator`, or registered through Selenide's
  `WebDriverRunner.addListener` if that applies to custom providers) records `beforeFindElement(s)` /
  `afterFindElement(s)` / `onError` for every find: locator (readable form), start time, duration, outcome
  (found with element id, not found, other error). Element-level finds (`$(parent).$(child)`) are covered too.
- A Selenide `LogEventListener` (registered with `SelenideLogger.addListener`) marks the start and end of each
  Selenide command. The attempts recorded between them belong to that command. When the command ends, a summary
  is attached to the current Allure step and logged: number of attempts, first and last duration, total time,
  final outcome. Long attempt lists are compressed (identical consecutive outcomes collapsed into one line with
  a count).
- The listener has to survive Selenide's driver unwrapping (`selenide-appium` unwraps decorated drivers to reach
  `AppiumDriver`); check that `flutter:` script commands still reach the real driver through the decorator.

### Failure diagnostics

When a Selenide command on a Flutter locator fails:

1. Call `flutter: renderTree` with no arguments (whole tree). Bound the cost: a timeout and a size cap; above the
   cap, attach the truncated tree and say it was truncated.
2. Attach the raw JSON and a readable indented text tree: one line per node with `type`, `key`, `text`,
   `semanticsLabel`, visibility, enabled, rect.
3. Compute near-miss suggestions for the failed locator and attach them as text (also logged):
   - same strategy, similar value: key or text by edit distance and case-insensitive / trimmed / contains
     comparisons; widget type by simple name;
   - exact match that the driver could not hit: node present but not visible, offstage, zero-size rect or
     disabled, with that reason stated;
   - for descendant/ancestor locators, which side of the relation matched and which did not;
   - top N (default 5), ranked, with the reason for each.
4. Never mask the original error (see Anti-goals). The tree is fetched once per failed command.

Ranking and formatting are pure functions over the tree JSON, unit-tested on fixture trees recorded from the test
app.

### q4j page model

Mobile screens implement the existing `Page` interface without `@PageUrl`; URL-based helpers are simply not used.
`PageSteps` / `Verification` work as on the web. No new screen abstraction in this iteration. The device tests in
this module are written in that style, so they double as usage examples in the README.

### Test app (`integrations/selenide-flutter/test-app/`)

A minimal Flutter app built only for these tests, with `appium_flutter_server` and the
`integration_test/appium_test.dart` entry point. Screens, each targeting a case the diagnostics must explain:

- login form: text fields and a button with keys; a disabled button until the form is valid;
- a delayed widget that appears after about 2 s;
- a long lazily built list (item far below the fold, needs `scrollTillVisible`);
- duplicate texts in different places (needs `descendant`);
- a widget with a semantics label but no key;
- a widget that exists but is offstage or covered.

Every widget the tests touch also carries a `Semantics(identifier: ...)` (and, where natural, a semantics label),
so the native path can reach it; the widget with a label but no key stays as is on purpose.

Flutter SDK version pinned in one file used by both the local script and CI.

### Flutter vs native comparison

The owner wants to see whether the Flutter path pays off compared with plain native automation. The device tests
therefore run each scenario in two modes against the same apk:

- **flutter:** `FlutterDriverProvider` (integration driver), `FlutterBy` locators, the visibility layer;
- **native:** a plain UiAutomator2 session (`AndroidDriver`, no Flutter driver), native locators: `AppiumBy.id`
  on the `Semantics(identifier:)` resource-id, `accessibilityId` on semantics labels, `-android uiautomator`
  where needed. On failure Selenide's usual screenshot and native page source (XML) are attached; the same find
  tracing listener records attempts, so attempts and timings are comparable.

Scenarios are written once against a locator table per mode (a page object with a flutter and a native variant
of each locator, or a mode-parameterized page); the TestNG run is parameterized by mode. Scenarios a mode cannot
express are recorded as "not reachable in this mode" rather than skipped silently.

After a run, a comparison summary is produced (attached to Allure and written as a Markdown/HTML file under
`build/`): per scenario and mode, pass/fail, wall time, number of find attempts, and, for the failure demo,
what each mode's diagnostics told you (widget tree + suggestions vs page source XML). The failure demo runs in
both modes so the two reports can be read side by side.

iOS joins the comparison with the iOS iteration (XCUITest native mode with `accessibilityIdentifier`); the mode
abstraction must not assume Android.

### One-command local run

A script under `tools/environment/scripts/flutter/` (or a Gradle task that calls it; builder's choice), runnable
from the repo root:

1. check prerequisites (Flutter, Android SDK, Node, Java) and print what is missing with how to install it;
2. install Appium 3.x and the pinned UiAutomator2 and Flutter integration drivers locally (project-local npm
   prefix, not global), if missing;
3. build the apk if the app sources changed since the last build;
4. boot the configured AVD if no emulator is running (if the AVD does not exist, print the one command to create
   it);
5. start Appium if it is not listening, with its log written under `build/`;
6. run the device test task; a separate command stops what the script started.

### Gradle tasks

- `test`: unit tests only, no device. The module goes into `integrationsBuildModules` in
  `reusable-module-matrices.yml` like the other integrations, so these run on every PR.
- `flutterDeviceTest`: the device scenarios in flutter mode only (separate source set or package filter); needs
  the running emulator and Appium. Expected green.
- `flutterComparison`: the same scenarios in both modes (flutter and native) plus the failure demo in both modes,
  then the comparison summary. The failure demo fails on purpose, so this task's outcome is not a pass/fail
  signal: the summary records "failed as intended" per mode, and the task itself succeeds when every
  non-demo scenario passed and the demo failed in both modes; any other outcome fails the task.
- `flutterFailureDemo`: opt-in, only the failure demo (both modes) for quick local inspection, like
  `allureFailureDemo` in `integrations/selenide`; excluded from `test` and `flutterDeviceTest`.

### CI

A new workflow `flutter-device-tests.yml`:

- triggers: `pull_request` with a `paths` filter (`integrations/selenide-flutter/**`, the workflow file,
  `gradle/libs.versions.toml`), a nightly `schedule`, `workflow_dispatch`;
- `ubuntu-latest`; enable KVM (udev rule); set up Java, Flutter (pinned), Node; install Appium 3.x and the pinned
  drivers; build the apk; run `reactivecircus/android-emulator-runner` (x86_64 image, one API level) whose script
  starts Appium in the background and runs the device task;
- which task (decided with the owner): on `pull_request`, `flutterDeviceTest` (flutter mode only, fast, green);
  on the nightly `schedule` and `workflow_dispatch`, `flutterComparison` (both modes, failure demo, summary);
- always upload Allure results, the Appium log and, for the nightly run, the comparison summary as artifacts;
- not a required check in this iteration.

### Documentation

`integrations/selenide-flutter/README.md`: prerequisites, the one command, how to instrument your own app, a
`FlutterBy` + page-object example, and a screenshot or excerpt of the failure diagnostics. Formatted with
Prettier.

## Builder's choices

- Exact Gradle wiring (source set vs package filter for device tests; script vs Gradle task as the entry point).
- The lowest accepted value for `flutterElementWaitTimeout` (verify against the driver: 0 may mean "default").
- Whether the readable `FlutterBy` wrapper is possible without breaking remoting, or the readable form lives in the
  listener.
- `EventFiringDecorator` vs Selenide's listener registration, whichever keeps both tracing and `flutter:` script
  commands working.
- Similarity metric details, size cap and timeout for the tree fetch.
- How mode-specific locators are expressed (two page variants vs one mode-aware page) and the exact format of the
  comparison summary. The summary is written by `flutterComparison` under the module's `build/` and attached to
  Allure.
- Whether the native mode reuses one Appium server with a second session or restarts the app between modes.
- Emulator API level and system image; AVD name.
- How the module is kept out of Maven Central publishing.

## Risks

- Driver stagnation: last release 2025-10, Appium 4 not covered. Pinned versions contain it; revisit before
  iOS.
- Suspected driver behavior (from source reading, not tested): matches evaluated before the server-side wait, so
  an element that appears during that wait may still be reported missing. Keeping the server-side wait minimal and
  letting Selenide poll avoids depending on it.
- Emulator boot time and flakiness in CI. Mitigated by path filters, nightly runs and not making the job
  required.

## Amendments

Controller rulings made during the build that change the approved design:

- R7: a widget without `state.visible`/`state.enabled` counts as visible and enabled; a missing flag is not
  evidence that the widget is hidden or disabled.
- R9: the WebDriver tracer is registered once globally, the Selenide log listener on every `install()` per thread;
  tests call `install()` in `@BeforeMethod`.
- R12: a test that exposes a known gap of a mode runs and fails for real; it carries the TestNG group
  `known-gap-<mode>`, which `flutterDeviceTest` excludes and `flutterComparison` includes.
- R13: native mode expresses descendant locators with a UiSelector `childSelector`; only combinations UiSelector
  cannot express are not reachable.
- R14: native mode runs a second apk built from `lib/main.dart` without the integration-test binding, instead of
  the same apk, so the comparison does not blame native automation for a harness artifact.
- R15: an exact tree match that the server does not find is reported as "exists in the widget tree but is not
  hit-testable", with hit-blocking ancestors named by widget type; occlusion is not computed.
- R17: the comparison adds the status `KNOWN_GAP` (a known-gap test that fails); a known gap that passes is
  `PASSED` and flagged "known gap did not reproduce"; neither breaks the verdict.
- R18: the native failure-demo excerpt is a compact list of page-source nodes with a resource-id, content-desc or
  text; the verdict requires an excerpt for every failed demo.
- R20: hit-blocking ancestors are worded as a possible cause (their flag is not in the tree); a button keyed
  directly on its widget is compared in both modes to separate driver gaps from keyed-wrapper gaps; the verdict
  requires every failure demo in every mode that ran.
