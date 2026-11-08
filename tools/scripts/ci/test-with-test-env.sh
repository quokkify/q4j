#!/usr/bin/env bash
set -euo pipefail

SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/with-test-env.sh"
ROOT="$(mktemp -d)"
trap 'rm -rf "${ROOT}"' EXIT
mkdir -p "${ROOT}/tools/environment"

cat > "${ROOT}/tools/environment/.kafka.env" <<'EOF'
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
EOF
cat > "${ROOT}/tools/environment/.nginx.env" <<'EOF'
NGINX_BASE_URL=http://nginx:8080
EOF

cd "${ROOT}"

output="$(QUOKKIFY_TEST_PROFILE=web bash "${SCRIPT}" bash -c 'printf "%s|%s|%s|%s\n" "$KAFKA_SERVER_ADDRESS" "$BASE_URL" "$1" "$2"' _ :module:test --refresh-dependencies)"
if [[ "${output}" != "kafka:9092|http://nginx:8080|:module:test|--refresh-dependencies" ]]; then
  printf 'unexpected environment or arguments: %s\n' "${output}" >&2
  exit 1
fi

rm "${ROOT}/tools/environment/.nginx.env"
if QUOKKIFY_TEST_PROFILE=web bash "${SCRIPT}" true 2> "${ROOT}/stderr"; then
  printf 'web profile without NGINX_BASE_URL must fail\n' >&2
  exit 1
fi
grep -qFx 'NGINX_BASE_URL is not set for web profile' "${ROOT}/stderr"

status=0
QUOKKIFY_TEST_PROFILE=none bash "${SCRIPT}" bash -c 'exit 7' || status=$?
if [[ "${status}" -ne 7 ]]; then
  printf 'wrapped command exit status was not propagated: %s\n' "${status}" >&2
  exit 1
fi
