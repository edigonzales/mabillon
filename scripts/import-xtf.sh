#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=interlis-tools-env.sh
source "${SCRIPT_DIR}/interlis-tools-env.sh"

if [[ "$#" -ne 1 ]]; then
  echo "Usage: scripts/import-xtf.sh path/to/data.xtf" >&2
  exit 2
fi

XTF="$1"
cd "$MABILLON_ROOT"
[[ -f "$XTF" ]] || { echo "XTF file not found: $XTF" >&2; exit 1; }
run_interlis_tool import-xtf "$XTF"
