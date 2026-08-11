#!/bin/bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/compose_utils.sh"
init_compose_files

PROFILE="${1:-all}"

PROFILES_ARGS=()
if [[ "$PROFILE" != "none" && -n "$PROFILE" ]]; then
  if [[ "$PROFILE" == "all" ]]; then
    PROFILES_ARGS=(--profile web --profile messaging --profile rabbitmq --profile mock --profile realtime --profile storage --profile redis --profile reporting)
  else
    IFS=',' read -ra TOKENS <<<"$PROFILE"
    for profile in "${TOKENS[@]}"; do
      profile="$(echo "$profile" | xargs)" # trim
      [[ -n "$profile" ]] && PROFILES_ARGS+=("--profile" "$profile")
    done
  fi
fi

DOWN_ARGS=(down)
if [[ "${CI:-}" == "true" ]]; then
  DOWN_ARGS+=( -v )
fi

compose_cmd \
  "${PROFILES_ARGS[@]}" \
  "${DOWN_ARGS[@]}"
