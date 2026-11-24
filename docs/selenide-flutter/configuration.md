# Configuration

Point the driver at your Appium server and apk, tune the server's timeouts, and add your own capabilities.

## Keys

What you write:

```bash
FLUTTER_APP_PATH=/path/to/your_app/build/app/outputs/flutter-apk/app-debug.apk \
  ./gradlew :your-module:test --tests CheckoutTest
```

What you get: `FlutterDriverProvider` installs that apk in the next session.

| Key                                | Default                                                | Meaning                                                                   |
| ---------------------------------- | ------------------------------------------------------ | ------------------------------------------------------------------------- |
| `APPIUM_URL`                       | `http://127.0.0.1:4723`                                | the Appium server                                                         |
| `FLUTTER_APP_PATH`                 | `test-app/build/app/outputs/flutter-apk/app-debug.apk` | the apk with the Appium Flutter server, relative to the working directory |
| `FLUTTER_DEVICE_NAME`              | `Android Emulator`                                     | `appium:deviceName`                                                       |
| `FLUTTER_DRIVER_FIND_TIMEOUT_MS`   | `1`                                                    | the server's own wait per find; values below `1` are rejected             |
| `FLUTTER_SERVER_LAUNCH_TIMEOUT_MS` | `120000`                                               | how long to wait for the server inside the app to start                   |

The keys live in `dev.quokkify.flutter.config.FlutterConfiguration`, which reads, first match wins: Java system
properties of the test JVM, environment variables, then `flutter.properties` on the test classpath. q4j
`ConfigRegistry` caches the result, so the values are read once per JVM.

Use environment variables. A `-D` on `./gradlew` sets a property of the Gradle JVM, not of the test JVM: q4j's
shared test setup (`gradle/tests.gradle`) passes only `file.encoding` and Allure properties to test JVMs, and this
module's device tasks add only `mobile.modes`, `mobile.excludeKnownGaps` and `allure.results.directory`. The test
JVM inherits the environment, so a variable works without build changes.

`flutter.properties` follows the same rules, but no test in the module covers it:

```properties
# src/test/resources/flutter.properties
APPIUM_URL=http://127.0.0.1:4723
```

[Waiting and conditions](waiting-and-conditions.md) explains why the find timeout defaults to `1`.

## `FlutterDriverProvider`

```java
Configuration.browser = FlutterDriverProvider.class.getName();
```

Selenide then starts every driver through `FlutterDriverProvider.createDriver(Capabilities)`, which builds a
`FlutterAndroidDriver` with these options:

| Option                       | Value                              |
| ---------------------------- | ---------------------------------- |
| `automationName`             | `FlutterIntegration`               |
| `platformName`               | `Android`                          |
| `appium:deviceName`          | `FLUTTER_DEVICE_NAME`              |
| `appium:app`                 | `FLUTTER_APP_PATH`, made absolute  |
| `flutterElementWaitTimeout`  | `FLUTTER_DRIVER_FIND_TIMEOUT_MS`   |
| `flutterServerLaunchTimeout` | `FLUTTER_SERVER_LAUNCH_TIMEOUT_MS` |

The provider validates two values before it connects:

```text
java.lang.IllegalArgumentException: APPIUM_URL is not a valid URL: <value>
java.lang.IllegalArgumentException: FLUTTER_DRIVER_FIND_TIMEOUT_MS must be at least 1 but was 0: ...
```

## Your own capabilities

Capabilities Selenide passes in are merged over the provider's options, so yours win. Set them through Selenide:

```java
Configuration.browserCapabilities = Configuration.browserCapabilities.merge(
    new MutableCapabilities(Map.of("appium:enforceAppInstall", true)));
```

Or subclass the provider when a capability belongs to a kind of session. The device tests do this for server-side
waiting: [`FlutterSyncDriverProvider`](waiting-and-conditions.md#server-side-waiting-flutter-sync).

The provider does not modify the capabilities object you pass.

## Selenide settings that matter

| Setting                     | Use                                                                         |
| --------------------------- | --------------------------------------------------------------------------- |
| `Configuration.browser`     | `FlutterDriverProvider.class.getName()`, or your subclass                   |
| `Configuration.timeout`     | how long Selenide polls a find or a condition; the device tests use 10 s    |
| `Configuration.browserSize` | `null`, as in the device tests, so Selenide does not try to resize a window |

Switching providers within one JVM needs `Selenide.closeWebDriver()` first; Selenide keeps the running driver
otherwise.
