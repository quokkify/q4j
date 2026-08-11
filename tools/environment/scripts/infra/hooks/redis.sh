#!/bin/bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../compose_utils.sh"
init_compose_files

info "[infra] redis hook: waiting for PING"
if ! wait_until 30 1 compose_cmd exec -T redis redis-cli ping >/dev/null 2>&1; then
  warning "[infra] redis hook: PING not ready after timeout"
fi

info "[infra] redis hook: set redis host and port"
require_port="false"
if [[ "${CI:-}" == "true" ]]; then
  require_port="true"
fi

if ! port="$(resolve_published_port redis 6379 6379 "${require_port}")"; then
  error "[infra] redis hook: cannot resolve redis port"
  exit 1
fi

host="$(select_runtime_host_for_port "$(resolve_runtime_host)" "${port}")"
echo "REDIS_HOST=${host}" > tools/environment/.redis.env
echo "REDIS_PORT=${port}" >> tools/environment/.redis.env
