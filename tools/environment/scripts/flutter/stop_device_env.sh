#!/usr/bin/env bash
# Stops the Appium server and the emulator that run_device_tests.sh started
# (only the processes recorded in the pid files under build/flutter-env, and only
# when the pid still belongs to that kind of process).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
ENV_DIR="$REPO_ROOT/build/flutter-env"
STOP_TIMEOUT=15

for sdk in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}"; do
  [[ -n "$sdk" && -d "$sdk/platform-tools" ]] && PATH="$PATH:$sdk/platform-tools"
done
export PATH

# stop_recorded <name> <command-pattern>: the pid file is trusted only while the
# process command line matches the pattern; otherwise it is stale and removed.
stop_recorded() {
  local name="$1" pattern="$2" pid_file="$ENV_DIR/$1.pid" pid command
  if [[ ! -f "$pid_file" ]]; then
    echo "[flutter-env] no $name started by the script"
    return 0
  fi
  pid="$(cat "$pid_file")"
  command=""
  if [[ "$pid" =~ ^[0-9]+$ ]]; then
    command="$(ps -p "$pid" -o command= 2>/dev/null || true)"
  fi
  if [[ -z "$command" || ! "$command" =~ $pattern ]]; then
    echo "[flutter-env] $name (pid $pid) is gone or not ours; removing the stale pid file"
    rm -f "$pid_file"
    return 0
  fi
  if [[ "$name" == "emulator" ]] && command -v adb >/dev/null 2>&1; then
    serial="$(adb devices 2>/dev/null | awk 'NR > 1 && $1 ~ /^emulator-/ { print $1; exit }')"
    if [[ -n "$serial" ]]; then
      adb -s "$serial" emu kill >/dev/null 2>&1 || true
    fi
  fi
  local deadline=$((SECONDS + STOP_TIMEOUT))
  while kill -0 "$pid" 2>/dev/null; do
    if ((SECONDS >= deadline)); then
      kill -9 "$pid" 2>/dev/null || true
      break
    fi
    kill "$pid" 2>/dev/null || true
    sleep 1
  done
  echo "[flutter-env] stopped $name (pid $pid)"
  rm -f "$pid_file"
}

stop_recorded appium 'appium'
stop_recorded emulator 'qemu|emulator'
