#!/usr/bin/env bash
set -euo pipefail

MABILLON_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export MABILLON_ROOT

export ILI2PG_JAR="${ILI2PG_JAR:-/Users/stefan/apps/ili2pg-5.5.2/ili2pg-5.5.2.jar}"
export ILI2C_JAR="${ILI2C_JAR:-/Users/stefan/apps/ili2c-5.6.8/ili2c.jar}"
export ILIVALIDATOR_JAR="${ILIVALIDATOR_JAR:-/Users/stefan/apps/ilivalidator-1.15.0/ilivalidator-1.15.0.jar}"

export MABILLON_MODEL="${MABILLON_ROOT}/model/SO_AGI_GEVER_20260707.ili"
export MABILLON_MODEL_DIR="${MABILLON_ROOT}/model;http://models.interlis.ch/;http://models.geo.admin.ch/"
export MABILLON_MODEL_NAME="SO_AGI_GEVER_20260707"

# Local development defaults. CI or a developer may override these without
# putting credentials into scripts, reports, or command output.
export PGHOST="${PGHOST:-localhost}"
export PGPORT="${PGPORT:-5432}"
export PGDATABASE="${PGDATABASE:-mabillon}"
export PGUSER="${PGUSER:-mabillon}"
export PGPASSWORD="${PGPASSWORD:-mabillon}"

require_tool_jar() {
  local tool_name="$1"
  local jar_path="$2"
  if [[ ! -f "$jar_path" ]]; then
    echo "${tool_name} JAR not found: ${jar_path}" >&2
    return 1
  fi
}

require_all_tool_jars() {
  require_tool_jar "ili2pg" "$ILI2PG_JAR"
  require_tool_jar "ili2c" "$ILI2C_JAR"
  require_tool_jar "ilivalidator" "$ILIVALIDATOR_JAR"
}

ili2pg_connection_args() {
  printf '%s\n' \
    --dbhost "$PGHOST" \
    --dbport "$PGPORT" \
    --dbdatabase "$PGDATABASE" \
    --dbusr "$PGUSER" \
    --dbpwd "$PGPASSWORD"
}
