#!/bin/bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../../compose_utils.sh"
init_compose_files

CONFIG_PATH="tools/environment/assets/selenium-grid/config.toml"
if [[ -d "$CONFIG_PATH" ]]; then
  echo "[selenium-grid] config path is a directory: $CONFIG_PATH"
  if [[ "${CI:-}" == "true" ]]; then
    rm -rf "$CONFIG_PATH"
  else
    exit 1
  fi
fi

if [[ ! -f "$CONFIG_PATH" ]]; then
  echo "[selenium-grid] missing config file: $CONFIG_PATH"
  exit 1
fi

project_name="${COMPOSE_PROJECT_NAME:-docker}"
selenium_network="${project_name}_default"

render_config() {
  local src="$1"
  local dst="$2"
  sed -e "s/__NETWORK__/${selenium_network}/g" "$src" > "$dst"
}

if [[ "${CI:-}" == "true" ]]; then
  SELENIUM_GRID_MOUNT="selenium-grid-config-${project_name}"
  echo "[selenium-grid] preparing volume ${SELENIUM_GRID_MOUNT}"
  docker volume create "${SELENIUM_GRID_MOUNT}" >/dev/null
  tmp_config="$(mktemp)"
  render_config "$CONFIG_PATH" "$tmp_config"
  # renovate: datasource=docker depName=busybox
  cat "$tmp_config" | docker run --rm -i -v "${SELENIUM_GRID_MOUNT}":/opt/selenium/config.d busybox:1.38.0@sha256:dc2d74b28e4cf8984fa52af1f39bc7c3d9c73760b41a74d629f5d11b1ab28616 \
    sh -c "mkdir -p /opt/selenium/config.d && cat > /opt/selenium/config.d/config.toml"
  rm -f "$tmp_config"
  export SELENIUM_GRID_MOUNT
else
  export SELENIUM_GRID_MOUNT="$(pwd)/tools/environment/assets/selenium-grid"
  render_config "$CONFIG_PATH" "${SELENIUM_GRID_MOUNT}/config.toml"
fi

extract_grid_image_tag() {
  local line
  line="$(grep -E '^[[:space:]]*configs[[:space:]]*=' "$CONFIG_PATH" | head -n1 || true)"
  if [[ -z "$line" ]]; then
    return
  fi
  echo "$line" \
    | sed -E 's/.*\[[[:space:]]*"([^"]+)".*/\1/' \
    | grep -E '.+/.+:.+' || true
}

extract_grid_digest() {
  local line
  line="$(grep -E '^[[:space:]]*# renovate: depName=selenium/standalone-chromium ' "$CONFIG_PATH" | head -n1 || true)"
  if [[ -z "$line" ]]; then
    return
  fi
  echo "$line" \
    | sed -E 's/.*currentDigest=(sha256:[a-f0-9]{64}).*/\1/' \
    | grep -E '^sha256:[a-f0-9]{64}$' || true
}

GRID_IMAGE_TAG="$(extract_grid_image_tag)"
GRID_IMAGE_DIGEST="$(extract_grid_digest)"
if [[ -n "$GRID_IMAGE_TAG" ]]; then
  if [[ -n "$GRID_IMAGE_DIGEST" ]]; then
    GRID_IMAGE="${GRID_IMAGE_TAG%@*}@${GRID_IMAGE_DIGEST}"
    echo "[selenium-grid] pulling grid image: ${GRID_IMAGE}"
    docker pull --quiet "$GRID_IMAGE"
  else
    if [[ ! "$GRID_IMAGE_TAG" =~ @sha256:[a-f0-9]{64}$ ]]; then
      echo "[selenium-grid] browser image must stay digest-pinned: ${GRID_IMAGE_TAG}" >&2
      exit 1
    fi
    echo "[selenium-grid] pulling grid image: ${GRID_IMAGE_TAG}"
    docker pull --quiet "$GRID_IMAGE_TAG"
  fi
fi

echo "[selenium-grid] starting selenium hub + node"
compose_cmd up -d --quiet-pull selenium-hub selenium-node-docker

get_hub_url() {
  local host="${1:-localhost}"
  local port_line
  port_line=$(compose_cmd port selenium-hub 4444 | head -n1 || true)
  if [[ -n "$port_line" ]]; then
    local port="${port_line##*:}"
    echo "http://${host}:${port}/wd/hub"
  else
    echo "http://${host}:4444/wd/hub"
  fi
}

