# selenide-flutter test app

Flutter app driven by the `selenide-flutter` device tests.

## Origin

Derived from `demo-app/` of
[AppiumTestDistribution/appium-flutter-server](https://github.com/AppiumTestDistribution/appium-flutter-server)
at commit `546ce1c45196e7904e8e6ab9fa663f96d4f51f56`, under the MIT license kept in [LICENSE](LICENSE).

Changes from the original:

- removed every plugin dependency and the screens using them, the screens the tests do not need, the widget test,
  and the `ios/` and `macos/` folders;
- migrated the Android build to the Flutter 3.50 Gradle template and dropped the permissions the removed screens
  needed;
- the home screen is a menu; the login, loader and lazy loading screens were reworked into the login, delayed and
  long list cases; the duplicates, label only and offstage screens were added;
- widgets the tests use are wrapped in `Tagged` (`lib/tagged.dart`): a `ValueKey` and a Semantics identifier with
  the same string, exposed on Android as the resource-id.

## Widget ids

| Screen     | Ids                                                                                         |
| ---------- | ------------------------------------------------------------------------------------------- |
| Home       | `home_login`, `home_delayed`, `home_list`, `home_duplicates`, `home_label`, `home_offstage` |
| Login      | `login_email`, `login_password`, `login_button`, `login_welcome`                            |
| Delayed    | `delayed_load`, `delayed_result`                                                            |
| Long list  | `list_view`, `list_item_0` … `list_item_99`, `list_selected`                                |
| Duplicates | `card_a`, `card_b`, `details_result`                                                        |
| Label only | `label_result` (the settings `IconButton` has only the label `Settings icon`)               |
| Offstage   | `hidden_banner` (offstage), `covered_button` (under an opaque cover)                        |

## Mode differences observed on the device

Native UiAutomator2 (Android 14 emulator, native apk):

- the resource-id has no package prefix: `AppiumBy.id` needs the `disableIdLocatorAutocompletion` capability;
- a text field merges into one `EditText` that carries the resource-id; Flutter applies an accessibility `setText`
  only to a focused field, so tap the field before typing;
- a button keeps a wrapper node with the resource-id that is always enabled; the `Button` with the real `enabled` state
  is its child;
- a list tile merges into one `Button` with the resource-id;
- texts are exposed as `content-desc`; `text` stays empty;
- an offstage widget is absent from the tree, but a widget under an opaque cover is reported as displayed: the
  accessibility tree has no notion of occlusion.

Flutter integration driver (flutter apk):

- the key sits on the `KeyedSubtree` that `Tagged` creates; for that element the Flutter server returns the text `""`
  and `enabled=false`, so `$(FlutterBy.key("login_button")).shouldBe(disabled)` passes whatever the form state is;
  target the `Text` or `ElevatedButton` under the key instead;
- java-client's `ScrollParameter` sends `dragDuration` in whole seconds while the server reads milliseconds, so a
  sub-second drag needs a raw `flutter: scrollTillVisible` argument map;
- the widget tree has no visible/enabled state and only local rects, so failure suggestions detect `Offstage`,
  `Visibility`, `Opacity`, `IgnorePointer` and `AbsorbPointer` ancestors by type; occlusion of `covered_button` by the
  cover is not computed.

## Build

From the repository root:

```bash
tools/environment/scripts/flutter/build_app.sh
```

It builds two debug apks:

- `build/app/outputs/flutter-apk/app-debug.apk` (flutter mode, `FLUTTER_APP_PATH`) with the entry point
  `integration_test/appium_test.dart`, which starts the Appium Flutter server inside the app;
- `build/native/app-native-debug.apk` (native mode, `FLUTTER_NATIVE_APP_PATH`) from `lib/main.dart`. The server's
  test binding mocks the platform text input, so native typing only works without it.

Both apks have the same package and version; the device tests reinstall the apk whenever the mode switches. The Flutter version is pinned in `pubspec.yaml`
(`environment.flutter`).
