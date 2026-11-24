# Reports and diagnostics

When a Flutter find fails, the Allure step and the log show the find attempts, the widget tree and the closest
widgets, each with the reason it did not match.

## Install and uninstall

```java
@BeforeMethod
public void installListeners() {
  SelenideLogger.addListener("AllureSelenide", new AllureSelenide().screenshots(true).savePageSource(true));
  FlutterVisibility.install();
}

@AfterMethod(alwaysRun = true)
public void removeListeners() {
  FlutterVisibility.uninstall();
  SelenideLogger.removeListener("AllureSelenide");
}
```

| Rule                                  | Why                                                                                                     |
| ------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| Call `install()` on every test thread | Selenide log listeners are per thread; `@BeforeMethod` runs on the test's thread.                       |
| Call it before the first driver       | The find tracer wraps only drivers created after its one global registration with `WebDriverRunner`.    |
| Register `AllureSelenide` yourself    | `install()` does not; without it there is no Allure step to attach to.                                  |
| Calling it twice is fine              | A repeated `install()` on a thread replaces the configuration; the latest call wins.                    |
| Call `uninstall()` on the same thread | It removes the listener and the thread's trace; the global tracer stays registered and records nothing. |

If the first `install()` happens after a driver started, the log says:

```text
FlutterVisibility installed after the WebDriver started; find attempts of that driver are not traced. Call FlutterVisibility.install() before the first driver is created.
```

`install()` uses the defaults: a 10 s limit for reading the tree, a 2,000,000-character cap, and the Allure and log
sinks. Pass your own with `install(FailureDiagnostics, List<DiagnosticsSink>)`:

```java
FlutterVisibility.install(
    new FailureDiagnostics(Duration.ofSeconds(5), 500_000),
    List.of(new AllureSink(), new LogSink(), (name, mimeType, content) -> myReport.add(name, content)));
```

`DiagnosticsSink` has one method, `attach(String name, String mimeType, String content)`.

## Attachments

| Attachment                    | When                                    | Content                                               |
| ----------------------------- | --------------------------------------- | ----------------------------------------------------- |
| `Flutter: element search`     | every step that searched, passed or not | each find attempt and its outcome                     |
| `Flutter: widget tree (json)` | the step failed with no element found   | the raw `flutter: renderTree` result                  |
| `Flutter: widget tree`        | same                                    | the tree as indented text                             |
| `Flutter: suggestions`        | same, when any widget scored            | up to 5 closest widgets, best first, with the reasons |
| `Flutter: diagnostics error`  | the tree could not be read or parsed    | the exception class and message                       |

Only a failed search is diagnosed. When the element was found and a condition failed (say `shouldBe(enabled)`),
the step gets `Flutter: element search` and nothing else: a tree would explain a search that succeeded.

### `Flutter: element search`

Repeated attempts with the same locator and outcome collapse into one line with their average.

`FailureDemoTest.tapsMistypedKey`, step `$("AppiumBy.flutterKey: login_btn") click()`, real run:

```text
23 find attempt(s), total 5409 ms, result NOT_FOUND
  23× flutter key "login_btn" → NOT_FOUND (avg 235 ms)
```

`DelayedWidgetTest`: Selenide polls until the widget appears, real run:

```text
4 find attempt(s), total 1748 ms, result FOUND
  3× flutter descendant(of: flutter key "delayed_result", matching: flutter type "Text") → NOT_FOUND (avg 471 ms)
  1× flutter descendant(of: flutter key "delayed_result", matching: flutter type "Text") → FOUND (avg 334 ms)
```

Outcomes are `FOUND`, `NOT_FOUND` and `ERROR`; an `ERROR` line ends with `— <first line of the error>`.

### `Flutter: widget tree`

One line per widget, two spaces of indent per depth: the type, then `key`, `text` and `label` when set, then the
rect `@x,y widthxheight`. A widget without a size gets `[zero-size]`. The formatter also has `[hidden]` and
`[disabled]` markers, but the real `renderTree` carries no visible or enabled flags, so they do not appear in trees
from the Flutter server.

```text
KeyedSubtree key="login_button" @0,0 363x48
```

Rects are local to the parent: the server sends no absolute coordinates.

### `Flutter: suggestions`

