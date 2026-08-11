#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUNNER_COUNT="${RUNNER_COUNT:-3}"

if [[ ! -f "${SCRIPT_DIR}/.env" ]]; then
  echo "[runner] Missing .env. Copy .env.example to .env and fill values."
  exit 1
fi

if ! [[ "${RUNNER_COUNT}" =~ ^[1-9][0-9]*$ ]]; then
  echo "[runner] RUNNER_COUNT must be a positive integer, got: ${RUNNER_COUNT}"
  exit 1
fi

docker compose \
  -f "${SCRIPT_DIR}/docker-compose.runner.yml" \
  --env-file "${SCRIPT_DIR}/.env" \
  up -d --scale "runner=${RUNNER_COUNT}"
