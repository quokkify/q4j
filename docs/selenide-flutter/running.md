# Running

Run the device tests with one command: it installs Appium, builds the app, boots the emulator, starts Appium and
runs Gradle.

## One command

From the repository root:

```bash
tools/environment/scripts/flutter/run_device_tests.sh flutterDeviceTest
```

What it does, in order:

1. Checks `java`, `node`, `npm`, `flutter`, `curl` and `adb` (plus `emulator`, `sdkmanager` and `avdmanager`
   outside CI) and names the missing ones with an install hint.
2. Installs the pinned Appium, UiAutomator2 driver and Flutter integration driver into `build/flutter-env`; nothing
   is installed globally.
3. Builds the test app with `build_app.sh` (skipped when the apks are newer than the sources).
4. Boots the AVD and waits up to 300 s for it, unless an emulator is already running.
5. Starts Appium on port 4723 and waits up to 60 s for `/status`, unless one already listens there.
6. Runs `./gradlew :integrations:selenide-flutter:<task>`.

The task is the only argument; it defaults to `flutterDeviceTest`. When the AVD is missing, the script prints the
`sdkmanager` and `avdmanager` commands that create it.

| Variable     | Effect                                                           |
| ------------ | ---------------------------------------------------------------- |
| `HEADLESS=1` | boots the emulator without a window                              |
| `CI=true`    | skips the emulator boot and expects `adb` to see a booted device |
| `AVD_NAME`   | overrides the AVD name from `versions.env` (`q4j-flutter`)       |

Stop what the script started (only the processes recorded in its pid files):

```bash
tools/environment/scripts/flutter/stop_device_env.sh
```

Logs land in `build/flutter-env/appium.log` and `build/flutter-env/emulator.log`.

## Pinned versions

`tools/environment/scripts/flutter/versions.env`:

| Pin                                  | Value                                          |
| ------------------------------------ | ---------------------------------------------- |
| `APPIUM_VERSION`                     | `3.9.1`                                        |
| `UIAUTOMATOR2_DRIVER_VERSION`        | `8.7.3`                                        |
| `FLUTTER_INTEGRATION_DRIVER_VERSION` | `2.0.3`                                        |
| `ANDROID_API_LEVEL`                  | `34`                                           |
| `ANDROID_NDK_VERSION`                | `28.2.13676358`                                |
| `ANDROID_ABI`                        | `arm64-v8a` on arm64 hosts, `x86_64` otherwise |
| `AVD_NAME`                           | `q4j-flutter`                                  |

Flutter itself is pinned in `test-app/pubspec.yaml` (`environment.flutter`, 3.50.1); `build_app.sh` warns when the
installed Flutter differs. CI installs the NDK before it builds the app; locally, install it with
`sdkmanager "ndk;28.2.13676358"` if the build asks for it.

## Gradle tasks

| Task                 | Runs                                                                        | Output                                           |
| -------------------- | --------------------------------------------------------------------------- | ------------------------------------------------ |
| `flutterDeviceTest`  | every scenario except the failure demo, flutter mode, known gaps excluded   | `build/allure-results-device/flutterDeviceTest`  |
| `flutterFailureDemo` | the intentionally failing demo, flutter and native modes                    | `build/allure-results-device/flutterFailureDemo` |
| `flutterComparison`  | every scenario in flutter, flutter-sync and native modes, then a comparison | `build/flutter-comparison/summary.md`            |

Paths are relative to `integrations/selenide-flutter`. With the emulator and Appium already up, call Gradle
directly, and override the modes with `-Dmobile.modes`:

```bash
./gradlew :integrations:selenide-flutter:flutterDeviceTest -Dmobile.modes=flutter,native
```

A test that exposes a known gap of a mode carries the TestNG group `known-gap-flutter` or `known-gap-native`.
`flutterDeviceTest` excludes those groups; `flutterComparison` runs them and records them as `KNOWN_GAP`.

## Comparison output

`flutterComparison` writes `summary.md`, `results.json` and `verdict.txt` to `build/flutter-comparison/`. The
summary has a per-scenario table (status, ms and finds per mode), the known gaps with their reasons, a Timing
section and the failure-demo excerpts of every mode.

The task fails unless `verdict.txt` says `OK`:

```text
Flutter comparison verdict: <reason>
```

It is not `OK` when a scenario that is not a known gap failed, a failure demo did not fail in every mode, or a demo
captured no diagnostics. A known gap that passes shows as `PASSED (known gap did not reproduce)` and does not
break the verdict.

## CI

`.github/workflows/flutter-device-tests.yml` runs on an Android 14 emulator (`ubuntu-latest`, 45 min limit):

| Trigger                                                                                              | Task                |
| ---------------------------------------------------------------------------------------------------- | ------------------- |
| pull request to `main` touching the module, its scripts, the workflow or `gradle/libs.versions.toml` | `flutterDeviceTest` |
| nightly at 02:30 UTC                                                                                 | `flutterComparison` |
| manual dispatch                                                                                      | `flutterComparison` |

Artifacts: `flutter-allure-results`, `flutter-test-reports`, `flutter-appium-log`, and for the comparison runs
`flutter-comparison`, whose `summary.md` is also appended to the job summary.
