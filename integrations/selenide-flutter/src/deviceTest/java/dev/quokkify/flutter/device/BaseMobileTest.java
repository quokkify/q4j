package dev.quokkify.flutter.device;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.flutter.FlutterVisibility;
import dev.quokkify.flutter.config.FlutterConfiguration;
import dev.quokkify.flutter.device.screens.HomeScreen;
import dev.quokkify.flutter.diagnostics.FailureDiagnostics;
import dev.quokkify.flutter.driver.FlutterDriverProvider;
import dev.quokkify.flutter.report.AllureSink;
import dev.quokkify.flutter.report.LogSink;

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
import org.testng.annotations.Listeners;

/**
 * Runs a device scenario in one {@link MobileMode}; scenario classes create one instance per mode with a
 * {@code @Factory}. A {@link NotReachableInModeException} becomes a skip whose reason starts with
 * {@code not reachable in <mode> mode:}.
 */
@Listeners(KnownGapInterceptor.class)
public abstract class BaseMobileTest implements IHookable {

  private static final String ALLURE_LISTENER = "AllureSelenide";
  private static final long TIMEOUT_MS = 10_000;
  private static final String ENFORCE_APP_INSTALL = "appium:enforceAppInstall";

  private static String installedApk;

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
    Configuration.browserCapabilities = Configuration.browserCapabilities.merge(new MutableCapabilities(
        Map.of(ENFORCE_APP_INSTALL, mustInstall(appPath()))));
  }

  /**
   * Both apks share one package and version, so Appium would keep whichever is installed. The apk is installed once
   * per run and again whenever the other mode's apk was installed in between. An apk counts as installed only once
   * a session with it has started.
   */
  private static synchronized boolean mustInstall(String apk) {
    return !apk.equals(installedApk);
  }

  private static synchronized void markInstalled(String apk) {
    installedApk = apk;
  }

  private String appPath() {
    return mode == MobileMode.FLUTTER
        ? Path.of(ConfigRegistry.get(FlutterConfiguration.class).appPath()).toAbsolutePath().toString()
        : NativeAndroidDriverProvider.appPath();
  }

  @BeforeMethod(alwaysRun = true)
  public void installListeners() {
    SelenideLogger.addListener(ALLURE_LISTENER, new AllureSelenide().screenshots(true).savePageSource(true));
    FlutterVisibility.install(
        new FailureDiagnostics(FailureDiagnostics.DEFAULT_TIMEOUT, FailureDiagnostics.DEFAULT_MAX_CHARS),
        List.of(new AllureSink(), new LogSink(), ComparisonRecorder.sink()));
    Allure.parameter("mode", mode.label());
  }

  @AfterMethod(alwaysRun = true)
  public void removeListeners() {
    if (WebDriverRunner.hasWebDriverStarted()) {
      markInstalled(appPath());
    }
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
