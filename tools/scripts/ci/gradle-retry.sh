#!/usr/bin/env bash
# Runs ./gradlew with the given task names/flags and retries only when the
# failure output shows an HTTP 429 (Too Many Requests) response from a
# repository. Any other failure (404, DNS/connection error, compilation
# error, failing test, etc.) is not retried and fails immediately.
# Usage: gradle-retry.sh <gradle task/flags...>
# Env: GRADLE_RETRY_MAX_ATTEMPTS (default 3), GRADLE_RETRY_INITIAL_DELAY_SECONDS (default 30)
set -uo pipefail

max_attempts="${GRADLE_RETRY_MAX_ATTEMPTS:-3}"
delay="${GRADLE_RETRY_INITIAL_DELAY_SECONDS:-30}"
attempt=1

while true; do
  output="$(mktemp)"

  ./gradlew "$@" --no-daemon --console=plain --stacktrace 2>&1 | tee "$output"
  status=$?

  if [ "$status" -eq 0 ]; then
    rm -f "$output"
    exit 0
  fi

  if [ "$attempt" -ge "$max_attempts" ]; then
    rm -f "$output"
    exit "$status"
  fi

  if ! grep -qE 'status code 429|429 Too Many Requests|Too Many Requests' "$output"; then
    rm -f "$output"
    exit "$status"
  fi

  jitter=$(( RANDOM % (delay / 3 + 1) ))
  sleep_for=$(( delay + jitter ))
  echo "::warning::Gradle failed with a transient Maven Central resolution error (attempt ${attempt}/${max_attempts}). Retrying in ${sleep_for}s..."
  rm -f "$output"
  sleep "$sleep_for"

  attempt=$(( attempt + 1 ))
  delay=$(( delay * 3 ))
done
