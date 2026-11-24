# integrations/selenide-flutter

Selenide + Appium for Flutter apps, with a readable report when an element is not found.

A Flutter app draws its own widgets, so a native driver sees an accessibility tree, not the widget tree, and a
failed `$(...)` tells you only "element not found". This module runs Selenide on the
[Appium Flutter integration driver](https://github.com/AppiumTestDistribution/appium-flutter-integration-driver)
and, on a failed search, attaches what the widget tree held at that moment: every find attempt, the tree, and the
closest matches with the reason each was rejected.

**Documentation:** [overview](../../docs/selenide-flutter/README.md) ·
[quick start](../../docs/selenide-flutter/quick-start.md) · [locators](../../docs/selenide-flutter/locators.md) ·
[waiting and conditions](../../docs/selenide-flutter/waiting-and-conditions.md) ·
[reports and diagnostics](../../docs/selenide-flutter/reports-and-diagnostics.md) ·
[configuration](../../docs/selenide-flutter/configuration.md) ·
[page objects](../../docs/selenide-flutter/page-objects.md) · [running](../../docs/selenide-flutter/running.md) ·
[flutter vs native](../../docs/selenide-flutter/flutter-vs-native.md) · [FAQ](../../docs/selenide-flutter/faq.md)

## Status and limits

- **Experimental.** Not published to Maven Central: use it inside the q4j build
  (`project(":integrations:selenide-flutter")`); external consumers are not supported yet.
- Android only. iOS is not supported yet.
- The app must be a debug or profile build that embeds `appium_flutter_server`; a release build has no server.
- Appium 3.x: `appium-flutter-integration-driver` 2.0.3 declares `appium ^3`. The pins live in
  [`versions.env`](../../tools/environment/scripts/flutter/versions.env).
- The server finds only hit-testable widgets, and its tree carries no visible/enabled flags: suggestions name
  hiding ancestors by type and do not detect occlusion.
- A key on a `KeyedSubtree` reads the text `""` and `enabled=false`
  ([the keyed-wrapper trap](../../docs/selenide-flutter/locators.md#the-keyed-wrapper-trap)).

## 30 seconds

What you write (a base class and a test against the module's test app):

```java
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.appium.SelenideAppium;
import com.codeborne.selenide.logevents.SelenideLogger;
import dev.quokkify.flutter.FlutterBy;
import dev.quokkify.flutter.FlutterVisibility;
import dev.quokkify.flutter.driver.FlutterDriverProvider;
import io.qameta.allure.selenide.AllureSelenide;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public abstract class BaseFlutterTest {

  @BeforeClass
  public void configure() {
    Configuration.browser = FlutterDriverProvider.class.getName();
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
}

public class LoginTest extends BaseFlutterTest {

  @Test
  public void tapsLogin() {
    SelenideAppium.launchApp();
    SelenideAppium.$(FlutterBy.key("home_login")).click();
    SelenideAppium.$(FlutterBy.key("login_btn")).click(); // typo: the key is login_button
  }
}
```

What you get in the Allure step `Flutter: suggestions` (real run, Android 14 emulator):

```text
0.75 KeyedSubtree key="login_button" @0,0 363x48 — similar key (distance 3)
0.55 KeyedSubtree key="login_email" @0,0 363x24 — similar key (distance 5)
```

Next to it: every find attempt of the step and the widget tree as JSON and as indented text
([all attachments](../../docs/selenide-flutter/reports-and-diagnostics.md#attachments)).

Run the module's own device tests, which install Appium, build the [test app](test-app/README.md), boot the
emulator and start Appium in one command ([details](../../docs/selenide-flutter/running.md)):

```bash
tools/environment/scripts/flutter/run_device_tests.sh flutterDeviceTest
```
