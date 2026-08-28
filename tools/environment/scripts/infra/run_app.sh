#!/bin/bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/compose_utils.sh"

if [[ "${CI:-}" == "true" ]]; then
  export GRADLE_OPTS="-Dorg.gradle.console=plain"
else
  export GRADLE_OPTS="-Dorg.gradle.console=rich"
fi

PROFILE="${1:-none}"

if [[ "$PROFILE" == "none" || -z "$PROFILE" ]]; then
  echo "[infra] profile=none -> nothing to start"
  exit 0
fi

PROFILES_ARGS=()
IFS=',' read -ra TOKENS <<<"$PROFILE"
for profile in "${TOKENS[@]}"; do
  profile="$(echo "$profile" | xargs)" # trim
  [[ -n "$profile" ]] && PROFILES_ARGS+=("--profile" "$profile")
done

init_compose_files

echo "[infra] docker compose up: ${PROFILES_ARGS[*]}"
source ./tools/environment/scripts/infra/before_compose.sh "${TOKENS[@]}"

compose_cmd "${PROFILES_ARGS[@]}" up -d --quiet-pull

source ./tools/environment/scripts/infra/after_health.sh "${TOKENS[@]}"
