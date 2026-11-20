package dev.quokkify.flutter.device;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.flutter.config.FlutterConfiguration;

import com.codeborne.selenide.WebDriverProvider;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;

/**
 * Starts the same test app under plain UiAutomator2, so scenarios can run against the native accessibility tree.
 */
public class NativeAndroidDriverProvider implements WebDriverProvider {

  @Override
  public WebDriver createDriver(Capabilities capabilities) {
    FlutterConfiguration config = ConfigRegistry.get(FlutterConfiguration.class);
    UiAutomator2Options options = new UiAutomator2Options()
        .setDeviceName(config.deviceName())
        .setApp(Path.of(config.appPath()).toAbsolutePath().toString());
    // Flutter resource-ids have no package prefix; without this AppiumBy.id looks for "<package>:id/<id>".
    options.setCapability("appium:disableIdLocatorAutocompletion", true);
    return new AndroidDriver(appiumUrl(config.appiumUrl()), options.merge(capabilities));
  }

  private static URL appiumUrl(String value) {
    try {
      return URI.create(value).toURL();
    } catch (IllegalArgumentException | MalformedURLException e) {
      throw new IllegalArgumentException("APPIUM_URL is not a valid URL: " + value, e);
    }
  }
}