`FailureDemoTest.tapsMistypedKey` (looks up `login_btn`), real run:

```text
0.75 KeyedSubtree key="login_button" @0,0 363x48 — similar key (distance 3)
0.55 KeyedSubtree key="login_email" @0,0 363x24 — similar key (distance 5)
```

`FailureDemoTest.tapsOffstageBanner` (the widget exists but is offstage), real run:

```text
1.00 KeyedSubtree key="hidden_banner" @0,0 94x20 — exact key match exists in the widget tree but is not hit-testable; has an Offstage ancestor that may make it non-hit-testable (widget type only; its flag is not in the tree)
```

Each line reads `score widget — reason; reason`.

## Suggestion reasons

Every widget in the tree is compared with the locator's value on the matching field (key, text, semantics label or
type). The top 5 by score are shown.

| Score         | Reason                                                                                                    | Meaning                                                                                                   |
| ------------- | --------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| 1.00          | `exact <field> match exists in the widget tree but is not hit-testable`                                   | the value is right; the server's hit test rejected the widget                                             |
| added to 1.00 | `has a <Type> ancestor that may make it non-hit-testable (widget type only; its flag is not in the tree)` | an `Offstage`, `Visibility`, `Opacity`, `IgnorePointer` or `AbsorbPointer` ancestor (`an` before a vowel) |
| added to 1.00 | `zero-size`                                                                                               | the widget has no rect, or a zero width or height                                                         |
| added to 1.00 | `hidden`, `disabled`                                                                                      | the tree flags the widget so; the real `renderTree` carries no such flags                                 |
| 0.95          | `differs only in case`                                                                                    | `Login` vs `login`                                                                                        |
| 0.90          | `differs only in whitespace`                                                                              | leading or trailing spaces                                                                                |
| 0.80          | `value found as semantics label; try FlutterBy.semanticsLabel`                                            | a key or text locator whose value is a widget's semantics label                                           |
| 0.70          | `partial match`                                                                                           | one value contains the other                                                                              |
| 0.50 or more  | `similar <field> (distance <n>)`                                                                          | Levenshtein; score = 1 − distance / length of the longer value                                            |

`<field>` is `key`, `text`, `semantics label` or `type`. For a descendant or ancestor locator each reason gets a
prefix: `of: ` while the anchor itself is not found, `matching: ` once it is.

Two limits follow from what the server sends:

- An ancestor's offstage, visible, opacity or ignoring flag is not in the tree, so the ancestor is named as a
  possible cause only.
- Occlusion by another widget is not computed.

When one widget's text is copied onto every wrapper above it, only the deepest exact match is listed.

## Big and deep trees

- **Size cap.** JSON or text over the cap (2,000,000 characters by default) is cut and ends with `…[truncated]`;
  truncated JSON is attached as `text/plain`.
- **Deep trees.** A whole app tree can exceed the java-client JSON nesting limit (255). Diagnostics then read the
  subtree of the topmost `Scaffold`, and the text tree starts with
  `subtree of the topmost Scaffold: full tree exceeded the JSON nesting limit`. An app with no `Scaffold` gets a
  `Flutter: diagnostics error` instead.
- **Slow trees.** Reading the tree is limited to 10 s by default; past it the error attachment says
  `TimeoutException: flutter: renderTree did not finish within 10000 ms`.

## What goes to the log

`LogSink` writes through SLF4J, logger `dev.quokkify.flutter.report.LogSink`:

| Attachment                    | Level | Logged as                                                           |
| ----------------------------- | ----- | ------------------------------------------------------------------- |
| `Flutter: element search`     | INFO  | the full summary                                                    |
| `Flutter: widget tree (json)` | INFO  | `<name>: JSON omitted from the log; see the test report attachment` |
| every other attachment        | WARN  | the full content                                                    |

## Guarantees

- **Your failure stays your failure.** Diagnostics run after the Selenide step and never throw. A failure while
  reporting is logged as a WARN (`Flutter visibility reporting failed`), and the test reports the original Selenide
  error.
- **One sink cannot break another.** A sink that throws is logged and skipped; the other sinks still get the
  attachment.
- **No running test, no Allure attachment.** `AllureSink` writes only while an Allure test or step is running.
