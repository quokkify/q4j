# Selenide Flutter

Test a Flutter app with Selenide and Appium, and when `$(...)` finds nothing, read in the report which widget you
probably meant and why the driver skipped it.

`integrations/selenide-flutter` runs Selenide on the
[Appium Flutter integration driver](https://github.com/AppiumTestDistribution/appium-flutter-integration-driver),
which sees the Flutter widget tree instead of the accessibility tree. You write ordinary Selenide; the module adds
Flutter locators (`FlutterBy`), a driver provider (`FlutterDriverProvider`) and failure diagnostics
(`FlutterVisibility`) that attach the find attempts, the widget tree and the closest matches to the Allure step.

> **Status: experimental.** Android only. Not published to Maven Central: use it from a checkout of q4j
> (`project(":integrations:selenide-flutter")`).

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

Next to it: every find attempt of the step, the widget tree as JSON and as indented text. A native UiAutomator2 run
of the same failure gets no suggestions ([side by side](flutter-vs-native.md#failures-side-by-side)).

## Pages

| Page                                                  | Read it to                                                                     |
| ----------------------------------------------------- | ------------------------------------------------------------------------------ |
| [Quick start](quick-start.md)                         | instrument your app, add the module, write and run the first test              |
| [Locators](locators.md)                               | pick the right `FlutterBy` factory and avoid the keyed-wrapper trap            |
| [Waiting and conditions](waiting-and-conditions.md)   | know who waits, which Selenide conditions work, and when to wait on the server |
| [Reports and diagnostics](reports-and-diagnostics.md) | read every attachment and every suggestion reason                              |
| [Configuration](configuration.md)                     | set the Appium URL, the apk, the timeouts and your own capabilities            |
| [Page objects](page-objects.md)                       | write q4j `Page` / `PageSteps` / `Verification` classes for a Flutter screen   |
| [Running](running.md)                                 | start the emulator and Appium with one command, locally and in CI              |
| [Flutter vs native](flutter-vs-native.md)             | choose between the Flutter driver, server-side waiting and UiAutomator2        |
| [FAQ](faq.md)                                         | find short answers to the questions people ask first                           |

## Limits

- Android only; `FlutterDriverProvider` creates a `FlutterAndroidDriver`.
- The app must be a debug or profile build with `appium_flutter_server` inside; a release build has no server.
- Appium 3.x: `appium-flutter-integration-driver` 2.0.3 declares `appium ^3`.

The module's own reference is [`integrations/selenide-flutter/README.md`](../../integrations/selenide-flutter/README.md).
