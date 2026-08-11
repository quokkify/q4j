#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

docker compose \
  -f "${SCRIPT_DIR}/docker-compose.runner.yml" \
  --env-file "${SCRIPT_DIR}/.env" \
  down -v
