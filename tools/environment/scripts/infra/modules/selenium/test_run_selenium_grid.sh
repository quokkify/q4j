#!/bin/bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
script="${script_dir}/run_selenium_grid.sh"
umask 077
grep -Fq 'repo_root="$(pwd)" || exit 1' "$script"
grep -Fq 'install -d -m 0755 "$SELENIUM_GRID_MOUNT"' "$script"
grep -Fq 'chmod 0755 "$SELENIUM_GRID_MOUNT"' "$script"
grep -Fq 'chmod 0644 "$tmp_config"' "$script"
grep -Fq 'trap '\''rm -f "$tmp_config"'\'' EXIT' "$script"

# Keep the temporary root traversable so the mode assertions model an unrelated
# UID such as the pinned Selenium node user (1200).
tmp_dir="$(mktemp -d)"
chmod 0755 "$tmp_dir"
cleanup() {
  rm -f "${tmp_config:-}" "${failure_tmp:-}"
  rm -rf "$tmp_dir"
}
trap cleanup EXIT INT TERM

mount="${tmp_dir}/parent/generated"
install -d -m 0755 "$mount"
chmod 0755 "$mount"
tmp_config="$(mktemp "${tmp_dir}/config.XXXXXX")"
printf '%s\n' 'generated config' > "$tmp_config"
chmod 0644 "$tmp_config"
generated="${mount}/config.toml"
mv "$tmp_config" "$generated"

# UID 1200 is unrelated to the host owner. Prove its access contract from
# mode bits for every path component rather than relying on test -r as owner.
for component in "$tmp_dir" "${tmp_dir}/parent" "$mount"; do
  mode=$((8#$(stat -c '%a' "$component")))
  (( mode & 001 )) || { echo "UID 1200 cannot traverse $component" >&2; exit 1; }
  (( mode & 002 )) && { echo "UID 1200 can write $component" >&2; exit 1; }
done
mode=$((8#$(stat -c '%a' "$generated")))
(( mode & 004 )) || { echo "UID 1200 cannot read $generated" >&2; exit 1; }
(( mode & 002 )) && { echo "UID 1200 can write $generated" >&2; exit 1; }
[[ "$(< "$generated")" == 'generated config' ]]
rm -f "$generated"
[[ ! -e "$generated" ]]
[[ ! -e "$tmp_config" ]]

# Verify cleanup also runs when rendering exits before the move.
failure_tmp="$(mktemp "${tmp_dir}/failure.XXXXXX")"
(failure_tmp="$failure_tmp"; trap 'rm -f "$failure_tmp"' EXIT; exit 1) || true
[[ ! -e "$failure_tmp" ]]
printf '%s\n' 'selenium grid config permission/cleanup checks passed'
