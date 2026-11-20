# Locators

Find a Flutter widget by key, text, type or semantics label, or relative to another widget, with `FlutterBy`.

Every factory returns a Selenium `By`, so it goes anywhere Selenide takes one:

```java
import com.codeborne.selenide.appium.SelenideAppium;
import dev.quokkify.flutter.FlutterBy;

SelenideAppium.$(FlutterBy.key("login_email")).setValue("user@example.com");
```

The examples below use these imports plus static imports of the Selenide conditions they check
(`import static com.codeborne.selenide.Condition.*;`).

## Factories

| Factory                              | Matches                                                                                |
| ------------------------------------ | -------------------------------------------------------------------------------------- |
| `FlutterBy.key(key)`                 | the widget whose `ValueKey` is `key`                                                   |
| `FlutterBy.text(text)`               | a text widget whose text equals `text`                                                 |
| `FlutterBy.textContaining(text)`     | a text widget whose text contains `text`                                               |
| `FlutterBy.type(type)`               | a widget by runtime type name, for example `"ElevatedButton"`                          |
| `FlutterBy.semanticsLabel(label)`    | a widget whose Semantics label is `label`                                              |
| `FlutterBy.descendant(of, matching)` | a widget matching `matching` somewhere under a widget `of`                             |
| `FlutterBy.ancestor(of, matching)`   | a widget matching `matching` somewhere above a widget `of` (not exercised on a device) |

Each factory wraps the matching `AppiumBy.flutter*` locator of the Appium java-client. The server finds only
widgets that can receive a hit (`hitTestable()`): a widget that is in the tree but offstage is not found.

## By key

```java
SelenideAppium.$(FlutterBy.key("login_password")).setValue("secret1");
```

The most stable choice: a key does not change with copy or language. Put it on the widget the test checks (see
[the keyed-wrapper trap](#the-keyed-wrapper-trap)).

## By text, text containing, semantics label

```java
SelenideAppium.$(FlutterBy.text("Details")).click();          // exact text of a Text widget
SelenideAppium.$(FlutterBy.textContaining("Welcome")).shouldBe(visible);
SelenideAppium.$(FlutterBy.semanticsLabel("Settings icon")).click(); // an IconButton with no text
```

- `text` and `textContaining` match text widgets only. A button's label lives in its child `Text`, not on the
  button.
- `semanticsLabel` is for widgets that show no text, such as an icon button with only an accessibility label
  (`LabelOnlyTest` taps the test app's settings icon this way).
- `text` (by `DuplicatesTest`) and `semanticsLabel` run on a device; `textContaining` is not exercised there.

When you search by text or key and the value exists as a semantics label instead, the failure report says so:
`value found as semantics label; try FlutterBy.semanticsLabel` (see
[Reports and diagnostics](reports-and-diagnostics.md#suggestion-reasons)).

## By type

```java
SelenideAppium.$(FlutterBy.type("ElevatedButton")).shouldBe(enabled);
```

Type alone rarely identifies one widget. Use it as the `matching` part of a descendant locator.

## Descendant and ancestor

Two identical cards each have a `Details` button; tap the one in `card_b`:

```java
SelenideAppium.$(FlutterBy.descendant(FlutterBy.key("card_b"), FlutterBy.text("Details"))).click();
```

Both arguments must be `FlutterBy` locators. Anything else fails at once:

```text
java.lang.IllegalArgumentException: Flutter locator expected but got: By.id: card_b
```

`FlutterBy.ancestor(of, matching)` works the other way round: it finds a widget matching `matching` that contains
the widget `of`. No device test uses it yet; failure suggestions cover it (the reasons get an `of: ` or
`matching: ` prefix).

## The keyed-wrapper trap

What you write:

```dart
KeyedSubtree(key: ValueKey("login_button"), child: ElevatedButton(...))
```

```java
SelenideAppium.$(FlutterBy.key("login_button")).shouldBe(disabled); // passes whatever the form state is
```

What you get: the key sits on the `KeyedSubtree`, and for that element the Flutter server returns the text `""` and
`enabled=false`. `shouldBe(disabled)` always passes and `shouldBe(enabled)` never does. This is verified for
`KeyedSubtree`, the wrapper the test app's `Tagged` creates; other wrappers that carry the key likely behave the
same, but that is not verified.

Fix it in the app, by putting the key on the button itself:

```dart
ElevatedButton(key: const ValueKey("login_button"), ...)
```

Or fix it in the test, by targeting the widget under the key:

```java
SelenideAppium.$(FlutterBy.descendant(FlutterBy.key("login_button"), FlutterBy.type("ElevatedButton")))
    .shouldBe(enabled);
SelenideAppium.$(FlutterBy.descendant(FlutterBy.key("login_welcome"), FlutterBy.type("Text")))
    .shouldHave(exactText("Welcome, user@example.com"));
```

The test app's `Tagged` wrapper produces this shape on purpose, so the device tests keep both variants:
`LoginTest.keyedButtonFollowsFormState` is a known gap, `LoginTest.directlyKeyedButtonFollowsFormState` passes.

## Mixing with native locators

`FlutterBy` drives the Flutter driver; `AppiumBy.id`, `AppiumBy.accessibilityId` and `AppiumBy.androidUIAutomator`
drive UiAutomator2. To run one scenario under both drivers, choose the locator by mode in one place, as the device
tests' [`ModeLocators`](../../integrations/selenide-flutter/src/deviceTest/java/dev/quokkify/flutter/device/ModeLocators.java) does (condensed):

```java
public By id(String id) {
  return mode.isFlutter() ? FlutterBy.key(id) : AppiumBy.id(id);
}

public By label(String label) {
  return mode.isFlutter() ? FlutterBy.semanticsLabel(label) : AppiumBy.accessibilityId(label);
}
```

Failure diagnostics cover Flutter locators only. For any other locator the step still gets
`Flutter: element search`, but no widget tree and no suggestions.
