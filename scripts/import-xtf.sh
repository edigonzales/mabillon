#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=interlis-tools-env.sh
source "${SCRIPT_DIR}/interlis-tools-env.sh"

require_tool_jar "ili2pg" "$ILI2PG_JAR"
if [[ "$#" -ne 1 ]]; then
  echo "Usage: scripts/import-xtf.sh path/to/data.xtf" >&2
  exit 2
fi

XTF="$1"
cd "$MABILLON_ROOT"
[[ -f "$XTF" ]] || { echo "XTF file not found: $XTF" >&2; exit 1; }
"${SCRIPT_DIR}/validate-xtf.sh" "$XTF"

mapfile -t connection_args < <(ili2pg_connection_args)
exec java -jar "$ILI2PG_JAR" \
  --import \
  --dbschema mabillon \
  --importTid \
  --importBid \
  --modeldir "$MABILLON_MODEL_DIR" \
  "${connection_args[@]}" \
  "$XTF"
