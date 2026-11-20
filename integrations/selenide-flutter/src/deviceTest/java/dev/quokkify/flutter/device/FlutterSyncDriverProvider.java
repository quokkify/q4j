package dev.quokkify.flutter.device;

import java.time.Duration;

import dev.quokkify.flutter.driver.FlutterDriverProvider;

import io.appium.java_client.flutter.FlutterDriverOptions;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;

/**
 * Starts the Flutter driver for {@link MobileMode#FLUTTER_SYNC}: every find waits on the Flutter server, which pumps
 * frames until the widget exists, instead of checking once and leaving the waiting to Selenide's polling.
 *
 * <p>The wait goes into this session's capabilities only (they override the provider's options), so the
 * {@code FLUTTER_DRIVER_FIND_TIMEOUT_MS} configuration that {@code flutter} mode reads, and its cached
 * {@code ConfigRegistry} entry, stay untouched.
 */
public class FlutterSyncDriverProvider extends FlutterDriverProvider {

  /**
   * Half of the Selenide timeout of the device tests (10 s): a missing element still gets a second server-side wait
   * and Selenide's own timeout keeps deciding when a step fails.
   */
  static final Duration SERVER_FIND_WAIT = Duration.ofSeconds(5);

  @Override
  public WebDriver createDriver(Capabilities capabilities) {
    return super.createDriver(capabilities.merge(new FlutterDriverOptions()
        .setFlutterElementWaitTimeout(SERVER_FIND_WAIT)));
  }
}
