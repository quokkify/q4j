#!/usr/bin/env bash
# Load the compose-generated service endpoints, then run the given command.
# Wraps the module test command so gradle-retry can re-run it as one unit.
# No `set -u`: generated env files may reference unset variables, as the
# former inline workflow step (bash -eo pipefail) allowed.
set -eo pipefail

for env_file in selenium-grid mock-server mongo postgres redis nginx reportportal testplanit kafka rabbitmq; do
  if [[ -f "tools/environment/.${env_file}.env" ]]; then
    set -a
    # shellcheck source=/dev/null
    source "tools/environment/.${env_file}.env"
    set +a
  fi
done
if [[ -n "${KAFKA_BOOTSTRAP_SERVERS:-}" ]]; then
  export KAFKA_SERVER_ADDRESS="${KAFKA_BOOTSTRAP_SERVERS}"
fi
if [[ "${QUOKKIFY_TEST_PROFILE:-}" == "web" ]]; then
  if [[ -n "${NGINX_BASE_URL:-}" ]]; then
    export BASE_URL="${NGINX_BASE_URL}"
  else
    echo "NGINX_BASE_URL is not set for web profile" >&2
    exit 1
  fi
fi

exec "$@"
