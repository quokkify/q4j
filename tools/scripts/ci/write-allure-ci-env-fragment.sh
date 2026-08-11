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

java_version="${ALLURE_JAVA_VERSION:-}"
if [[ -z "${java_version}" ]] && command -v java >/dev/null 2>&1; then
  java_version_output="$(java -version 2>&1 || true)"
  java_version="${java_version_output%%$'\n'*}"
  if [[ "${java_version}" =~ version[[:space:]]+\"([^\"]+)\" ]]; then
    java_version="${BASH_REMATCH[1]}"
  fi
fi

gradle_version="${ALLURE_GRADLE_VERSION:-}"
if [[ -z "${gradle_version}" ]]; then
  gradle_command="${GRADLE_COMMAND:-./gradlew}"
  if [[ -x "${gradle_command}" ]] || command -v "${gradle_command}" >/dev/null 2>&1; then
    gradle_version_output="$("${gradle_command}" --version --no-daemon 2>/dev/null || true)"
    while IFS= read -r line; do
      if [[ "${line}" =~ ^Gradle[[:space:]]+(.+)$ ]]; then
        gradle_version="${BASH_REMATCH[1]}"
        break
      fi
    done <<< "${gradle_version_output}"
  fi
fi

: > "${OUT}"
write_kv "Suite" "Gradle TestNG"
write_kv "Job" "${GITHUB_JOB:-local}"
write_kv_nonempty "Module" "${MODULE_PATH:-}"
write_kv_nonempty "Profile" "${QUOKKIFY_TEST_PROFILE:-}"
write_kv_nonempty "Runner" "${RUNNER_NAME:-}"
write_kv_nonempty "Java" "${java_version}"
write_kv_nonempty "Gradle" "${gradle_version}"
