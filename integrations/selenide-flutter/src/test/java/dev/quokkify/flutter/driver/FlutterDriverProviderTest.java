package dev.quokkify.flutter.driver;

import dev.quokkify.flutter.config.FlutterConfiguration;

import io.appium.java_client.flutter.FlutterDriverOptions;
import org.aeonbits.owner.ConfigFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FlutterDriverProviderTest {

  @AfterMethod(alwaysRun = true)
  public void clearProperties() {
    System.clearProperty("FLUTTER_APP_PATH");
    System.clearProperty("APPIUM_URL");
    System.clearProperty("FLUTTER_DRIVER_FIND_TIMEOUT_MS");
  }

  @Test
  public void buildsFlutterIntegrationOptions() {
    System.setProperty("FLUTTER_APP_PATH", "/apps/demo.apk");
    FlutterDriverOptions o = FlutterDriverProvider.options(ConfigFactory.create(FlutterConfiguration.class));
    assertThat(o.getCapability("appium:automationName")).isEqualTo("FlutterIntegration");
    assertThat(o.getCapability("platformName").toString()).isEqualToIgnoringCase("android");
    assertThat(o.getCapability("appium:app")).isEqualTo("/apps/demo.apk");
    assertThat(o.getCapability("appium:flutterElementWaitTimeout")).isEqualTo(1L);
    assertThat(o.getCapability("appium:flutterServerLaunchTimeout")).isEqualTo(120000L);
    assertThat(o.getCapability("appium:deviceName")).isEqualTo("Android Emulator");
  }

  @Test
  public void resolvesRelativeAppPathToAbsolute() {
    System.setProperty("FLUTTER_APP_PATH", "build/demo.apk");
    FlutterDriverOptions o = FlutterDriverProvider.options(ConfigFactory.create(FlutterConfiguration.class));
    assertThat(java.nio.file.Path.of((String) o.getCapability("appium:app"))).isAbsolute();
  }

  @Test
  public void rejectsFindTimeoutBelowOne() {
    System.setProperty("FLUTTER_DRIVER_FIND_TIMEOUT_MS", "0");
    FlutterConfiguration config = ConfigFactory.create(FlutterConfiguration.class);
    assertThatThrownBy(() -> FlutterDriverProvider.options(config))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("FLUTTER_DRIVER_FIND_TIMEOUT_MS must be at least 1 but was 0");
  }

  @Test
  public void malformedAppiumUrlNamesTheKey() {
    System.setProperty("APPIUM_URL", "not a url");
    FlutterConfiguration config = ConfigFactory.create(FlutterConfiguration.class);
    assertThatThrownBy(() -> FlutterDriverProvider.appiumUrl(config))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("APPIUM_URL")
        .hasMessageContaining("not a url");
  }
}
