# Waiting and conditions

Let Selenide do the waiting, know which conditions read real widget state, and know what server-side waiting
costs.

## Selenide waits, the server checks once

What you write:

```java
Configuration.timeout = 10_000;

SelenideAppium.$(FlutterBy.key("delayed_load")).click();
SelenideAppium.$(FlutterBy.descendant(FlutterBy.key("delayed_result"), FlutterBy.type("Text")))
    .shouldHave(exactText("Loaded"));
```

What you get: Selenide polls the find until the widget shows up or `Configuration.timeout` runs out, as it does in
a browser. Each poll is one find on the Flutter server, and each find is traced (see
[Reports and diagnostics](reports-and-diagnostics.md)).

The step's `Flutter: element search` attachment from a real run:

```text
4 find attempt(s), total 1748 ms, result FOUND
  3× flutter descendant(of: flutter key "delayed_result", matching: flutter type "Text") → NOT_FOUND (avg 471 ms)
  1× flutter descendant(of: flutter key "delayed_result", matching: flutter type "Text") → FOUND (avg 334 ms)
```

The same step in native mode, polling the accessibility tree:

```text
7 find attempt(s), total 1303 ms, result FOUND
  6× id "delayed_result" → NOT_FOUND (avg 160 ms)
  1× id "delayed_result" → FOUND (avg 340 ms)
```

The server has a wait of its own, `flutterElementWaitTimeout`. `FlutterDriverProvider` sets it from
`FLUTTER_DRIVER_FIND_TIMEOUT_MS`, default `1` ms, so the server checks once per find and Selenide owns the timeout.
Values below `1` are rejected when the driver starts:

```text
java.lang.IllegalArgumentException: FLUTTER_DRIVER_FIND_TIMEOUT_MS must be at least 1 but was 0: with 0 the Flutter server times out before it evaluates the finder, so no element is ever found
```

A find that hits costs 220–400 ms whatever the timeout: the server's loop pumps frames (`pumpAndSettle`, up to
200 ms) and sleeps 100 ms before it checks.

## Conditions

| Condition                        | Works on a Flutter element                                                          |
| -------------------------------- | ----------------------------------------------------------------------------------- |
| `visible`, `appear`, `disappear` | yes; an offstage widget, or one under an opaque cover, is not found and not visible |
| `enabled`, `disabled`            | yes, on the widget that carries the state (not on a keyed wrapper)                  |
| `text`, `exactText`              | yes, on a text widget; any other widget reports `""`                                |
| `click()`, `setValue()`          | yes                                                                                 |

Two known gaps, both from the Flutter server, not from Selenide:

- **Text only from text widgets.** `$(FlutterBy.key("direct_button"))` reports `""` although the button shows
  `Direct login`. Check a label through its `Text`: `FlutterBy.text("Direct login")`, or
  `FlutterBy.descendant(FlutterBy.key("direct_button"), FlutterBy.type("Text"))`.
- **`enabled=false` on wrappers.** A key on a `KeyedSubtree` reads `enabled=false` and the text `""` forever. See
  [the keyed-wrapper trap](locators.md#the-keyed-wrapper-trap).

## Server-side waiting (`flutter-sync`)

You can move the waiting to the server instead: give `flutterElementWaitTimeout` a real value and each find blocks
until the widget exists. The device tests call this mode `flutter-sync`:

```java
public class FlutterSyncDriverProvider extends FlutterDriverProvider {

  static final Duration SERVER_FIND_WAIT = Duration.ofSeconds(5);

  @Override
  public WebDriver createDriver(Capabilities capabilities) {
    return super.createDriver(new MutableCapabilities(capabilities).merge(new FlutterDriverOptions()
        .setFlutterElementWaitTimeout(SERVER_FIND_WAIT)));
  }
}
```

Measured on an Android 14 emulator over the 7 scenarios that passed in every mode:

| Mode           | Total  | Finds | ms per find |
| -------------- | ------ | ----- | ----------- |
| `flutter`      | 62.8 s | 64    | 981         |
| `flutter-sync` | 79.8 s | 48    | 1663        |

What the numbers mean:

- **No faster hits.** A widget that is already there costs the same 220–400 ms in both modes.
- **Same arrival time.** A widget that appears 2 s after a tap is found at the same moment (`DelayedWidgetTest`:
  2.35 s vs 2.38 s for the step), in two attempts instead of four or five.
- **Slow misses.** Every miss costs the full server wait. `shouldNotBe(visible)` on an offstage widget takes 5.1 s
  instead of 0.35 s; an `is(visible, Duration.ofSeconds(2))` probe takes 5.2 s instead of 2.1 s.

**Stale result, reproduced.** The server evaluates the finder once before its wait and reports
`Unable to locate element` from that stale result, even when the widget appeared during the wait. In
`DelayedWidgetTest` the first `flutter-sync` attempt blocked 1.9 s, until the result showed, and still reported it
missing; Selenide's retry found it in 0.2 s. With Selenide on top the scenario passes; a bare
`driver.findElement(...)` would fail.

Keep the default unless you have a reason not to. [Flutter vs native](flutter-vs-native.md) compares all three
modes.

## Scrolling a lazy list

A `ListView.builder` builds only the items near the viewport, so an item far down does not exist until you scroll.
Ask the server to scroll until it is visible, then find it:

```java
((CanExecuteFlutterScripts) WebDriverRunner.getWebDriver()).executeFlutterCommand("scrollTillVisible", Map.of(
    "finder", Map.of("using", "-flutter key", "value", "list_item_80"),
    "delta", 300,
    "dragDuration", 600,
    "maxScrolls", 30));
SelenideAppium.$(FlutterBy.key("list_item_80")).click();
```

Pass a raw argument map. java-client's `ScrollParameter` sends `dragDuration` in whole seconds while the server
reads milliseconds, so a sub-second drag needs the map. Keep the drag slow (600 ms here): the server's default
100 ms drag flings the list past the item between two visibility checks.
