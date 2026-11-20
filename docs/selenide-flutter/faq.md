# FAQ

Short answers to the questions people ask first, each with a link to the full story.

## Why is my button always disabled?

The key is probably on a wrapper, not on the button. For a `KeyedSubtree` (or any widget that wraps the button
with the key) the Flutter server reports `enabled=false` and the text `""`. Put the key on the `ElevatedButton`, or
use `FlutterBy.descendant(FlutterBy.key("pay"), FlutterBy.type("ElevatedButton"))`. See
[the keyed-wrapper trap](locators.md#the-keyed-wrapper-trap).

## Why is the button's text empty?

The server reads text only from text widgets. Check the label through its `Text`: `FlutterBy.text("Pay")`, or a
descendant locator with `FlutterBy.type("Text")`.

## The widget is on screen, why "not found"?

The server finds only widgets that can receive a hit. An offstage widget, or one under an `IgnorePointer`, is in
the tree but not found. `Flutter: suggestions` then lists it with score 1.00 and
`exact key match exists in the widget tree but is not hit-testable`
([all reasons](reports-and-diagnostics.md#suggestion-reasons)).

## Why Appium 3?

`appium-flutter-integration-driver` 2.0.3 declares `appium ^3`. The pins live in
`tools/environment/scripts/flutter/versions.env` ([Running](running.md#pinned-versions)).

## Can I use it with iOS?

Not yet. `FlutterDriverProvider` creates a `FlutterAndroidDriver` with `platformName` `Android`, and the scripts
and CI boot only an Android emulator.

## Can I test a release build?

No. The driver talks to `appium_flutter_server` inside the app, and only a debug or profile build built from
`integration_test/appium_test.dart` has it. Use a native driver for a release build
([Flutter vs native](flutter-vs-native.md#when-to-use-which)).

## Why is `flutter-sync` slower?

Every miss waits the full server timeout (5 s in the device tests), and negative checks such as
`shouldNotBe(visible)` are made of misses: 5.1 s instead of 0.35 s. Hits cost the same in both modes. Over 7
scenarios: 79.8 s vs 62.8 s ([numbers](waiting-and-conditions.md#server-side-waiting-flutter-sync)).

## Why does `FLUTTER_DRIVER_FIND_TIMEOUT_MS=0` fail?

With `0` the server's wait loop times out before it evaluates the finder, so nothing is ever found. The provider
rejects values below `1` when the driver starts ([Configuration](configuration.md#keys)).

## Where is the widget tree when the app tree is deep?

A whole app tree can exceed the java-client JSON nesting limit of 255. Diagnostics then attach the subtree of the
topmost `Scaffold`, and the text tree starts with
`subtree of the topmost Scaffold: full tree exceeded the JSON nesting limit`
([Big and deep trees](reports-and-diagnostics.md#big-and-deep-trees)).

## Why no attachments on my failed step?

Check three things: `FlutterVisibility.install()` ran on the test thread before the driver started (otherwise the
log has a warning), `AllureSelenide` is registered, and the step failed because no element was found. A step whose
element was found but whose condition failed gets only `Flutter: element search`.

## Does it work in parallel?

The module is built for it: `install()` and the trace are per thread, and the driver tracer is registered once for
all threads. Each thread needs its own device and Appium session. The device tests themselves run serially on one
emulator (`maxParallelForks = 1`), so parallel runs are not exercised here.

## Can I send the diagnostics somewhere other than Allure?

Yes. Implement `DiagnosticsSink` (one method, `attach(name, mimeType, content)`) and pass it to
`FlutterVisibility.install(FailureDiagnostics, List<DiagnosticsSink>)`
([Install and uninstall](reports-and-diagnostics.md#install-and-uninstall)).
