# Running

Run the device tests with one command: it installs Appium, builds the app, boots the emulator, starts Appium and
runs Gradle.

## One command

Paths on this page are relative to the repository root unless they say otherwise. From the repository root:

```bash
tools/environment/scripts/flutter/run_device_tests.sh flutterDeviceTest
```

What it does, in order:

1. Checks `java`, `node`, `npm`, `flutter`, `curl` and `adb` (plus `emulator`, `sdkmanager` and `avdmanager`
   outside CI) and names the missing ones with an install hint.
2. Installs the pinned Appium, UiAutomator2 driver and Flutter integration driver into `build/flutter-env`; nothing
   is installed globally.
3. Builds the test app with `build_app.sh` (skipped when the apks are newer than the sources).
4. Boots the AVD unless an emulator is already running, then waits up to 300 s until `sys.boot_completed` is `1`
   (it waits even for an emulator it did not start). With `CI=true` it skips both the boot and the wait.
5. Starts Appium on port 4723 and waits up to 60 s for `/status`, unless one already listens there.
6. Runs `./gradlew :integrations:selenide-flutter:<task>`.

The task is the only argument; it defaults to `flutterDeviceTest`. When the AVD is missing, the script stops and
prints the commands that create it, with `$ANDROID_SYSTEM_IMAGE` filled in from `versions.env`:

```bash
sdkmanager "$ANDROID_SYSTEM_IMAGE"
avdmanager create avd --name q4j-flutter --package "$ANDROID_SYSTEM_IMAGE" --device pixel_6
```

`ANDROID_SYSTEM_IMAGE` is `system-images;android-34;google_apis;<abi>`, with `<abi>` `arm64-v8a` on arm64 hosts and
`x86_64` otherwise.

| Variable     | Effect                                                                              |
| ------------ | ----------------------------------------------------------------------------------- |
| `HEADLESS=1` | boots the emulator without a window                                                 |
| `CI=true`    | skips the emulator boot and the boot wait, and expects `adb` to see a booted device |
| `AVD_NAME`   | overrides the AVD name from `versions.env` (`q4j-flutter`)                          |

Stop what the script started (only the processes recorded in its pid files):

```bash
tools/environment/scripts/flutter/stop_device_env.sh
```

Logs land in `build/flutter-env/appium.log` and `build/flutter-env/emulator.log`.

## Pinned versions

The versions of Appium, both Appium drivers, the Android API level, the NDK and the AVD name live in
[`tools/environment/scripts/flutter/versions.env`](../../tools/environment/scripts/flutter/versions.env); the
scripts and CI source that file, so it wins over any version quoted elsewhere. Flutter itself is pinned in
`integrations/selenide-flutter/test-app/pubspec.yaml` (`environment.flutter`); `build_app.sh` warns when the
installed Flutter differs.

CI installs the pinned NDK before it builds the app. Locally, if the Android build asks for an NDK, install the
version from `versions.env` with `sdkmanager "ndk;<ANDROID_NDK_VERSION>"`; the local script does not install it.

## Gradle tasks

| Task                 | Runs                                                                        | Output                                           |
| -------------------- | --------------------------------------------------------------------------- | ------------------------------------------------ |
| `flutterDeviceTest`  | every scenario except the failure demo, flutter mode, known gaps excluded   | `build/allure-results-device/flutterDeviceTest`  |
| `flutterFailureDemo` | the intentionally failing demo, flutter and native modes                    | `build/allure-results-device/flutterFailureDemo` |
| `flutterComparison`  | every scenario in flutter, flutter-sync and native modes, then a comparison | `build/flutter-comparison/summary.md`            |

Output paths in this table are relative to `integrations/selenide-flutter`. With the emulator and Appium already up, call Gradle
directly, and override the modes with `-Dmobile.modes`:

```bash
./gradlew :integrations:selenide-flutter:flutterDeviceTest -Dmobile.modes=flutter,native
```

`build_app.sh` builds two debug apks: `test-app/build/app/outputs/flutter-apk/app-debug.apk` (flutter modes, with
the Appium server) and `test-app/build/native/app-native-debug.apk` (native mode, without it), both under
`integrations/selenide-flutter`. They share one package, so the tests reinstall the apk whenever the mode switches.

A test that exposes a known gap of a mode carries the TestNG group `known-gap-flutter` (covering `flutter` and
`flutter-sync`) or `known-gap-native`.
`flutterDeviceTest` excludes those groups; `flutterComparison` runs them and records them as `KNOWN_GAP`.

## Comparison output

`flutterComparison` writes `summary.md`, `results.json` and `verdict.txt` to
`integrations/selenide-flutter/build/flutter-comparison/`. The
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
