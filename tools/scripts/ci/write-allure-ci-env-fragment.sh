#!/usr/bin/env bash
# Writes non-secret CI metadata into an Allure result directory before artifact upload.
# Usage: write-allure-ci-env-fragment.sh <legacy-prefix> <allure_results_dir>
set -euo pipefail

: "${1:?legacy prefix e.g. common-core}"
ALLURE_RESULTS_DIR="${2:?allure results dir}"

mkdir -p "${ALLURE_RESULTS_DIR}"
OUT="${ALLURE_RESULTS_DIR}/ci-env-fragment.properties"

write_kv() {
  local key="$1"
  local val="$2"
  val="${val//$'\r'/}"
  val="${val//$'\n'/ }"
  printf '%s=%s\n' "${key}" "${val}" >> "${OUT}"
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
module_name="${MODULE_PATH#:}"
module_name="${module_name:-unknown-module}"
if [[ ! "${module_name}" =~ ^[A-Za-z0-9][A-Za-z0-9._:-]*$ ]]; then
  printf 'Unsupported module path for Allure provenance: %s\n' "${module_name}" >&2
  exit 1
fi

os_release="${OS_RELEASE_FILE:-/etc/os-release}"
ubuntu_id="unknown"
ubuntu_version_id="unknown"
if [[ -r "${os_release}" ]]; then
  ubuntu_id="$(awk -F= '$1 == "ID" {gsub(/^"|"$/, "", $2); print $2; exit}' "${os_release}")"
  ubuntu_version_id="$(awk -F= '$1 == "VERSION_ID" {gsub(/^"|"$/, "", $2); print $2; exit}' "${os_release}")"
  ubuntu_id="${ubuntu_id:-unknown}"
  ubuntu_version_id="${ubuntu_version_id:-unknown}"
fi
if [[ ! "${ubuntu_id}" =~ ^[A-Za-z0-9._-]+$ || ! "${ubuntu_version_id}" =~ ^[0-9]+([.][0-9]+)*$ ]]; then
  printf 'Unsupported OS metadata for Allure provenance: ID=%s VERSION_ID=%s\n' \
    "${ubuntu_id}" "${ubuntu_version_id}" >&2
  exit 1
fi

# Keep the legacy positional argument accepted while emitting the versioned,
# module-scoped provenance contract consumed by the pinned report action.
write_kv "Module" "${module_name}"
write_kv "${module_name}.Environment" "${ubuntu_id}-${ubuntu_version_id}"
write_kv "${module_name}.Ubuntu ID" "${ubuntu_id}"
write_kv "${module_name}.Ubuntu VERSION_ID" "${ubuntu_version_id}"
write_kv "${module_name}.Suite" "Gradle TestNG"
write_kv "${module_name}.Job" "${GITHUB_JOB:-local}"
write_kv_nonempty "${module_name}.Profile" "${QUOKKIFY_TEST_PROFILE:-}"
write_kv_nonempty "${module_name}.Runner" "${RUNNER_NAME:-}"
write_kv_nonempty "${module_name}.Java" "${java_version}"
write_kv_nonempty "${module_name}.Gradle" "${gradle_version}"
