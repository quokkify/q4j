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

  @Key("FLUTTER_DRIVER_FIND_TIMEOUT_MS")
  @DefaultValue("0")
  long findTimeoutMs();

  @Key("FLUTTER_SERVER_LAUNCH_TIMEOUT_MS")
  @DefaultValue("120000")
  long serverLaunchTimeoutMs();
}
