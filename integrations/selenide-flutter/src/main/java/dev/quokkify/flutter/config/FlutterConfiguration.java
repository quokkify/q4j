package dev.quokkify.flutter.config;

import org.aeonbits.owner.Config;

@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({"system:properties", "system:env", "classpath:flutter.properties"})
public interface FlutterConfiguration extends Config {

  @Key("APPIUM_URL")
  @DefaultValue("http://127.0.0.1:4723")
  String appiumUrl();

  @Key("FLUTTER_APP_PATH")
  @DefaultValue("test-app/build/app/outputs/flutter-apk/app-debug.apk")
  String appPath();

  @Key("FLUTTER_DEVICE_NAME")
  @DefaultValue("Android Emulator")
  String deviceName();

  /**
   * The Flutter server's own wait per find. Selenide does the waiting, so the server should check once: with
   * {@code 0} the server's wait loop times out before it evaluates the finder and no element is ever found;
   * {@code 1} gives exactly one check.
   */
  @Key("FLUTTER_DRIVER_FIND_TIMEOUT_MS")
  @DefaultValue("1")
  long findTimeoutMs();

  @Key("FLUTTER_SERVER_LAUNCH_TIMEOUT_MS")
  @DefaultValue("120000")
  long serverLaunchTimeoutMs();
}