if [[ "${CI:-}" == "true" && "${EXECUTION_MODE:-}" == "DIND" ]]; then
  remote_host="dind"
else
  remote_host="localhost"
fi
REMOTE_HUB_URL="$(get_hub_url "${remote_host}")"
HEALTHCHECK_URL="$(get_hub_url "localhost")"
STATUS_URL="${HEALTHCHECK_URL}/status"
CI_CONTAINER_STATUS_URL="http://localhost:4444/wd/hub/status"

wait_interval_in_seconds=1
max_wait_time_in_seconds=60
end_time=$((SECONDS + max_wait_time_in_seconds))
time_left=$max_wait_time_in_seconds

echo "[selenium-grid] waiting for ${STATUS_URL}"
while [ $SECONDS -lt $end_time ]; do
  if [[ "${CI:-}" == "true" ]]; then
    hub_container="$(compose_cmd ps -q selenium-hub | head -n1 || true)"
    if [[ -n "$hub_container" ]]; then
      response="$(docker run --rm --network "container:${hub_container}" curlimages/curl:8.21.0@sha256:7c12af72ceb38b7432ab85e1a265cff6ae58e06f95539d539b654f2cfa64bb13 -sL "$CI_CONTAINER_STATUS_URL" || true)"
    else
      response=""
    fi
  else
    response="$(curl -sL "$STATUS_URL" || true)"
  fi
  if echo "$response" | tr -d '\n ' | grep -q '"ready":true'; then
    echo "[selenium-grid] ready"
    break
  else
    if [[ -n "$response" ]]; then
      echo "[selenium-grid] not ready yet, ${time_left}s left, status=$(echo "$response" | tr -d '\n' | cut -c1-200)"
    else
      echo "[selenium-grid] not ready yet, ${time_left}s left, status=empty"
    fi
    sleep "$wait_interval_in_seconds"
    time_left=$((time_left - wait_interval_in_seconds))
  fi
done

if [ $SECONDS -ge $end_time ]; then
  echo "[selenium-grid] timeout after ${max_wait_time_in_seconds}s"
  compose_cmd logs --tail=200 selenium-hub selenium-node-docker || true
  exit 1
fi

echo "BROWSER_REMOTE_URL=${REMOTE_HUB_URL}" > tools/environment/.selenium-grid.env

if [[ "${CI:-}" == "true" ]]; then
  node_container="$(compose_cmd ps -q selenium-node-docker | head -n1 || true)"
  if [[ -n "$node_container" ]]; then
    nginx_port_line="$(compose_cmd port nginx 80/tcp 2>/dev/null | head -n1 || true)"
    if [[ -z "$nginx_port_line" ]]; then
      nginx_port_line="$(compose_cmd port nginx 80 2>/dev/null | head -n1 || true)"
    fi
    echo "[selenium-grid] nginx port line: ${nginx_port_line:-<empty>}"
    nginx_container="$(compose_cmd ps -q nginx | head -n1 || true)"
    if [[ -n "$nginx_container" ]]; then
      echo "[selenium-grid] waiting for nginx to respond..."
      for i in $(seq 1 30); do
        if docker run --rm --network "container:${nginx_container}" curlimages/curl:8.21.0@sha256:7c12af72ceb38b7432ab85e1a265cff6ae58e06f95539d539b654f2cfa64bb13 -sSf http://localhost/table/ >/dev/null 2>&1; then
          echo "[selenium-grid] nginx is ready"
          break
        fi
        sleep 1
      done
    fi
    if [[ -n "$nginx_port_line" ]]; then
      nginx_port="${nginx_port_line##*:}"
    else
      nginx_port="80"
    fi
    echo "[selenium-grid] probe from selenium-node-docker:"
    docker exec "$node_container" sh -lc "wget -S -O- http://host.docker.internal:${nginx_port}/table/ 2>&1 | head -c 300; echo \" rc=\$?\""
    docker exec "$node_container" sh -lc "wget -S -O- http://nginx:80/table/ 2>&1 | head -c 300; echo \" rc=\$?\""
  fi
fi
