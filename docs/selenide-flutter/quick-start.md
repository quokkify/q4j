# Quick start

Go from a plain Flutter app to a passing Selenide test on an Android emulator.

## Prerequisites

| Tool                  | Version                                                                                  |
| --------------------- | ---------------------------------------------------------------------------------------- |
| JDK                   | 21+                                                                                      |
| Node.js               | 20+ (Appium is installed project-locally by the run script)                              |
| Flutter               | the test app pins 3.50.1 (`environment.flutter` in `test-app/pubspec.yaml`)              |
| Android SDK + adb     | `ANDROID_HOME` set                                                                       |
| Emulator system image | `system-images;android-34;google_apis;<abi>` (API 34), AVD `q4j-flutter` created by hand |

You do not install Appium yourself. [Running](running.md) explains what the one-command script installs and where.

## 1. Embed the Appium Flutter server in your app

Add the server to `pubspec.yaml`, as the test app does:

```yaml
dev_dependencies:
  integration_test:
    sdk: flutter
  appium_flutter_server: 0.0.33
```

Create `integration_test/appium_test.dart`, which starts your app with the server inside:

```dart
import 'package:appium_flutter_server/appium_flutter_server.dart';
import 'package:your_app/main.dart';

void main() {
  initializeTest(app: const MyApp());
}
```

Build a debug apk from that entry point:

```bash
flutter build apk --debug -t integration_test/appium_test.dart
```

The apk lands in `build/app/outputs/flutter-apk/app-debug.apk`. A release build has no server, so the driver cannot
talk to it.

## 2. Give widgets a key

What you write:

```dart
ElevatedButton(
  key: const ValueKey("pay"),
  onPressed: canPay ? pay : null,
  child: const Text("Pay"),
)
```

What you get: `FlutterBy.key("pay")` lands on the `ElevatedButton` and reads its real `enabled` state.

Put the key on the widget the test checks, not on a wrapper around it. A key on a wrapper such as `KeyedSubtree`
makes the element report the text `""` and `enabled=false` whatever the button does;
[Locators](locators.md#the-keyed-wrapper-trap) shows the fix.

If the same app must also run under a native driver, add a Semantics identifier (Android exposes it as the
resource-id) and wrap it in `MergeSemantics`, so the id lands on the button's own node with its state and label:

```dart
MergeSemantics(
  child: Semantics(
    identifier: "direct_button",
    child: ElevatedButton(
      key: const ValueKey("direct_button"),
      onPressed: _canSubmit ? _submit : null,
      child: const Text("Direct login"),
    ),
  ),
)
```

The test app also has a `Tagged(id, child)` helper
([`test-app/lib/tagged.dart`](../../integrations/selenide-flutter/test-app/lib/tagged.dart)) that sets the
`ValueKey` and the identifier in one call. It is convenient, but both ids land on wrapper nodes: `FlutterBy.key`
then hits the wrapper, and the native node is always enabled. Assert on the child widget there.

## 3. Add the module

Inside the q4j build, depend on the project:

```groovy
dependencies {
    testImplementation project(":integrations:selenide-flutter")
    testImplementation libs.allure.selenide
    testImplementation libs.allure.testng
}
```

The module brings `integrations/selenide` and Selenide Appium as `api` dependencies. `AllureSelenide` comes from
`allure-selenide`, which the module does not ship.

## 4. Write the base test

```java
public abstract class BaseFlutterTest {

  @BeforeClass
  public void configure() {
    Configuration.browser = FlutterDriverProvider.class.getName();
    Configuration.timeout = 10_000;
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

  @AfterClass(alwaysRun = true)
  public void closeDriver() {
    Selenide.closeWebDriver();
  }
}
```

Call `FlutterVisibility.install()` before the thread creates its first driver; a driver created earlier is not
traced. [Reports and diagnostics](reports-and-diagnostics.md#install-and-uninstall) has the full lifecycle.

## 5. Write the first test

```java
public class LoginTest extends BaseFlutterTest {

  @Test
  public void enablesLoginForValidInput() {
    SelenideAppium.launchApp();
    SelenideAppium.$(FlutterBy.key("home_login")).click();

    SelenideElement email = SelenideAppium.$(FlutterBy.key("login_email"));
    email.click();
    email.setValue("user@example.com");

    SelenideElement password = SelenideAppium.$(FlutterBy.key("login_password"));
    password.click();
    password.setValue("secret1");

    SelenideAppium.$(FlutterBy.key("direct_button")).shouldBe(enabled);
  }
}
```

`FlutterDriverProvider` reads the apk from `FLUTTER_APP_PATH` (default
`test-app/build/app/outputs/flutter-apk/app-debug.apk`, relative to the working directory). Point it at your apk:
see [Configuration](configuration.md).

## 6. Run it

From the repository root, one command installs the pinned Appium and drivers, builds the test app, boots the
emulator, starts Appium and runs the module's device tests:

```bash
tools/environment/scripts/flutter/run_device_tests.sh flutterDeviceTest
```

The Allure results land in `integrations/selenide-flutter/build/allure-results-device/flutterDeviceTest`. With the
emulator and Appium already up, run your own test task with Gradle as usual. [Running](running.md) has the details.
