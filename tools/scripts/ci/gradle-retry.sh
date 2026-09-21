#!/usr/bin/env bash
# Run a Gradle command with bounded recovery for transient resolution failures.
# 429/403 failures retain exponential retry; a dependency not-found gets one refresh.
set -uo pipefail

command_string="${GRADLE_RETRY_COMMAND:-}"
max_attempts="${GRADLE_RETRY_MAX_ATTEMPTS:-3}"
delay="${GRADLE_RETRY_INITIAL_DELAY_SECONDS:-30}"

if [[ -z "$command_string" ]]; then
  echo "gradle-retry.sh: GRADLE_RETRY_COMMAND is required" >&2
  exit 2
fi
if ! [[ "$max_attempts" =~ ^[1-9][0-9]*$ ]]; then
  echo "gradle-retry.sh: GRADLE_RETRY_MAX_ATTEMPTS must be a positive integer, got '${max_attempts}'" >&2
  exit 2
fi
if ! [[ "$delay" =~ ^[1-9][0-9]*$ ]]; then
  echo "gradle-retry.sh: GRADLE_RETRY_INITIAL_DELAY_SECONDS must be a positive integer, got '${delay}'" >&2
  exit 2
fi

attempt=1
refresh_attempted=0
while true; do
  output="$(mktemp)"
  bash -euo pipefail -c "$command_string" 2>&1 | tee "$output"
  status=${PIPESTATUS[0]}

  if [[ "$status" -eq 0 ]]; then
    rm -f "$output"
    exit 0
  fi

  is_repository_failure=0
  grep -qE 'Could not (GET|HEAD|resolve)' "$output" && is_repository_failure=1
  is_429=0
  grep -qE 'status code 429|429 Too Many Requests|Too Many Requests' "$output" && is_429=1

  # A 403 is ambiguous (it can be a persistent permission failure), so only
  # retry the Gradle repository form: an HTTP GET/HEAD URL followed by the
  # repository's explicit 403 status diagnostic. This excludes bare
  # "Forbidden" text and unrelated "Could not resolve" failures.
  is_403=0
  if tr '\n' ' ' < "$output" | grep -qE "Could not (GET|HEAD) ['\"]?https?://[^[:space:]'\">]+['\"]?[^.]*status code 403"; then
    is_403=1
  fi

  if [[ "$is_repository_failure" -eq 1 && ( "$is_429" -eq 1 || "$is_403" -eq 1 ) && "$attempt" -lt "$max_attempts" ]]; then
    jitter=$(( RANDOM % (delay / 3 + 1) ))
    sleep_for=$(( delay + jitter ))
    status_code=429
    [[ "$is_403" -eq 1 ]] && status_code=403
    echo "::warning::Gradle failed with a transient repository ${status_code} (attempt ${attempt}/${max_attempts}). Retrying in ${sleep_for}s..."
    rm -f "$output"
    sleep "$sleep_for"
    attempt=$((attempt + 1))
    delay=$((delay * 3))
    continue
  fi

  # Gradle's dependency-resolution not-found report includes both the module
  # coordinates and a repository search section. Do not treat unrelated DSL
  # errors such as "Could not find method implementation()" as resolvable.
  is_dependency_not_found=0
  if grep -qE 'Could not find [^[:space:]]+:[^[:space:]]+:[^[:space:]]+' "$output" \
    && grep -qE 'Searched in( the following locations)?:' "$output"; then
    is_dependency_not_found=1
  fi
  if [[ "$is_dependency_not_found" -eq 1 && "$refresh_attempted" -eq 0 && "$command_string" != *--refresh-dependencies* ]]; then
    refresh_attempted=1
    command_string="$command_string --refresh-dependencies"
    echo "::warning::Gradle dependency resolution failed; retrying once with --refresh-dependencies."
    rm -f "$output"
    continue
  fi

  rm -f "$output"
  exit "$status"
done
