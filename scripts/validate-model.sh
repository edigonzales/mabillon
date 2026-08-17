#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=interlis-tools-env.sh
source "${SCRIPT_DIR}/interlis-tools-env.sh"

require_tool_jar "ili2c" "$ILI2C_JAR"

cd "$MABILLON_ROOT"
exec java -jar "$ILI2C_JAR" "$MABILLON_MODEL"
