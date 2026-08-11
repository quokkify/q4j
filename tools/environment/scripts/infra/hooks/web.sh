#!/bin/bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../compose_utils.sh"
init_compose_files

info "[infra] web hook: start selenium grid"
./tools/environment/scripts/infra/modules/selenium/run_selenium_grid.sh

info "[infra] web hook: set nginx and browser proxy urls"
if ! port="$(resolve_published_port nginx 80 80 false)"; then
  error "[infra] web hook: cannot resolve nginx port"
  exit 1
fi

if [[ "${CI:-}" == "true" && "${EXECUTION_MODE:-}" == "DIND" ]]; then
  host="$(getent ahostsv4 dind | awk 'NR == 1 { print $1 }')"
  proxy_host="$(hostname -I | awk '{ print $1 }')"
else
  project_name="${COMPOSE_PROJECT_NAME:-docker}"
  network_name="${project_name}_default"
  host="$(docker network inspect "$network_name" --format '{{(index .IPAM.Config 0).Gateway}}')"
  proxy_host="$host"
fi

if [[ -z "$host" || -z "$proxy_host" ]]; then
  error "[infra] web hook: cannot resolve shared nginx/proxy hosts"
  exit 1
fi

{
  echo "NGINX_BASE_URL=http://${host}:${port}"
  echo "BROWSER_PROXY_HOST=${proxy_host}"
} > tools/environment/.nginx.env
