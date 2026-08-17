#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=interlis-tools-env.sh
source "${SCRIPT_DIR}/interlis-tools-env.sh"

require_tool_jar "ili2pg" "$ILI2PG_JAR"
if [[ "$#" -ne 2 ]]; then
  echo "Usage: scripts/export-xtf.sh catalog|master-data|business-data target.xtf" >&2
  exit 2
fi

case "$1" in
  catalog) TOPIC="Kataloge" ;;
  master-data) TOPIC="Stammdaten" ;;
  business-data) TOPIC="Geschaeftsdaten" ;;
  *) echo "Unknown export scope: $1" >&2; exit 2 ;;
esac

TARGET="$2"
cd "$MABILLON_ROOT"
mkdir -p "$(dirname "$TARGET")"
connection_args=(--dbhost "$PGHOST" --dbport "$PGPORT" --dbdatabase "$PGDATABASE" --dbusr "$PGUSER" --dbpwd "$PGPASSWORD")
java -jar "$ILI2PG_JAR" \
  --export \
  --dbschema mabillon \
  --exportTid \
  --modeldir "$MABILLON_MODEL_DIR" \
  --models "$MABILLON_MODEL_NAME" \
  --topics "${MABILLON_MODEL_NAME}.${TOPIC}" \
  "${connection_args[@]}" \
  "$TARGET"
"${SCRIPT_DIR}/validate-xtf.sh" "$TARGET"
