#!/bin/bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../compose_utils.sh"
init_compose_files

info "[infra] sql hook: set postgres url"
require_port="false"
if [[ "${CI:-}" == "true" ]]; then
  require_port="true"
fi

if ! port="$(resolve_published_port postgres 5432 5432 "${require_port}")"; then
  error "[infra] sql hook: cannot resolve postgres port"
  exit 1
fi

host="$(select_runtime_host_for_port "$(resolve_runtime_host)" "${port}")"
echo "POSTGRES_JDBC_URL=jdbc:postgresql://${host}:${port}/q4j" > tools/environment/.postgres.env
