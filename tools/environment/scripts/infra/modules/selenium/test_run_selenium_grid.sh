#!/bin/bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
script="${script_dir}/run_selenium_grid.sh"

grep -Fq 'repo_root="$(pwd)" || exit 1' "$script"
grep -Fq 'export SELENIUM_GRID_MOUNT="${repo_root}/tools/environment/assets/selenium-grid/generated"' "$script"
grep -Fq 'chmod 0644 "$tmp_config"' "$script"
grep -Fq 'trap '\''rm -f "$tmp_config"'\'' EXIT' "$script"

tmp_root="$(mktemp -d)"
tmp_bin="${tmp_root}/bin"
tmp_tmp="${tmp_root}/tmp"
cleanup() {
  rm -rf "$tmp_root"
}
trap cleanup EXIT INT TERM
install -d -m 0755 "$tmp_bin" "$tmp_tmp"

# Run the production script in a minimal isolated repository tree. The mocks only
# satisfy Docker/Grid readiness; config rendering and filesystem operations are real.
install -d -m 0755 \
  "${tmp_root}/tools/environment/scripts/infra/modules/selenium" \
  "${tmp_root}/tools/environment/scripts/infra" \
  "${tmp_root}/tools/environment/assets/selenium-grid"
cp "$script" "${tmp_root}/tools/environment/scripts/infra/modules/selenium/run_selenium_grid.sh"
cp "${script_dir}/../../compose_utils.sh" "${tmp_root}/tools/environment/scripts/infra/compose_utils.sh"
cp "${script_dir}/../../../../assets/selenium-grid/config.toml" \
  "${tmp_root}/tools/environment/assets/selenium-grid/config.toml"

cat > "${tmp_bin}/docker" <<'MOCK'
#!/bin/bash
if [[ "${1:-}" == compose ]]; then
  for arg in "$@"; do
    if [[ "$arg" == port ]]; then
      printf '%s\n' '0.0.0.0:4444'
      exit 0
    fi
  done
fi
exit 0
MOCK
cat > "${tmp_bin}/curl" <<'MOCK'
#!/bin/bash
printf '%s\n' '{"value":{"ready":true}}'
MOCK
chmod 0755 "${tmp_bin}/docker" "${tmp_bin}/curl"

run_env=(PATH="${tmp_bin}:$PATH" TMPDIR="$tmp_tmp" COMPOSE_PROJECT_NAME=isolated-grid)
(
  cd "$tmp_root"
  env -u CI "${run_env[@]}" ./tools/environment/scripts/infra/modules/selenium/run_selenium_grid.sh
)

generated="${tmp_root}/tools/environment/assets/selenium-grid/generated/config.toml"
[[ -f "$generated" ]]
[[ "$(< "$generated")" == *'isolated-grid_default'* ]]
[[ "$(stat -c '%a' "$generated")" == 644 ]]
[[ "$(stat -c '%a' "${generated%/*}")" == 755 ]]
[[ "$(< "${tmp_root}/tools/environment/.selenium-grid.env")" == 'BROWSER_REMOTE_URL=http://localhost:4444/wd/hub' ]]

# Force render_config to fail and prove the production trap removes its temp file.
fail_bin="${tmp_root}/fail-bin"
install -d -m 0755 "$fail_bin"
cat > "${fail_bin}/sed" <<'MOCK'
#!/bin/bash
exit 1
MOCK
chmod 0755 "${fail_bin}/sed"
if (
  cd "$tmp_root"
  env -u CI PATH="$fail_bin:${tmp_bin}:$PATH" TMPDIR="$tmp_tmp" \
    ./tools/environment/scripts/infra/modules/selenium/run_selenium_grid.sh
); then
  echo 'render failure unexpectedly succeeded' >&2
  exit 1
fi
shopt -s nullglob
leftovers=("${tmp_tmp}"/*)
[[ "${#leftovers[@]}" -eq 0 ]]
printf '%s\n' 'production Selenium Grid script checks passed'
