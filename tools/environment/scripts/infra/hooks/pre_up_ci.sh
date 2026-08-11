#!/bin/bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../compose_utils.sh"
init_compose_files

ensure_dynamic_port() {
  local var_name="$1"
  local label="$2"
  local current="${!var_name:-}"

  if [[ -n "$current" && "$current" != "0" ]]; then
    return 0
  fi

  local dynamic_port
  dynamic_port="$(find_free_port || true)"
  if [[ -z "$dynamic_port" ]]; then
    error "[infra] ${label}: cannot allocate free host port"
    exit 1
  fi

  printf -v "$var_name" '%s' "$dynamic_port"
  export "$var_name"
}

needs_nginx_build="false"
for profile in "$@"; do
  profile="$(echo "$profile" | xargs)"
  [[ -z "$profile" ]] && continue

  case "$profile" in
    storage)
      export MONGODB_PUBLISHED_PORT=0
      ;;
    redis)
      export REDIS_PUBLISHED_PORT=0
      ;;
    web)
      needs_nginx_build="true"
      ensure_dynamic_port SELENIUM_HUB_PUBLISHED_PORT "web hook: selenium hub"
      if [[ "${EXECUTION_MODE:-}" == "DIND" ]]; then
        grid_host="dind"
      else
        grid_host="localhost"
      fi
      export GRID_EXTERNAL_URL="http://${grid_host}:${SELENIUM_HUB_PUBLISHED_PORT}/wd/hub"
      ;;
    messaging)
      export KAFKA_EXTERNAL_HOST="$(resolve_runtime_host)"
      ensure_dynamic_port KAFKA_PUBLISHED_PORT "messaging hook: kafka"
      export KAFKA_EXTERNAL_PORT="${KAFKA_PUBLISHED_PORT}"
      ;;
    rabbitmq)
      ensure_dynamic_port RABBITMQ_PUBLISHED_PORT "rabbitmq hook: rabbitmq amqp"
      ensure_dynamic_port RABBITMQ_MANAGEMENT_PUBLISHED_PORT "rabbitmq hook: rabbitmq management"
      ;;
    *)
      :
      ;;
  esac
done

if [[ "$needs_nginx_build" == "true" ]]; then
  info "[infra] building nginx image for CI"
  compose_cmd build nginx
fi
