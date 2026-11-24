# Waiting and conditions

Let Selenide do the waiting, know which conditions read real widget state, and know what server-side waiting
changes.

## Selenide waits, the server checks once

What you write:

```java
import static com.codeborne.selenide.Condition.exactText;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.appium.SelenideAppium;
import dev.quokkify.flutter.FlutterBy;

Configuration.timeout = 10_000;

SelenideAppium.$(FlutterBy.key("delayed_load")).click();
SelenideAppium.$(FlutterBy.descendant(FlutterBy.key("delayed_result"), FlutterBy.type("Text")))
    .shouldHave(exactText("Loaded"));
```

What you get: Selenide polls the find until the widget shows up or `Configuration.timeout` runs out, as it does in
a browser. Each poll is one find on the Flutter server, and each find is traced
([Reports and diagnostics](reports-and-diagnostics.md)).

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

## Conditions

| Condition               | Works on a Flutter element                                                                         |
| ----------------------- | -------------------------------------------------------------------------------------------------- |
| `visible`               | yes; an offstage widget, or one under an opaque cover, is not visible (verified by `OffstageTest`) |
| `appear`, `disappear`   | likely, as Selenide aliases of `visible` and `hidden`; not covered by a device test                |
| `enabled`, `disabled`   | yes, on the widget that carries the state, not on a keyed wrapper (verified by `LoginTest`)        |
| `text`, `exactText`     | yes, on a text widget; any other widget reports `""` (`exactText` verified by `LoginTest`)         |
| `click()`, `setValue()` | yes (verified by `LoginTest`)                                                                      |

Two known gaps, both from the Flutter server, not from Selenide:

- **Text only from text widgets.** `$(FlutterBy.key("direct_button"))` reports `""` although the button shows
  `Direct login`. Check a label through its `Text`: `FlutterBy.text("Direct login")`, or
  `FlutterBy.descendant(FlutterBy.key("direct_button"), FlutterBy.type("Text"))`.
- **`enabled=false` on wrappers.** A key on a `KeyedSubtree` reads `enabled=false` and the text `""` forever. See
  [the keyed-wrapper trap](locators.md#the-keyed-wrapper-trap).

## Server-side waiting (`flutter-sync`)

You can move the waiting to the server instead: give `flutterElementWaitTimeout` a real value and each find blocks
until the widget exists. The device tests call this mode `flutter-sync` and start it with their own provider
(`src/deviceTest`, not part of the module's main code):

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

What changes, in short:

- A widget that is already there is not found any faster.
- A widget that appears later is found at the same moment, in fewer, longer attempts.
- Every miss costs the full server wait, so negative checks such as `shouldNotBe(visible)` get slow.
- The server can report a widget missing from a stale result even when it appeared during the wait; Selenide's
  next attempt then finds it.

The measurements behind each point are in [Flutter vs native](flutter-vs-native.md#results). Keep the default
unless you have a reason not to.

## Scrolling a lazy list

A `ListView.builder` builds only the items near the viewport, so an item far down does not exist until you scroll.
Ask the server to scroll until it is visible, then find it (from the device tests' `LongListScreen`):

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
