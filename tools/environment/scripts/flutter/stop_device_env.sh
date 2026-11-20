#!/usr/bin/env bash
# Stops the Appium server and the emulator that run_device_tests.sh started
# (only the processes recorded in the pid files under build/flutter-env).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
ENV_DIR="$REPO_ROOT/build/flutter-env"
STOP_TIMEOUT=15

stop_recorded() {
  local name="$1" pid_file="$ENV_DIR/$1.pid" pid
  if [[ ! -f "$pid_file" ]]; then
    echo "[flutter-env] no $name started by the script"
    return 0
  fi
  pid="$(cat "$pid_file")"
  if [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null; then
    kill "$pid"
    local deadline=$((SECONDS + STOP_TIMEOUT))
    while kill -0 "$pid" 2>/dev/null; do
      if ((SECONDS >= deadline)); then
        kill -9 "$pid" 2>/dev/null || true
        break
      fi
      sleep 1
    done
    echo "[flutter-env] stopped $name (pid $pid)"
  else
    echo "[flutter-env] $name (pid $pid) is not running"
  fi
  rm -f "$pid_file"
}

stop_recorded appium
stop_recorded emulator
