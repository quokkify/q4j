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

Observed with native UiAutomator2 (Android 14 emulator): the resource-id has no package prefix (use the
`disableIdLocatorAutocompletion` capability for `AppiumBy.id`); a text field merges into one `EditText` that carries the
resource-id; a button keeps a wrapper node with the resource-id and the `Button` with the real `enabled` state as its
child; a list tile merges into one `Button` with the resource-id; texts are exposed as `content-desc`, `text` stays empty.
The app runs under the Appium Flutter server's test binding, which mocks the platform text input, so native typing never
reaches a `TextField`. The accessibility tree also reports the covered button as displayed.

## Build

From the repository root:

```bash
tools/environment/scripts/flutter/build_app.sh
```

It builds `build/app/outputs/flutter-apk/app-debug.apk` with the entry point `integration_test/appium_test.dart`,
which starts the Appium Flutter server inside the app. The Flutter version is pinned in `pubspec.yaml`
(`environment.flutter`).
