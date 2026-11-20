# integrations/selenide-flutter

Selenide + Appium for Flutter apps, with a readable report when an element is not found.

A Flutter app draws its own widgets, so a native driver sees an accessibility tree, not the widget tree. When
`$(...)` fails, you learn only "element not found". This module wraps the
[Appium Flutter integration driver](https://github.com/AppiumTestDistribution/appium-flutter-integration-driver)
with Selenide and, on a failed search, attaches what the widget tree held at that moment: every find attempt, the
tree, and the closest matches with the reason each was rejected.

> **Experimental.** The module is not published to Maven Central yet. Use it from a checkout of this repository
> (`project(":integrations:selenide-flutter")`).

---

## Limits

- Android only. iOS is not supported yet.
- Appium is pinned to 3.x: `appium-flutter-integration-driver` 2.0.3 declares `appium ^3`, and as of 2026-11-20
  upstream shows no Appium 4 work. The pinned versions live in `tools/environment/scripts/flutter/versions.env`.
- The app must be a debug or profile build that embeds `appium_flutter_server`. A release build has no server.
- The server's find uses `hitTestable()`: an element that exists in the tree but cannot receive a hit is not found.
- The real `renderTree` carries no visible/enabled flags or absolute rects. Suggestions name ancestors whose widget
  type can block a hit (`Offstage`, `Visibility`, `Opacity`, `IgnorePointer`, `AbsorbPointer`) as a possible cause
  only: the ancestor's flag is not in the tree. Occlusion by another widget is not detected.
- Keyed-wrapper gotcha: when the key sits on a wrapper such as a `KeyedSubtree`, `$(FlutterBy.key(..))` reads the
  text `""` and `enabled=false`. Put the key on the widget itself, or assert on the child widget
  (`FlutterBy.descendant(FlutterBy.key(..), FlutterBy.type(..))`).
- The server reads text only from text widgets: a key on an `ElevatedButton` gives its real `enabled` state but the
  text `""`. Check a label through its `Text` (`FlutterBy.text(..)` or a descendant locator).
- On a deep tree the widget-tree JSON exceeds the java-client nesting limit of 255, so diagnostics fall back to the
  topmost `Scaffold` subtree.

---

## Prerequisites

| Tool                  | Version                                                  |
| --------------------- | -------------------------------------------------------- |
| JDK                   | 21+                                                      |
| Node.js               | 20+ (Appium is installed project-locally)                |
| Flutter               | the version in `test-app/pubspec.yaml` (`environment`)   |
| Android SDK + adb     | `ANDROID_HOME` set; AVD `q4j-flutter` is created by hand |
| Emulator system image | `system-images;android-34;google_apis;<abi>` (API 34)    |

If the AVD is missing, `run_device_tests.sh` prints the `sdkmanager` and `avdmanager` commands to create it.

---

## Run the device tests

From the repository root:

```bash
tools/environment/scripts/flutter/run_device_tests.sh flutterDeviceTest
```

The script installs the pinned Appium and drivers into `build/flutter-env`, builds the test app, boots the emulator,
starts Appium and runs the Gradle task. The task is its only argument:

| Task                 | Runs                                                               | Output                                           |
| -------------------- | ------------------------------------------------------------------ | ------------------------------------------------ |
| `flutterDeviceTest`  | every scenario except the failure demo, flutter mode (the default) | `build/allure-results-device/flutterDeviceTest`  |
| `flutterFailureDemo` | the intentionally failing demo, flutter and native modes           | `build/allure-results-device/flutterFailureDemo` |
| `flutterComparison`  | every scenario in flutter and native modes, then a comparison      | `build/flutter-comparison/summary.md`            |

Output paths are relative to `integrations/selenide-flutter`.

Environment variables:

- `HEADLESS=1` boots the emulator without a window.
- `CI=true` skips the emulator boot (the CI emulator action boots it) and expects `adb` to see a device.
- `AVD_NAME` overrides the AVD name from `versions.env`.

Stop the Appium server and the emulator the script started:

```bash
tools/environment/scripts/flutter/stop_device_env.sh
```

`build_app.sh` builds two debug apks (flutter mode with the Appium server, native mode without it). Both share one
package, so the tests reinstall the apk when the mode switches. The CI job is
`.github/workflows/flutter-device-tests.yml`: `flutterDeviceTest` on pull requests, `flutterComparison` nightly and
on manual dispatch.

---

## Instrument your own app

1. Add the server to `pubspec.yaml`: `appium_flutter_server: 0.0.33` (the version the test app uses).
2. Create `integration_test/appium_test.dart`:

   ```dart
   import 'package:appium_flutter_server/appium_flutter_server.dart';
   import 'package:your_app/main.dart';

   void main() {
     initializeTest(app: const MyApp());
   }
   ```

3. Build a debug apk from that entry point:
   `flutter build apk --debug -t integration_test/appium_test.dart`.
4. Give every widget a test drives a stable id. Put the `key:` directly on the widget the test checks
   (`ElevatedButton(key: const ValueKey("pay"), ...)`): `FlutterBy.key` then lands on that widget and reads its
   real `enabled` state. For native drivers add a `Semantics(identifier: id)`, which Android exposes as the
   resource-id; wrap it in `MergeSemantics` so the id lands on the button's own node with its state and label
   (the test app's `direct_button` on the login screen does this). The test app's `Tagged` wrapper
   ([`test-app/lib/tagged.dart`](test-app/lib/tagged.dart)) trades that for convenience: one call sets the
   `ValueKey` and the identifier with the same string, but both land on wrapper nodes, so `FlutterBy.key` reads
   the text `""` and `enabled=false` and the native node is always enabled; assert on the child widget there.

---

## Configuration

Keys are read from system properties, environment variables, then `flutter.properties` on the classpath.

| Key                                | Default                                                | Meaning                                                            |
| ---------------------------------- | ------------------------------------------------------ | ------------------------------------------------------------------ |
| `APPIUM_URL`                       | `http://127.0.0.1:4723`                                | Appium server                                                      |
| `FLUTTER_APP_PATH`                 | `test-app/build/app/outputs/flutter-apk/app-debug.apk` | apk with the Appium server, resolved against the working directory |
| `FLUTTER_DEVICE_NAME`              | `Android Emulator`                                     | `appium:deviceName`                                                |
| `FLUTTER_DRIVER_FIND_TIMEOUT_MS`   | `1`                                                    | the server's own wait per find; values below 1 are rejected        |
| `FLUTTER_SERVER_LAUNCH_TIMEOUT_MS` | `120000`                                               | wait for the server inside the app to start                        |

Selenide does the waiting (`Configuration.timeout`), so the server checks once per find. `0` is rejected because
the server's wait loop then times out before it evaluates the finder and nothing is ever found.

---

## Use it in tests

Point Selenide at the provider and install visibility on each test thread before that thread creates its first
driver. Register `AllureSelenide` yourself; `install()` does not.

```java
public abstract class BaseMobileTest {

  @BeforeClass
  public void configure() {
    Configuration.browser = FlutterDriverProvider.class.getName();
  }

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
}
```

`install()` uses Allure and log sinks. `install(FailureDiagnostics, List<DiagnosticsSink>)` takes your own timeout,
size cap and sinks. A driver created before `install()` is not traced; the module logs a warning in that case.

### Locators

| Method                                                          | Finds                             |
| --------------------------------------------------------------- | --------------------------------- |
| `FlutterBy.key(key)`                                            | a widget by `ValueKey`            |
| `FlutterBy.text(text)` / `textContaining(text)`                 | a `Text` by exact or partial text |
| `FlutterBy.type(type)`                                          | a widget by runtime type          |
| `FlutterBy.semanticsLabel(label)`                               | a widget by Semantics label       |
| `FlutterBy.descendant(of, matching)` / `ancestor(of, matching)` | a widget relative to another      |

### Page and steps

Condensed from `src/deviceTest/.../screens/LoginScreen.java` and `steps/LoginScreenSteps.java` (the real classes
route locators through `ModeLocators` so the same scenario also runs natively):

```java
public class LoginScreen implements Page {

  public SelenideElement email()    { return SelenideAppium.$(FlutterBy.key("login_email")); }
  public SelenideElement password() { return SelenideAppium.$(FlutterBy.key("login_password")); }

  // The key sits on a KeyedSubtree, which reports enabled=false: target the button under it.
  public SelenideElement loginButton() {
    return SelenideAppium.$(FlutterBy.descendant(FlutterBy.key("login_button"), FlutterBy.type("ElevatedButton")));
  }

  public void type(SelenideElement field, String value) {
    field.click();
    field.setValue(value);
  }
}

public class LoginScreenSteps extends PageSteps<LoginScreenSteps, LoginScreenVerification, LoginScreen> {

  @Step("Type '{email}' as the email")
  public LoginScreenSteps typeEmail(String email) {
    page.type(page.email(), email);
    return this;
  }

  @Step("Submit the login form")
  public LoginScreenSteps submit() {
    page.loginButton().click();
    return this;
  }
}
```

---

## What a failed find attaches

When a Selenide step fails because no element was found, `FlutterVisibility` attaches these to the Allure step
(and logs them):

| Attachment                    | Content                                                                               |
| ----------------------------- | ------------------------------------------------------------------------------------- |
| `Flutter: element search`     | every find attempt of the step and its outcome (attached on every step that searched) |
| `Flutter: widget tree (json)` | the raw widget tree (plain text when truncated)                                       |
| `Flutter: widget tree`        | the tree as indented text                                                             |
| `Flutter: suggestions`        | the closest widgets, scored, with the reason each was rejected                        |
| `Flutter: diagnostics error`  | why diagnostics could not be collected, when that happens                             |

Real emulator run (`flutterComparison`, Android 14), flutter `Flutter: suggestions` against the compact native
excerpt for the same failure.

`FailureDemoTest.tapsMistypedKey` (the test looks up `login_btn`):

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

The flutter side names the near-miss key and the reason. The native side lists what is on screen and leaves the
matching to you.

---

## Flutter vs native comparison

`flutterComparison` runs every scenario in both modes and writes `build/flutter-comparison/summary.md` (plus
`results.json` and `verdict.txt`) under this module. The summary has a per-scenario table with status, duration and
find count per mode, the known gaps with their reasons, and the failure demo excerpts above. The task fails when the verdict is not `OK`: a scenario that is not a known gap failed, a failure demo did not fail, or a demo captured no diagnostics.

The observed differences between the modes, and how the test app is built, are in
[`test-app/README.md`](test-app/README.md).
