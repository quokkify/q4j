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
  http_host="dind"
  proxy_browser_host="$(getent ahostsv4 dind | awk 'NR == 1 { print $1 }')"
  proxy_host="$(hostname -I | awk '{ print $1 }')"
elif [[ "${CI:-}" == "true" ]]; then
  project_name="${COMPOSE_PROJECT_NAME:-docker}"
  network_name="${project_name}_default"
  http_host="localhost"
  proxy_browser_host="$(docker network inspect "$network_name" --format '{{(index .IPAM.Config 0).Gateway}}')"
  proxy_host="$proxy_browser_host"
else
  http_host="localhost"
  proxy_browser_host="host.docker.internal"
  proxy_host="host.docker.internal"
fi

if [[ -z "$http_host" || -z "$proxy_browser_host" || -z "$proxy_host" ]]; then
  error "[infra] web hook: cannot resolve shared nginx/proxy hosts"
  exit 1
fi

{
  if [[ "${CI:-}" == "true" ]]; then
    echo "NGINX_BASE_URL=http://nginx:80"
    echo "DOWNLOAD_BROWSER_BASE_URL=http://nginx:80"
  else
    echo "NGINX_BASE_URL=http://${http_host}:${port}"
    echo "DOWNLOAD_BROWSER_BASE_URL=http://host.docker.internal:${port}"
  fi
  echo "DOWNLOAD_HTTP_BASE_URL=http://${http_host}:${port}"
  echo "DOWNLOAD_PROXY_BASE_URL=http://${proxy_browser_host}:${port}"
  echo "BROWSER_PROXY_HOST=${proxy_host}"
} > tools/environment/.nginx.env
