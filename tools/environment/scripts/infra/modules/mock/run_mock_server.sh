#!/bin/bash

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../../compose_utils.sh"
init_compose_files

echo "🚀 Starting MockServer container..."

compose_cmd up -d --quiet-pull mock-server

sleep=2
max_count=30
count=0
status=0

while true; do
  if [[ "${CI:-}" == "true" ]]; then
    container_id="$(compose_cmd ps -q mock-server | head -n1 || true)"
    if [[ -n "$container_id" ]]; then
      status=$(docker run --rm --network "container:${container_id}" curlimages/curl:8.21.0@sha256:7c12af72ceb38b7432ab85e1a265cff6ae58e06f95539d539b654f2cfa64bb13 \
        -o /dev/null -s -w "%{http_code}" http://localhost:1080/mockserver/dashboard || true)
    else
      status=000
    fi
  else
    status=$(curl -o /dev/null -s -w "%{http_code}" http://localhost:1080/mockserver/dashboard)
  fi

  if [[ "$status" -eq 200 ]]; then
    echo "✅ MockServer container started successfully"
    break
  fi

  echo "⏳ Waiting for MockServer... ${count}s elapsed, status code: $status"
  sleep "$sleep"
  count=$((count + sleep))

  if [[ "$count" -gt "$max_count" ]]; then
    echo -e "\e[1;31m❌ MockServer did not start within ${max_count}s (http://localhost:1080)\e[0m"
    exit 1
  fi
done

get_mockserver_base_url() {
  local host
  host="$(resolve_runtime_host)"
  local port_line
  port_line="$(compose_cmd port mock-server 1080 | head -n1 || true)"
  if [[ -n "$port_line" ]]; then
    local port="${port_line##*:}"
    echo "http://${host}:${port}"
  else
    echo "http://${host}:1080"
  fi
}

MOCKSERVER_BASE_URL="$(get_mockserver_base_url)"
echo "BASE_API_URL=${MOCKSERVER_BASE_URL}" > tools/environment/.mock-server.env
