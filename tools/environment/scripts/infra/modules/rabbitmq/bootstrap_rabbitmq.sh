#!/bin/bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../../compose_utils.sh"
init_compose_files

rabbit_user="${RABBITMQ_USER:-guest}"
rabbit_password="${RABBITMQ_PASSWORD:-guest}"
rabbit_vhost="${RABBITMQ_VHOST:-/}"

info "[rabbitmq] waiting for health check"
if ! wait_until 60 2 compose_cmd exec -T rabbitmq rabbitmq-diagnostics -q ping >/dev/null 2>&1; then
  error "[rabbitmq] rabbitmq is not ready after timeout"
  compose_cmd logs --tail=200 rabbitmq || true
  exit 1
fi

require_port="false"
if [[ "${CI:-}" == "true" ]]; then
  require_port="true"
fi

amqp_port="$(resolve_published_port rabbitmq 5672 5672 "${require_port}" || true)"
management_port="$(resolve_published_port rabbitmq 15672 15672 "${require_port}" || true)"

if [[ -z "$amqp_port" || -z "$management_port" ]]; then
  error "[rabbitmq] cannot resolve exposed ports for rabbitmq"
  exit 1
fi

host="$(select_runtime_host_for_port "$(resolve_runtime_host)" "${amqp_port}")"
if ! wait_until 30 1 is_tcp_reachable "${host}" "${amqp_port}"; then
  error "[rabbitmq] rabbitmq endpoint is not reachable on ${host}:${amqp_port}"
  compose_cmd logs --tail=200 rabbitmq || true
  exit 1
fi

rabbit_url_vhost="${rabbit_vhost#/}"
if [[ -z "$rabbit_url_vhost" ]]; then
  rabbit_url_vhost="%2F"
fi

rabbit_url="amqp://${rabbit_user}:${rabbit_password}@${host}:${amqp_port}/${rabbit_url_vhost}"
rabbit_management_url="http://${host}:${management_port}"

cat > tools/environment/.rabbitmq.env <<ENV
RABBIT_HOST=${host}
RABBIT_PORT=${amqp_port}
RABBIT_USER=${rabbit_user}
RABBIT_PASSWORD=${rabbit_password}
RABBIT_VIRTUAL_HOST=${rabbit_vhost}
RABBIT_URL=${rabbit_url}
RABBIT_MANAGEMENT_URL=${rabbit_management_url}
ENV

mkdir -p integrations/rabbitmq/src/test/resources/local_resources
cat > integrations/rabbitmq/src/test/resources/local_resources/rabbit.properties <<ENV
RABBIT_HOST=${host}
RABBIT_PORT=${amqp_port}
RABBIT_USER=${rabbit_user}
RABBIT_PASSWORD=${rabbit_password}
RABBIT_VIRTUAL_HOST=${rabbit_vhost}
ENV

info "[rabbitmq] endpoint: ${host}:${amqp_port}"
info "[rabbitmq] management url: ${rabbit_management_url}"
info "[rabbitmq] env file written: tools/environment/.rabbitmq.env"
info "[rabbitmq] owner config written: integrations/rabbitmq/src/test/resources/local_resources/rabbit.properties"
