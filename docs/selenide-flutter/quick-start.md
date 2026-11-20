# Quick start

See the module work on the bundled test app in one command, then wire it to your own Flutter app.

Two tracks:

- [A. Try it in q4j](#a-try-it-in-q4j): run the module's own device tests and open the report. No code to write.
- [B. Use it with your app](#b-use-it-with-your-app): instrument your app, write a test, run it.

All commands run from the repository root.

## Prerequisites

| Tool                  | Version                                                                     |
| --------------------- | --------------------------------------------------------------------------- |
| JDK                   | 21+                                                                         |
| Node.js               | 20+ (Appium is installed project-locally by the run script)                 |
| Flutter               | the test app pins 3.50.1 (`environment.flutter` in `test-app/pubspec.yaml`) |
| Android SDK + adb     | `ANDROID_HOME` set                                                          |
| Emulator system image | `system-images;android-34;google_apis;<abi>` (API 34)                       |

Create the AVD once. These are the commands `run_device_tests.sh` prints when the AVD is missing; `<abi>` is
`arm64-v8a` on arm64 hosts and `x86_64` otherwise:

```bash
sdkmanager "system-images;android-34;google_apis;<abi>"
avdmanager create avd --name q4j-flutter --package "system-images;android-34;google_apis;<abi>" --device pixel_6
```

## A. Try it in q4j

```bash
tools/environment/scripts/flutter/run_device_tests.sh flutterDeviceTest
```

The script installs the pinned Appium and drivers into `build/flutter-env`, builds the test app, boots the
emulator, starts Appium and runs the module's device scenarios ([details](running.md)).

Open the results with the Allure command-line tool:

```bash
allure serve integrations/selenide-flutter/build/allure-results-device/flutterDeviceTest
```

To see a failure with its diagnostics, run the intentionally failing demo instead and open its directory,
`integrations/selenide-flutter/build/allure-results-device/flutterFailureDemo`:

```bash
tools/environment/scripts/flutter/run_device_tests.sh flutterFailureDemo
```

The failed steps carry `Flutter: element search`, `Flutter: widget tree` and `Flutter: suggestions`
([what each one shows](reports-and-diagnostics.md#attachments)).

## B. Use it with your app

The module is not published to Maven Central. Your tests live inside the q4j build, in a test source set of a
module that depends on `project(":integrations:selenide-flutter")`. Using it from a project outside q4j is not
supported yet.

### 1. Embed the Appium Flutter server in your app

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

The apk lands in `build/app/outputs/flutter-apk/app-debug.apk` of your app. A release build has no server, so the
driver cannot talk to it.

### 2. Give widgets a key

What you write:

```dart
ElevatedButton(
  key: const ValueKey("pay"),
  onPressed: canPay ? pay : null,
  child: const Text("Pay"),
)
```

What you get: `FlutterBy.key("pay")` lands on the `ElevatedButton` and reads its real `enabled` state.

Put the key on the widget the test checks. A key on a `KeyedSubtree` (which the test app's `Tagged` helper
creates) makes the element report the text `""` and `enabled=false` whatever the button does; other wrappers likely
behave the same, but only `KeyedSubtree` is verified. [Locators](locators.md#the-keyed-wrapper-trap) shows the fix.

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

The test app's `Tagged(id, child)` helper
([`test-app/lib/tagged.dart`](../../integrations/selenide-flutter/test-app/lib/tagged.dart)) sets the `ValueKey`
and the identifier in one call, but both land on wrapper nodes: `FlutterBy.key` hits the `KeyedSubtree`, and the
native node is always enabled. Assert on the child widget there.

### 3. Add the module

In your q4j module's `build.gradle`:

```groovy
dependencies {
    testImplementation project(":integrations:selenide-flutter")
    testImplementation libs.allure.selenide
    testImplementation libs.allure.testng
}
```

The module brings `integrations/selenide` and Selenide Appium as `api` dependencies. `AllureSelenide` comes from
`allure-selenide`, which the module does not ship.

### 4. Write the base test

```java
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.logevents.SelenideLogger;
import dev.quokkify.flutter.FlutterVisibility;
import dev.quokkify.flutter.driver.FlutterDriverProvider;
import io.qameta.allure.selenide.AllureSelenide;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

public abstract class BaseFlutterTest {

  @BeforeClass
  public void configure() {
    Configuration.browser = FlutterDriverProvider.class.getName();
    Configuration.timeout = 10_000;
    Configuration.browserSize = null; // no browser window on a device: keep Selenide from resizing one
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
traced ([lifecycle](reports-and-diagnostics.md#install-and-uninstall)).

### 5. Write the first test

```java
import static com.codeborne.selenide.Condition.enabled;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.appium.SelenideAppium;
import dev.quokkify.flutter.FlutterBy;
import org.testng.annotations.Test;

public class CheckoutTest extends BaseFlutterTest {

  @Test
  public void enablesPayForValidCard() {
    SelenideAppium.launchApp();

    SelenideElement card = SelenideAppium.$(FlutterBy.key("card_number"));
    card.click();
    card.setValue("4242424242424242");

    SelenideAppium.$(FlutterBy.key("pay")).shouldBe(enabled);
  }
}
```

The keys are your app's. The device tests use the same calls against the test app; see `LoginTest` in
`integrations/selenide-flutter/src/deviceTest`.

### 6. Start the emulator and Appium

The script from track A installs Appium into `build/flutter-env` on its first run, so run it once (any task) before
starting things by hand. Then, from the repository root:

```bash
emulator -avd q4j-flutter -no-snapshot-save -no-audio -no-boot-anim &
adb wait-for-device
APPIUM_HOME="$PWD/build/flutter-env/appium-home" build/flutter-env/npm/node_modules/.bin/appium \
  --port 4723 --log build/flutter-env/appium.log &
```

These are the flags `run_device_tests.sh` uses. Wait until `adb shell getprop sys.boot_completed` prints `1` and
`curl http://127.0.0.1:4723/status` answers. `stop_device_env.sh` stops only what the script itself started, so
stop these processes yourself.

### 7. Run your test

Pass the apk through an environment variable. A `-D` on `./gradlew` does not reach the test JVM
([why](configuration.md#keys)):

```bash
FLUTTER_APP_PATH=/path/to/your_app/build/app/outputs/flutter-apk/app-debug.apk \
  ./gradlew :your-module:test --tests CheckoutTest
```

Replace `:your-module:test` with the task that runs your test source set. A relative `FLUTTER_APP_PATH` resolves
against the test JVM's working directory, which is your module's directory.
