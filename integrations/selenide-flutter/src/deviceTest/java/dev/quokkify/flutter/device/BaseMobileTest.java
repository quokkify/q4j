package dev.quokkify.flutter.device;

import java.util.Map;

import dev.quokkify.flutter.FlutterVisibility;
import dev.quokkify.flutter.device.screens.HomeScreen;
import dev.quokkify.flutter.driver.FlutterDriverProvider;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.logevents.SelenideLogger;
import io.appium.java_client.flutter.FlutterIntegrationTestDriver;
import io.qameta.allure.Allure;
import io.qameta.allure.selenide.AllureSelenide;
import org.openqa.selenium.MutableCapabilities;
import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

/**
 * Runs a device scenario in one {@link MobileMode}; scenario classes create one instance per mode with a
 * {@code @Factory}. A {@link NotReachableInModeException} becomes a skip whose reason starts with
 * {@code not reachable in <mode> mode:}.
 */
public abstract class BaseMobileTest implements IHookable {

  private static final String ALLURE_LISTENER = "AllureSelenide";
  private static final long TIMEOUT_MS = 10_000;

  protected final MobileMode mode;
  protected final ModeLocators locators;

  protected BaseMobileTest(MobileMode mode) {
    this.mode = mode;
    this.locators = new ModeLocators(mode);
  }

  @BeforeClass(alwaysRun = true)
  public void selectMode() {
    if (WebDriverRunner.hasWebDriverStarted()
        && (WebDriverRunner.getWebDriver() instanceof FlutterIntegrationTestDriver) != (mode == MobileMode.FLUTTER)) {
      Selenide.closeWebDriver();
    }
    Configuration.browser = mode == MobileMode.FLUTTER
        ? FlutterDriverProvider.class.getName()
        : NativeAndroidDriverProvider.class.getName();
    Configuration.timeout = TIMEOUT_MS;
    Configuration.browserSize = null;
    // The app keeps version 1.0.0, so without this Appium skips installing a freshly built apk.
    Configuration.browserCapabilities = new MutableCapabilities(Map.of("appium:enforceAppInstall", true));
  }

  @BeforeMethod(alwaysRun = true)
  public void installListeners() {
    SelenideLogger.addListener(ALLURE_LISTENER, new AllureSelenide().screenshots(true).savePageSource(true));
    FlutterVisibility.install();
    Allure.parameter("mode", mode.label());
  }

  @AfterMethod(alwaysRun = true)
  public void removeListeners() {
    FlutterVisibility.uninstall();
    SelenideLogger.removeListener(ALLURE_LISTENER);
  }

  @AfterClass(alwaysRun = true)
  public void closeDriver() {
    Selenide.closeWebDriver();
  }

  /**
   * Returns to the home screen: waits for it after a fresh start, otherwise presses back until it shows.
   */
  protected HomeScreen home() {
    return HomeScreen.reach(locators);
  }

  @Override
  public void run(IHookCallBack callBack, ITestResult testResult) {
    callBack.runTestMethod(testResult);
    NotReachableInModeException notReachable = notReachable(testResult.getThrowable());
    if (notReachable != null) {
      throw new SkipException(notReachable.getMessage(), notReachable);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + mode.label() + "]";
  }

  private static NotReachableInModeException notReachable(Throwable error) {
    for (Throwable cause = error; cause != null; cause = cause.getCause()) {
      if (cause instanceof NotReachableInModeException notReachable) {
        return notReachable;
      }
    }
    return null;
  }
}
