#!/usr/bin/env bash
# Builds the selenide-flutter test app (debug apk with the Appium Flutter server)
# and prints the apk path. Skips the build when the apk is newer than its sources.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
APP_DIR="$REPO_ROOT/integrations/selenide-flutter/test-app"
ENTRY_POINT="integration_test/appium_test.dart"
APK="$APP_DIR/build/app/outputs/flutter-apk/app-debug.apk"

if ! command -v flutter >/dev/null 2>&1; then
  echo "[flutter] flutter is not on PATH; install it from https://docs.flutter.dev/get-started/install" >&2
  exit 1
fi

cd "$APP_DIR"

pinned="$(awk '/^environment:/ { in_env = 1; next } in_env && /^[^[:space:]]/ { in_env = 0 } in_env && $1 == "flutter:" { gsub(/["'\'']/, "", $2); print $2; exit }' pubspec.yaml)"
installed="$(flutter --version 2>/dev/null | awk '/^Flutter / { print $2; exit }')"
if [[ -z "$pinned" ]]; then
  echo "[flutter] warning: environment.flutter is not pinned in pubspec.yaml" >&2
elif [[ "$installed" != "$pinned" ]]; then
  echo "[flutter] warning: Flutter $installed is installed, pubspec.yaml pins $pinned" >&2
fi

if [[ -f "$APK" ]]; then
  newer="$(find lib integration_test android pubspec.yaml \
    \( -name .gradle -o -name .kotlin -o -name .cxx -o -name local.properties \) -prune \
    -o -newer "$APK" -print -quit)"
  if [[ -z "$newer" ]]; then
    echo "[flutter] apk is up to date"
    echo "$APK"
    exit 0
  fi
fi

flutter pub get
flutter build apk --debug -t "$ENTRY_POINT"
# Gradle keeps the old apk mtime when the output is unchanged; mark it fresh.
touch "$APK"
echo "$APK"
