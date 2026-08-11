#!/bin/bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../../compose_utils.sh"
init_compose_files

service_name="reportportal-gateway"
admin_user="${REPORTPORTAL_ADMIN_USER:-superadmin}"
admin_password="${REPORTPORTAL_ADMIN_PASSWORD:-erebus}"

require_port="false"
if [[ "${CI:-}" == "true" ]]; then
  require_port="true"
fi
port="$(resolve_published_port "${service_name}" 8080 8084 "${require_port}" || true)"
if [[ -z "$port" ]]; then
  error "[reporting] cannot resolve exposed port for ${service_name}"
  exit 1
fi

host="$(select_runtime_host_for_port "$(resolve_runtime_host)" "${port}")"

endpoint="http://${host}:${port}"

is_reportportal_healthy() {
  if curl -sS -f "${endpoint}/ui/health" >/dev/null 2>&1 \
      && curl -sS -f "${endpoint}/uat/health" >/dev/null 2>&1 \
      && curl -sS -f "${endpoint}/api/health" >/dev/null 2>&1; then
    return 0
  fi
  return 1
}

info "[reporting] waiting for health check on ${endpoint}"
if ! wait_until 90 2 is_reportportal_healthy; then
  error "[reporting] report portal services are not healthy on ${endpoint}"
  exit 1
fi

candidate_users="${REPORTPORTAL_CANDIDATE_USERS:-${admin_user},superadmin,default}"
last_response=""
token=""
IFS=',' read -ra USERS <<<"$candidate_users"
for user in "${USERS[@]}"; do
  user="$(echo "$user" | xargs)"
  [[ -z "$user" ]] && continue
  for _ in {1..40}; do
    token_response="$(curl -sS --show-error \
      --user 'ui:uiman' \
      -H 'Content-Type: application/x-www-form-urlencoded' \
      --data-urlencode 'grant_type=password' \
      --data-urlencode "username=${user}" \
      --data-urlencode "password=${admin_password}" \
      "${endpoint}/uat/sso/oauth/token" || true)"
    last_response="$token_response"
    token="$(printf '%s' "$token_response" \
      | sed -n 's/.*"access_token"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"
    if [[ -n "$token" ]]; then
      break 2
    fi
    sleep 2
  done
done

if [[ -z "$token" ]]; then
  error "[reporting] access token is empty"
  if [[ -n "$last_response" ]]; then
    warning "[reporting] oauth response (truncated): ${last_response:0:500}"
  fi
  exit 1
fi

project_target="${REPORTPORTAL_PROJECT_NAME:-quokkify}"

is_project_ready() {
  local status
  status="$(curl -sS -o /dev/null -w '%{http_code}' \
    -H "Authorization: Bearer ${token}" \
    "${endpoint}/api/v1/project/${project_target}" 2>/dev/null || true)"
  [[ "$status" == "200" ]]
}

info "[reporting] waiting for project '${project_target}' to be ready"
if ! wait_until 90 2 is_project_ready; then
  warning "[reporting] project '${project_target}' not accessible after 180s — proceeding anyway"
fi

project_response="$(curl -sS -f \
  -H "Authorization: Bearer ${token}" \
  "${endpoint}/api/v1/project/list?page.page=1&page.size=1")"
project_name="$(printf '%s' "$project_response" | sed -n 's/.*"projectName"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"
if [[ -z "$project_name" ]]; then
  project_name="default_personal"
fi

cat > tools/environment/.reportportal.env <<ENV
REPORTPORTAL_ENDPOINT=${endpoint}
REPORTPORTAL_PROJECT=${project_name}
REPORTPORTAL_PROJECT_NAME=${project_target}
REPORTPORTAL_API_KEY=${token}
REPORTPORTAL_BEARER_TOKEN=${token}
ENV

mkdir -p integrations/reportportal/testng/src/test/resources/local_resources
cat > integrations/reportportal/testng/src/test/resources/local_resources/reportportal-test.properties <<ENV
REPORTPORTAL_ENDPOINT=${endpoint}
REPORTPORTAL_PROJECT_NAME=${project_target}
REPORTPORTAL_API_KEY=${token}
ENV

info "[reporting] endpoint: ${endpoint}"
info "[reporting] project: ${project_target}"
info "[reporting] env file written: tools/environment/.reportportal.env"
info "[reporting] owner config written: integrations/reportportal/testng/src/test/resources/local_resources/reportportal-test.properties"
