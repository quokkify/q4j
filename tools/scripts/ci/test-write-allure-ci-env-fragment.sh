#!/usr/bin/env bash
set -euo pipefail

ROOT="$(mktemp -d)"
trap 'rm -rf "${ROOT}"' EXIT

cat > "${ROOT}/os-release-2204" <<'EOF'
ID=ubuntu
VERSION_ID="22.04"
EOF

MODULE_PATH=':common-utils:core' \
OS_RELEASE_FILE="${ROOT}/os-release-2204" \
GITHUB_JOB=fixture-job \
RUNNER_NAME=fixture-runner \
bash tools/scripts/ci/write-allure-ci-env-fragment.sh legacy-prefix "${ROOT}/allure-results"

fragment="${ROOT}/allure-results/ci-env-fragment.properties"
grep -Fx 'common-utils:core.Module=common-utils:core' "${fragment}"
grep -Fx 'common-utils:core.Environment=ubuntu-22.04' "${fragment}"
grep -Fx 'common-utils:core.Ubuntu ID=ubuntu' "${fragment}"
grep -Fx 'common-utils:core.Ubuntu VERSION_ID=22.04' "${fragment}"
grep -Fx 'common-utils:core.Suite=Gradle TestNG' "${fragment}"
grep -Fx 'common-utils:core.Job=fixture-job' "${fragment}"
grep -Fx 'common-utils:core.Runner=fixture-runner' "${fragment}"
if grep -q 'legacy-prefix' "${fragment}"; then
  printf 'legacy prefix leaked into provenance\n' >&2
  exit 1
fi
if grep -q 'SECRET' "${fragment}"; then
  printf 'secret sentinel leaked into provenance\n' >&2
  exit 1
fi

cat > "${ROOT}/os-release-2404" <<'EOF'
ID=ubuntu
VERSION_ID="24.04"
EOF

MODULE_PATH=':data-utils:sql' \
OS_RELEASE_FILE="${ROOT}/os-release-2404" \
GITHUB_JOB=second-fixture-job \
RUNNER_NAME=second-fixture-runner \
bash tools/scripts/ci/write-allure-ci-env-fragment.sh another-prefix "${ROOT}/second-results"

second_fragment="${ROOT}/second-results/ci-env-fragment.properties"
grep -Fx 'data-utils:sql.Module=data-utils:sql' "${second_fragment}"
grep -Fx 'data-utils:sql.Environment=ubuntu-24.04' "${second_fragment}"
grep -Fx 'data-utils:sql.Ubuntu ID=ubuntu' "${second_fragment}"
grep -Fx 'data-utils:sql.Ubuntu VERSION_ID=24.04' "${second_fragment}"
grep -Fx 'data-utils:sql.Suite=Gradle TestNG' "${second_fragment}"
grep -Fx 'data-utils:sql.Job=second-fixture-job' "${second_fragment}"
if grep -q 'common-utils:core' "${second_fragment}"; then
  printf 'first module leaked into second provenance\n' >&2
  exit 1
fi
if grep -q '22.04' "${second_fragment}"; then
  printf 'first Ubuntu value leaked into second provenance\n' >&2
  exit 1
fi
if grep -q 'another-prefix' "${second_fragment}"; then
  printf 'legacy prefix leaked into second provenance\n' >&2
  exit 1
fi
if grep -q 'SECRET' "${second_fragment}"; then
  printf 'secret sentinel leaked into second provenance\n' >&2
  exit 1
fi

if MODULE_PATH=':unsafe/module' OS_RELEASE_FILE="${ROOT}/os-release-2204" \
  bash tools/scripts/ci/write-allure-ci-env-fragment.sh ignored "${ROOT}/rejected-results"; then
  printf 'unsafe module path was accepted\n' >&2
  exit 1
fi
