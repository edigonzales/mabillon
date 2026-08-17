#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=interlis-tools-env.sh
source "${SCRIPT_DIR}/interlis-tools-env.sh"

require_tool_jar "ili2pg" "$ILI2PG_JAR"
"${SCRIPT_DIR}/validate-model.sh"

cd "$MABILLON_ROOT"
mapfile -t connection_args < <(ili2pg_connection_args)
java -jar "$ILI2PG_JAR" \
  --schemaimport \
  --dbschema mabillon \
  --createFk \
  --createFkIdx \
  --createUnique \
  --createMandatoryChecks \
  --createNumChecks \
  --createTextChecks \
  --createDateTimeChecks \
  --createMetaInfo \
  --createTidCol \
  --createBasketCol \
  --setupPgExt \
  --modeldir "$MABILLON_MODEL_DIR" \
  "${connection_args[@]}" \
  "$MABILLON_MODEL"

POSTGRES_JDBC_JAR="${POSTGRES_JDBC_JAR:-}"
if [[ -z "$POSTGRES_JDBC_JAR" ]]; then
  POSTGRES_JDBC_JAR="$(find "$(dirname "$ILI2PG_JAR")/libs" -maxdepth 1 -type f -name 'postgresql-*.jar' -print -quit)"
fi
if [[ -z "$POSTGRES_JDBC_JAR" || ! -f "$POSTGRES_JDBC_JAR" ]]; then
  echo "PostgreSQL JDBC driver not found; set POSTGRES_JDBC_JAR." >&2
  exit 1
fi

java -cp "$POSTGRES_JDBC_JAR" scripts/SchemaConstraintRepair.java \
  "$PGHOST" "$PGPORT" "$PGDATABASE" "$PGUSER" "$PGPASSWORD" mabillon
