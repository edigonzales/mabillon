#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=interlis-tools-env.sh
source "${SCRIPT_DIR}/interlis-tools-env.sh"

require_tool_jar "ilivalidator" "$ILIVALIDATOR_JAR"

cd "$MABILLON_ROOT"
if [[ "$#" -gt 0 ]]; then
  xtf_files=("$@")
else
  mapfile -t xtf_files < <(find model/testdata -maxdepth 1 -type f -name '*.xtf' -print | sort)
fi

if [[ "${#xtf_files[@]}" -eq 0 ]]; then
  echo "No XTF files found under model/testdata" >&2
  exit 1
fi

for xtf in "${xtf_files[@]}"; do
  [[ -f "$xtf" ]] || { echo "XTF file not found: $xtf" >&2; exit 1; }
  java -jar "$ILIVALIDATOR_JAR" \
    --models "$MABILLON_MODEL_NAME" \
    --modeldir "$MABILLON_MODEL_DIR" \
    "$xtf"
done
