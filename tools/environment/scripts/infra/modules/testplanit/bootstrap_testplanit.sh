#!/bin/bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../../compose_utils.sh"
init_compose_files

service_name="testplanit"
admin_email="${TESTPLANIT_ADMIN_EMAIL:-admin@example.com}"
admin_password="${TESTPLANIT_ADMIN_PASSWORD:-admin}"
project_name="${TESTPLANIT_PROJECT_NAME:-q4j}"
token_name="${TESTPLANIT_TOKEN_NAME:-q4j-ci}"
CURL_OPTS=(--connect-timeout 5 --max-time 30)

require_port="false"
if [[ "${CI:-}" == "true" ]]; then
  require_port="true"
fi
port="$(resolve_published_port "${service_name}" 3000 3300 "${require_port}" || true)"
if [[ -z "$port" ]]; then
  error "[testplanit] cannot resolve exposed port for ${service_name}"
  exit 1
fi

host="$(select_runtime_host_for_port "$(resolve_runtime_host)" "${port}")"
endpoint="http://${host}:${port}"

work_dir="$(mktemp -d)"
trap 'rm -rf "${work_dir}"' EXIT
cookie_jar="${work_dir}/cookies.txt"

# /api/health answers 200 for healthy and degraded (optional storage/search
# are not started in this profile) and 503 only when the database is down.
is_testplanit_healthy() {
  curl "${CURL_OPTS[@]}" -sS -f -o /dev/null "${endpoint}/api/health" >/dev/null 2>&1
}

info "[testplanit] waiting for health check on ${endpoint}"
if ! wait_until 90 2 is_testplanit_healthy; then
  error "[testplanit] TestPlanIt is not healthy on ${endpoint}"
  exit 1
fi

login() {
  local csrf_token status
  csrf_token="$(curl "${CURL_OPTS[@]}" -sS -f -c "${cookie_jar}" -b "${cookie_jar}" "${endpoint}/api/auth/csrf" 2>/dev/null \
    | sed -n 's/.*"csrfToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"
  [[ -z "$csrf_token" ]] && return 1
  status="$(curl "${CURL_OPTS[@]}" -sS -o /dev/null -w '%{http_code}' -c "${cookie_jar}" -b "${cookie_jar}" \
    --data-urlencode "email=${admin_email}" \
    --data-urlencode "password=${admin_password}" \
    --data-urlencode "csrfToken=${csrf_token}" \
    --data-urlencode "json=true" \
    "${endpoint}/api/auth/callback/credentials" 2>/dev/null || true)"
  [[ "$status" == "200" ]] || return 1
  curl "${CURL_OPTS[@]}" -sS -f -b "${cookie_jar}" "${endpoint}/api/auth/session" 2>/dev/null | grep -q '"user"'
}

info "[testplanit] logging in as ${admin_email}"
if ! wait_until 30 2 login; then
  error "[testplanit] admin login failed"
  exit 1
fi

token_response="$(curl "${CURL_OPTS[@]}" -sS -f -b "${cookie_jar}" \
  -H 'Content-Type: application/json' \
  --data "{\"name\":\"${token_name}\"}" \
  "${endpoint}/api/api-tokens" || true)"
token="$(printf '%s' "$token_response" \
  | sed -n 's/.*"token"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"
if [[ -z "$token" ]]; then
  error "[testplanit] API token creation failed"
  exit 1
fi

find_project_id() {
  local query
  query="$(printf '{"where":{"name":"%s","isDeleted":false},"select":{"id":true}}' "${project_name}")"
  curl "${CURL_OPTS[@]}" -sS -f -G -H "Authorization: Bearer ${token}" \
    --data-urlencode "q=${query}" \
    "${endpoint}/api/model/projects/findFirst" 2>/dev/null \
    | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*\([0-9][0-9]*\).*/\1/p'
}

project_id="$(find_project_id || true)"
if [[ -z "$project_id" ]]; then
  info "[testplanit] creating project '${project_name}'"
  user_query="$(printf '{"where":{"email":"%s"},"select":{"id":true}}' "${admin_email}")"
  user_id="$(curl "${CURL_OPTS[@]}" -sS -f -G -H "Authorization: Bearer ${token}" \
    --data-urlencode "q=${user_query}" \
    "${endpoint}/api/model/user/findFirst" \
    | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"
  if [[ -z "$user_id" ]]; then
    error "[testplanit] cannot resolve admin user id"
    exit 1
  fi
  if ! curl "${CURL_OPTS[@]}" -sS -f -o /dev/null \
      -H "Authorization: Bearer ${token}" \
      -H 'Content-Type: application/json' \
      --data "{\"data\":{\"name\":\"${project_name}\",\"createdBy\":\"${user_id}\"}}" \
      "${endpoint}/api/model/projects/create"; then
    error "[testplanit] project '${project_name}' creation failed"
    exit 1
  fi
  project_id="$(find_project_id || true)"
fi
if [[ -z "$project_id" ]]; then
  error "[testplanit] cannot resolve id of project '${project_name}'"
  exit 1
fi

(
  umask 077
  cat > tools/environment/.testplanit.env <<ENV
TESTPLANIT_API_URL=${endpoint}
TESTPLANIT_API_TOKEN=${token}
TESTPLANIT_PROJECT_ID=${project_id}
ENV
)
chmod 600 tools/environment/.testplanit.env

info "[testplanit] endpoint: ${endpoint}"
info "[testplanit] project: ${project_name} (id ${project_id})"
info "[testplanit] env file written: tools/environment/.testplanit.env"
