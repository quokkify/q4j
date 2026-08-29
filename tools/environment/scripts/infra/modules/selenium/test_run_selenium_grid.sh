#!/bin/bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
script="${script_dir}/run_selenium_grid.sh"
grep -Fq 'repo_root="$(pwd)" || exit 1' "$script"
grep -Fq 'chmod 0644 "$tmp_config"' "$script"
grep -Fq 'trap '\''rm -f "$tmp_config"'\'' EXIT' "$script"

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT
tmp_config="${tmp_dir}/config.toml"
printf '%s\n' 'generated config' > "$tmp_config"
chmod 0644 "$tmp_config"
generated="${tmp_dir}/generated/config.toml"
mkdir -p "${generated%/*}"
mv "$tmp_config" "$generated"
[[ "$(stat -c '%a' "$generated")" == "644" ]]
test -r "$generated"
mode="$(stat -c '%a' "$generated")"
[[ "$mode" == "644" ]]
rm -f "$generated"
[[ ! -e "$generated" ]]
printf '%s\n' 'selenium grid config permission/cleanup checks passed'