#!/usr/bin/env bash
set -euo pipefail

# Merges multiple JUnit XML files (Gradle/Maven/TestNG-compatible) into one file using junitparser.
# Usage:
#   ./tools/scripts/ci/merge_junit_reports.sh <input_root_dir> <output_file>
# Example:
#   ./tools/scripts/ci/merge_junit_reports.sh artifacts artifacts/_combined/combined-junit.xml

INPUT_ROOT="${1:-}"
OUTPUT_FILE="${2:-}"

if [[ -z "$INPUT_ROOT" || -z "$OUTPUT_FILE" ]]; then
  echo "Usage: $0 <input_root_dir> <output_file>"
  exit 2
fi

if [[ ! -d "$INPUT_ROOT" ]]; then
  echo "❌ Input root directory does not exist: $INPUT_ROOT"
  exit 2
fi

# Find all JUnit XML files Gradle usually produces.
mapfile -t FILES < <(
  find "$INPUT_ROOT" -type f -path "*/build/test-results/*/TEST-*.xml" | sort
)

if [[ "${#FILES[@]}" -eq 0 ]]; then
  echo "❌ No JUnit XML files found under: $INPUT_ROOT/**/build/test-results/**/TEST-*.xml"
  exit 1
fi

echo "✅ Found ${#FILES[@]} JUnit XML files"

# Ensure pip is available and install junitparser (CI tool).
python3 -m pip install --upgrade --disable-pip-version-check junitparser >/dev/null

OUT_DIR="$(dirname "$OUTPUT_FILE")"
mkdir -p "$OUT_DIR"

# Merge all XMLs into one.
junitparser merge "${FILES[@]}" "$OUTPUT_FILE"

echo "✅ Merged into: $OUTPUT_FILE"
