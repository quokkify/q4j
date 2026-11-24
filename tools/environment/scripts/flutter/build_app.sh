#!/usr/bin/env bash
# Builds the two debug apks of the selenide-flutter test app and prints their paths:
# - flutter mode: entry point integration_test/appium_test.dart, which embeds the Appium Flutter server;
# - native mode: the plain lib/main.dart app, without the server's test binding (which mocks text input).
# Skips each build when its apk is newer than the sources.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
APP_DIR="$REPO_ROOT/integrations/selenide-flutter/test-app"
ENTRY_POINT="integration_test/appium_test.dart"
APK="$APP_DIR/build/app/outputs/flutter-apk/app-debug.apk"
NATIVE_APK="$APP_DIR/build/native/app-native-debug.apk"

if ! command -v flutter >/dev/null 2>&1; then
  echo "[flutter] flutter is not on PATH; install it from https://docs.flutter.dev/get-started/install" >&2
  exit 1
fi

cd "$APP_DIR"

pinned="$(awk '/^environment:/ { in_env = 1; next } in_env && /^[^[:space:]]/ { in_env = 0 } in_env && $1 == "flutter:" { gsub(/["'\'']/, "", $2); print $2; exit }' pubspec.yaml)"
installed="$(flutter --version 2>/dev/null | awk '/^Flutter / && version == "" { version = $2 } END { print version }')"
if [[ -z "$pinned" ]]; then
  echo "[flutter] warning: environment.flutter is not pinned in pubspec.yaml" >&2
elif [[ "$installed" != "$pinned" ]]; then
  echo "[flutter] warning: Flutter $installed is installed, pubspec.yaml pins $pinned" >&2
fi

# is_stale <apk>: true when the apk is missing or older than any app source.
is_stale() {
  [[ ! -f "$1" ]] && return 0
  [[ -n "$(find lib integration_test android pubspec.yaml \
    \( -name .gradle -o -name .kotlin -o -name .cxx -o -name local.properties \) -prune \
    -o -newer "$1" -print -quit)" ]]
}

flutter_stale=0
native_stale=0
is_stale "$APK" && flutter_stale=1
is_stale "$NATIVE_APK" && native_stale=1

if ((flutter_stale || native_stale)); then
  flutter pub get
fi

# Both variants come out of Gradle as app-debug.apk, so the native one is built first and copied away.
if ((native_stale)); then
  flutter build apk --debug
  mkdir -p "$(dirname "$NATIVE_APK")"
  cp "$APK" "$NATIVE_APK"
  flutter_stale=1
else
  echo "[flutter] native apk is up to date"
fi

if ((flutter_stale)); then
  flutter build apk --debug -t "$ENTRY_POINT"
  # Gradle keeps the old apk mtime when the output is unchanged; mark it fresh.
  touch "$APK"
else
  echo "[flutter] flutter apk is up to date"
fi

echo "$APK"
echo "$NATIVE_APK"
