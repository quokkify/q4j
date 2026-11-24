package dev.quokkify.flutter.device;

import org.aeonbits.owner.Config;

/**
 * The apk native mode installs: the test app built from {@code lib/main.dart}, without the Appium Flutter server
 * whose test binding mocks the platform text input.
 */
@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({"system:properties", "system:env"})
public interface NativeAppConfiguration extends Config {

  @Key("FLUTTER_NATIVE_APP_PATH")
  @DefaultValue("test-app/build/native/app-native-debug.apk")
  String nativeAppPath();
}
