#!/usr/bin/env bash
# One-command local environment for the selenide-flutter device tests: installs the pinned
# Appium and drivers project-locally, builds the test app, boots the emulator, starts Appium
# and runs a Gradle task of :integrations:selenide-flutter.
# Usage (from the repo root): run_device_tests.sh [flutterDeviceTest|flutterComparison|flutterFailureDemo]
# Env: HEADLESS=1 boots the emulator without a window; CI=true skips the emulator boot
# (the CI emulator action boots it); AVD_NAME overrides the AVD from versions.env.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
avd_override="${AVD_NAME:-}"
# shellcheck source=versions.env
source "$SCRIPT_DIR/versions.env"
AVD_NAME="${avd_override:-$AVD_NAME}"

GRADLE_TASK="${1:-flutterDeviceTest}"
ENV_DIR="$REPO_ROOT/build/flutter-env"
NPM_PREFIX="$ENV_DIR/npm"
export APPIUM_HOME="$ENV_DIR/appium-home"
APPIUM_PORT=4723
APPIUM_STATUS_URL="http://127.0.0.1:$APPIUM_PORT/status"
EMULATOR_BOOT_TIMEOUT=300
APPIUM_READY_TIMEOUT=60

log() { echo "[flutter-env] $*"; }

# Android tools are usually not on PATH; fall back to the SDK locations.
for sdk in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}"; do
  [[ -n "$sdk" && -d "$sdk" ]] || continue
  for dir in platform-tools emulator cmdline-tools/latest/bin; do
    [[ -d "$sdk/$dir" ]] && PATH="$PATH:$sdk/$dir"
  done
done
export PATH

# --- 1. prerequisites ---------------------------------------------------------------
missing=0
require() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[flutter-env] missing: $1 -- $2" >&2
    missing=1
  fi
}
sdk_hint="install the Android command-line tools and set ANDROID_HOME (https://developer.android.com/studio#command-tools)"
require java "install a JDK 21+ (https://adoptium.net)"
require node "install Node.js 20+ (https://nodejs.org)"
require npm "install Node.js 20+ (https://nodejs.org)"
require flutter "install Flutter (https://docs.flutter.dev/get-started/install)"
require curl "install curl with your package manager"
require adb "$sdk_hint; then: sdkmanager \"platform-tools\""
if [[ "${CI:-}" != "true" ]]; then
  require emulator "$sdk_hint; then: sdkmanager \"emulator\""
  require sdkmanager "$sdk_hint"
  require avdmanager "$sdk_hint"
fi
if [[ "$missing" -ne 0 ]]; then
  exit 1
fi

mkdir -p "$ENV_DIR" "$APPIUM_HOME"

# --- 2. Appium and drivers (project-local) ------------------------------------------
APPIUM_BIN="$NPM_PREFIX/node_modules/.bin/appium"
appium_version="$("$APPIUM_BIN" --version 2>/dev/null || true)"
if [[ "$appium_version" != "$APPIUM_VERSION" ]]; then
  log "installing appium@$APPIUM_VERSION into build/flutter-env/npm"
  npm install --silent --no-audit --no-fund --prefix "$NPM_PREFIX" "appium@$APPIUM_VERSION"
fi

installed_drivers="$("$APPIUM_BIN" driver list --installed --json 2>/dev/null || true)"
driver_version() {
  node -e 'let s = ""; process.stdin.on("data", (d) => (s += d)).on("end", () => { try { process.stdout.write(JSON.parse(s)[process.argv[1]]?.version ?? ""); } catch { /* not installed */ } });' "$1" <<<"$installed_drivers"
}
if [[ "$(driver_version uiautomator2)" != "$UIAUTOMATOR2_DRIVER_VERSION" ]]; then
  log "installing uiautomator2@$UIAUTOMATOR2_DRIVER_VERSION"
  "$APPIUM_BIN" driver uninstall uiautomator2 >/dev/null 2>&1 || true
  "$APPIUM_BIN" driver install "uiautomator2@$UIAUTOMATOR2_DRIVER_VERSION"
