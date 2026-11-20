# Flutter vs native

Choose how to drive a Flutter app from the measured differences between three modes on the same scenarios.

This page is the one home of the comparison numbers; other pages link here.

## The three modes

| Mode           | Driver and apk                                           | Who waits for an element                                      |
| -------------- | -------------------------------------------------------- | ------------------------------------------------------------- |
| `flutter`      | Flutter integration driver, apk with the server          | Selenide polls; the server checks once per find (the default) |
| `flutter-sync` | the same driver and apk, `flutterElementWaitTimeout` 5 s | the server blocks up to 5 s per find; Selenide polls on top   |
| `native`       | UiAutomator2, plain apk without the server               | Selenide polls the accessibility tree                         |

`flutter-sync` is a device-test mode only; its provider is shown in
[Waiting and conditions](waiting-and-conditions.md#server-side-waiting-flutter-sync). The native apk is built from
`lib/main.dart` without the server's test binding, which mocks the platform text input: with the same apk, native
typing would fail for a harness reason. [Running](running.md#comparison-output) shows how to reproduce the
comparison.

## Results

`flutterComparison`, Android 14 emulator, 2026-11-20, the 7 scenarios that passed in all three modes:

| Mode           | Total  | Finds | ms per find (scenario ms / finds) |
| -------------- | ------ | ----- | --------------------------------- |
| `flutter`      | 62.8 s | 64    | 981                               |
| `native`       | 64.8 s | 77    | 841                               |
| `flutter-sync` | 79.8 s | 48    | 1663                              |

Scenario time includes taps, typing and screen changes, so "ms per find" is an upper bound, not the cost of a
find. What the traces show:

- **Hits cost the same in both flutter modes**, 220–400 ms: the server's wait loop pumps frames (`pumpAndSettle`,
  up to 200 ms) and sleeps 100 ms before it checks, whatever the timeout. Server-side waiting does not make a
  present widget faster to find.
- **A widget that appears later is found at the same moment** in both flutter modes (`DelayedWidgetTest`: 2.35 s
  vs 2.38 s for the step): two attempts (one blocking, then a retry) instead of four or five polled ones.
- **Every miss in `flutter-sync` costs the full server wait** (5 s). Negative checks pay it: `shouldNotBe(visible)`
  on an offstage widget takes 5.1 s instead of 0.35 s, and the `is(visible, 2s)` probe that `HomeScreen.reach` uses
  before pressing back takes 5.2 s instead of 2.1 s. Most of the 17 s that `flutter-sync` loses goes there; the rest
  (an estimated 0.4–1.1 s per test class) is TestNG running `flutter-sync` first in each class, right after
  `native`, so its session reinstalls the flutter apk.
- **Server bug, reproduced.** The server evaluates the finder once before its wait and throws
  `Unable to locate element` from that stale result even when the widget appeared during the wait. In
  `DelayedWidgetTest` the first `flutter-sync` attempt blocked 1.9 s, until the result showed, and still reported
  it missing; Selenide's retry found it in 0.2 s. With Selenide polling on top the scenario passes; a bare
  `driver.findElement` would fail.

Frame synchronisation, which made the old `flutter_driver` feel fast, is already part of both flutter modes: the
integration server settles frames after every tap and around every find. It does not guarantee a fresh find: the
finder is evaluated before the settle, and that earlier result decides found or missing.

## Failures side by side

On a failure the difference is larger than the timing. The native side is the compact page-source excerpt the
comparison builds; the flutter side is `Flutter: suggestions`.

`FailureDemoTest.tapsMistypedKey` (looks up `login_btn`):

```text
flutter
0.75 KeyedSubtree key="login_button" @0,0 363x48 — similar key (distance 3)
0.55 KeyedSubtree key="login_email" @0,0 363x24 — similar key (distance 5)

native
FrameLayout id="android:id/content" [0,0][1080,2337]
Button desc="Back" [11,139][137,265]
View desc="Login" [189,165][331,238]
View desc="Please Login" [63,359][343,427]
EditText id="login_email" [63,480][1017,543]
EditText id="login_password" [63,622][1017,685]
View id="login_button" [63,737][1017,863]
Button desc="Login" [63,737][1017,863]
Button id="direct_button" desc="Direct login" [63,916][1017,1042]
View id="android:id/navigationBarBackground" [0,2337][1080,2400]
```

`FailureDemoTest.tapsOffstageBanner` (the widget exists but is offstage):

```text
flutter
1.00 KeyedSubtree key="hidden_banner" @0,0 94x20 — exact key match exists in the widget tree but is not hit-testable; has an Offstage ancestor that may make it non-hit-testable (widget type only; its flag is not in the tree)

native
FrameLayout id="android:id/content" [0,0][1080,2337]
Button desc="Back" [11,139][137,265]
View desc="Offstage" [189,165][412,238]
View id="covered_button" [408,1254][672,1380]
Button desc="Covered" [408,1254][672,1380]
View id="android:id/navigationBarBackground" [0,2337][1080,2400]
```

The flutter side names the near miss and the reason. The native side lists what is on screen and leaves the
matching to you.

## When to use which

- **`flutter`** for Flutter apps you can instrument: widget-level locators, a per-attempt trace, suggestions on a
  failed find, and fast negative checks. Total time is within 2 s of native on these scenarios.
- **`native`** when you cannot test a build with the server inside, such as a release build. Expect to add
  Semantics identifiers and to work around the gaps below.
- **`flutter-sync`** not by default: on this app it gave no speed gain, slow misses, and it inherits the server's
  stale-result bug. The module ships `flutter` behaviour (`FLUTTER_DRIVER_FIND_TIMEOUT_MS=1`).

## Known gaps per mode

Flutter modes (`flutter`, `flutter-sync`):

- A key on a `KeyedSubtree` reads the text `""` and `enabled=false`. Fix: key the widget itself or use a descendant
  locator ([details](locators.md#the-keyed-wrapper-trap)).
- The server reads text only from text widgets: a keyed `ElevatedButton` reports `""`.
- The widget tree has no visible or enabled flags and only local rects, so suggestions name hiding ancestors by
  type and do not compute occlusion.
- `ScrollParameter` sends `dragDuration` in seconds; use a raw `scrollTillVisible` argument map.

Native mode:

- The resource-id has no package prefix: `AppiumBy.id` needs the `appium:disableIdLocatorAutocompletion`
  capability.
- Texts are exposed as `content-desc`; `text` stays empty.
- Flutter applies an accessibility `setText` only to a focused field: tap the field before typing.
- A widget wrapped by `Tagged` keeps a wrapper node with the resource-id that is always enabled; the `Button` with
  the real state is its child. `MergeSemantics` folds the id into the button node.
- A widget under an opaque cover is reported as displayed: the accessibility tree knows nothing of occlusion.

The full list, as observed on the device, is in
[`test-app/README.md`](../../integrations/selenide-flutter/test-app/README.md#mode-differences-observed-on-the-device).
