#!/bin/bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../compose_utils.sh"
init_compose_files

info "[infra] storage hook: set mongo url"
require_port="false"
if [[ "${CI:-}" == "true" ]]; then
  require_port="true"
fi

if ! port="$(resolve_published_port mongodb 27017 27017 "${require_port}")"; then
  error "[infra] storage hook: cannot resolve mongodb port"
  exit 1
fi

host="$(select_runtime_host_for_port "$(resolve_runtime_host)" "${port}")"
echo "MONGODB_URL=mongodb://${host}:${port}" > tools/environment/.mongo.env
