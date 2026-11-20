package dev.quokkify.flutter.driver;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;

import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.flutter.config.FlutterConfiguration;

import com.codeborne.selenide.WebDriverProvider;
import io.appium.java_client.flutter.FlutterDriverOptions;
import io.appium.java_client.flutter.android.FlutterAndroidDriver;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;

public class FlutterDriverProvider implements WebDriverProvider {

  private static final String AUTOMATION_NAME = "FlutterIntegration";
  private static final String PLATFORM_NAME = "Android";

  @Override
  public WebDriver createDriver(Capabilities capabilities) {
    FlutterConfiguration config = ConfigRegistry.get(FlutterConfiguration.class);
    return new FlutterAndroidDriver(appiumUrl(config), options(config).merge(capabilities));
  }

  static FlutterDriverOptions options(FlutterConfiguration config) {
    FlutterDriverOptions options = new FlutterDriverOptions();
    options.setAutomationName(AUTOMATION_NAME);
    options.setPlatformName(PLATFORM_NAME);
    options.setCapability("appium:deviceName", config.deviceName());
    options.setCapability("appium:app", Path.of(config.appPath()).toAbsolutePath().toString());
    options.setFlutterElementWaitTimeout(Duration.ofMillis(config.findTimeoutMs()));
    options.setFlutterServerLaunchTimeout(Duration.ofMillis(config.serverLaunchTimeoutMs()));
    return options;
  }

  static URL appiumUrl(FlutterConfiguration config) {
    String value = config.appiumUrl();
    try {
      return URI.create(value).toURL();
    } catch (IllegalArgumentException | MalformedURLException e) {
      throw new IllegalArgumentException("APPIUM_URL is not a valid URL: " + value, e);
    }
  }
}
