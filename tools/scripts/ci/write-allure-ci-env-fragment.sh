#!/usr/bin/env bash
# Writes non-secret CI metadata into an Allure result directory before artifact upload.
# Usage: write-allure-ci-env-fragment.sh <Prefix> <allure_results_dir>
set -euo pipefail

PREFIX="${1:?prefix e.g. common-core}"
ALLURE_RESULTS_DIR="${2:?allure results dir}"

mkdir -p "${ALLURE_RESULTS_DIR}"
OUT="${ALLURE_RESULTS_DIR}/ci-env-fragment.properties"

write_kv() {
  local key="$1"
  local val="$2"
  val="${val//$'\r'/}"
  val="${val//$'\n'/ }"
  printf '%s.%s=%s\n' "${PREFIX}" "${key}" "${val}" >> "${OUT}"
}

write_kv_nonempty() {
  local key="$1"
  local val="${2:-}"
  [[ -n "${val}" ]] || return 0
  write_kv "${key}" "${val}"
}

: > "${OUT}"
write_kv "Suite" "Gradle TestNG"
write_kv "Job" "${GITHUB_JOB:-local}"
write_kv_nonempty "Module" "${MODULE_PATH:-}"
write_kv_nonempty "Profile" "${QUOKKIFY_TEST_PROFILE:-}"
write_kv_nonempty "Runner" "${RUNNER_NAME:-}"