fi
if [[ "$(driver_version flutter-integration)" != "$FLUTTER_INTEGRATION_DRIVER_VERSION" ]]; then
  log "installing appium-flutter-integration-driver@$FLUTTER_INTEGRATION_DRIVER_VERSION"
  "$APPIUM_BIN" driver uninstall flutter-integration >/dev/null 2>&1 || true
  "$APPIUM_BIN" driver install --source=npm "appium-flutter-integration-driver@$FLUTTER_INTEGRATION_DRIVER_VERSION"
fi

# --- 3. test app --------------------------------------------------------------------
"$SCRIPT_DIR/build_app.sh"

# --- 4. emulator --------------------------------------------------------------------
# "offline" is an emulator that is still booting; count it so a rerun does not start a second one.
has_emulator() { adb devices | awk 'NR > 1 && $1 ~ /^emulator-/ && ($2 == "device" || $2 == "offline") { found = 1 } END { exit !found }'; }

emulator_pid=""
if [[ "${CI:-}" == "true" ]]; then
  log "CI=true: expecting the emulator to be booted already"
elif has_emulator; then
  log "an emulator is already running"
else
  avds="$(emulator -list-avds)" || avds=""
  if ! grep -qxF "$AVD_NAME" <<<"$avds"; then
    echo "[flutter-env] AVD '$AVD_NAME' does not exist; create it with:" >&2
    echo "  sdkmanager \"$ANDROID_SYSTEM_IMAGE\"" >&2
    echo "  avdmanager create avd --name $AVD_NAME --package \"$ANDROID_SYSTEM_IMAGE\" --device pixel_6" >&2
    exit 1
  fi
  emulator_args=(-avd "$AVD_NAME" -no-snapshot-save -no-audio -no-boot-anim)
  if [[ "${HEADLESS:-}" == "1" ]]; then
    emulator_args+=(-no-window)
  fi
  log "booting emulator $AVD_NAME"
  nohup emulator "${emulator_args[@]}" >"$ENV_DIR/emulator.log" 2>&1 &
  emulator_pid=$!
  echo "$emulator_pid" >"$ENV_DIR/emulator.pid"
fi

if [[ "${CI:-}" != "true" ]]; then
  log "waiting for the emulator to finish booting (up to ${EMULATOR_BOOT_TIMEOUT}s)"
  deadline=$((SECONDS + EMULATOR_BOOT_TIMEOUT))
  until [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; do
    if [[ -n "$emulator_pid" ]] && ! kill -0 "$emulator_pid" 2>/dev/null; then
      echo "[flutter-env] emulator exited during boot; see build/flutter-env/emulator.log" >&2
      exit 1
    fi
    if ((SECONDS >= deadline)); then
      echo "[flutter-env] emulator did not finish booting within ${EMULATOR_BOOT_TIMEOUT}s; see build/flutter-env/emulator.log" >&2
      exit 1
    fi
    sleep 3
  done
fi

# --- 5. Appium ----------------------------------------------------------------------
appium_up() { curl -fsS --max-time 2 "$APPIUM_STATUS_URL" >/dev/null 2>&1; }

if appium_up; then
  log "Appium is already listening on port $APPIUM_PORT"
else
  log "starting Appium $APPIUM_VERSION"
  nohup "$APPIUM_BIN" --port "$APPIUM_PORT" --log "$ENV_DIR/appium.log" >/dev/null 2>&1 &
  appium_pid=$!
  echo "$appium_pid" >"$ENV_DIR/appium.pid"
  deadline=$((SECONDS + APPIUM_READY_TIMEOUT))
  until appium_up; do
    if ! kill -0 "$appium_pid" 2>/dev/null; then
      echo "[flutter-env] Appium exited during startup; see build/flutter-env/appium.log" >&2
      exit 1
    fi
    if ((SECONDS >= deadline)); then
      echo "[flutter-env] Appium did not become ready within ${APPIUM_READY_TIMEOUT}s; see build/flutter-env/appium.log" >&2
      exit 1
    fi
    sleep 1
  done
fi

# --- 6. Gradle ----------------------------------------------------------------------
log "running :integrations:selenide-flutter:$GRADLE_TASK"
cd "$REPO_ROOT"
exec ./gradlew ":integrations:selenide-flutter:$GRADLE_TASK"
