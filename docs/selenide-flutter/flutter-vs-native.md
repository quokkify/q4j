# Flutter vs native

Choose how to drive a Flutter app from the measured differences between three modes on the same scenarios.

## The three modes

| Mode           | Driver and apk                                           | Who waits for an element                                      |
| -------------- | -------------------------------------------------------- | ------------------------------------------------------------- |
| `flutter`      | Flutter integration driver, apk with the server          | Selenide polls; the server checks once per find (the default) |
| `flutter-sync` | the same driver and apk, `flutterElementWaitTimeout` 5 s | the server blocks up to 5 s per find; Selenide polls on top   |
| `native`       | UiAutomator2, plain apk without the server               | Selenide polls the accessibility tree                         |

The native apk is built from `lib/main.dart` without the server's test binding, which mocks the platform text
input: with the same apk, native typing would fail for a harness reason. [Running](running.md#comparison-output)
shows how to reproduce the comparison.

## Results

`flutterComparison`, Android 14 emulator, the 7 scenarios that passed in all three modes:

| Mode           | Total  | Finds | ms per find (scenario ms / finds) |
| -------------- | ------ | ----- | --------------------------------- |
| `flutter`      | 62.8 s | 64    | 981                               |
| `native`       | 64.8 s | 77    | 841                               |
| `flutter-sync` | 79.8 s | 48    | 1663                              |

Scenario time includes taps, typing and screen changes, so "ms per find" is an upper bound, not the cost of a
find. `flutter` and `native` are within 2 s of each other; `flutter-sync` loses about 17 s, mostly on misses that
each wait the full 5 s ([details](waiting-and-conditions.md#server-side-waiting-flutter-sync)).

On a failure the difference is larger than the timing. The same mistyped key `login_btn`:

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

The flutter side names the near miss and the reason. The native side (a compact excerpt of the page source that
the comparison builds) lists what is on screen and leaves the matching to you.

## When to use which

- **`flutter`** for Flutter apps you can instrument: widget-level locators, a per-attempt trace, suggestions on a
  failed find, and fast negative checks. Speed is on par with native.
- **`native`** when you cannot test a build with the server inside, such as a release build. Expect to add
  Semantics identifiers and to work around the gaps below.
- **`flutter-sync`** not by default: on this app it gave no speed gain, slow misses, and it inherits the server's
  stale-result bug. It is a device-test mode only (`FlutterSyncDriverProvider` in `src/deviceTest`); the module
  ships `flutter` behaviour.

## Known gaps per mode

Flutter modes (`flutter`, `flutter-sync`):

- A key on a wrapper (`KeyedSubtree`) reads the text `""` and `enabled=false`. Fix: key the widget itself or use a
  descendant locator ([details](locators.md#the-keyed-wrapper-trap)).
- The server reads text only from text widgets: a keyed `ElevatedButton` reports `""`.
- The widget tree has no visible or enabled flags and only local rects, so suggestions name hiding ancestors by type
  and do not compute occlusion.
- `ScrollParameter` sends `dragDuration` in seconds; use a raw `scrollTillVisible` argument map.

Native mode:

- The resource-id has no package prefix: `AppiumBy.id` needs the `appium:disableIdLocatorAutocompletion`
  capability.
- Texts are exposed as `content-desc`; `text` stays empty.
- Flutter applies an accessibility `setText` only to a focused field: tap the field before typing.
- A widget wrapped with a Semantics identifier keeps a wrapper node with the resource-id that is always enabled;
  the `Button` with the real state is its child. `MergeSemantics` folds the id into the button node.
- A widget under an opaque cover is reported as displayed: the accessibility tree knows nothing of occlusion.

The full list, as observed on the device, is in
[`test-app/README.md`](../../integrations/selenide-flutter/test-app/README.md#mode-differences-observed-on-the-device).
