#!/usr/bin/env bash
set -euo pipefail

MABILLON_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export MABILLON_ROOT

export MABILLON_MODEL="${MABILLON_MODEL:-${MABILLON_ROOT}/model/SO_AGI_GEVER_20260707.ili}"
export MABILLON_MODEL_DIR="${MABILLON_MODEL_DIR:-${MABILLON_ROOT}/model;http://models.interlis.ch/;http://models.geo.admin.ch/}"
export MABILLON_MODEL_NAME="${MABILLON_MODEL_NAME:-SO_AGI_GEVER_20260707}"

# Local development defaults. CI or a developer may override these without
# putting credentials into scripts, reports, or command output.
export PGHOST="${PGHOST:-localhost}"
export PGPORT="${PGPORT:-5432}"
export PGDATABASE="${PGDATABASE:-mabillon}"
export PGUSER="${PGUSER:-mabillon}"
export PGPASSWORD="${PGPASSWORD:-mabillon}"

run_interlis_tool() {
  local operation="$1"
  local file="${2:-}"
  if [[ -n "${MABILLON_RUNTIME_CLASSPATH:-}" ]]; then
    if [[ -n "$file" ]]; then
      java -cp "$MABILLON_RUNTIME_CLASSPATH" guru.interlis.mabillon.interlis.InterlisToolCli "$operation" "$file"
    else
      java -cp "$MABILLON_RUNTIME_CLASSPATH" guru.interlis.mabillon.interlis.InterlisToolCli "$operation"
    fi
    return
  fi

  local gradle_args=("-q" "interlisTool" "--no-daemon" "-PinterlisOperation=${operation}")
  if [[ -n "$file" ]]; then
    gradle_args+=("-PinterlisFile=${file}")
  fi
  (cd "$MABILLON_ROOT" && ./gradlew "${gradle_args[@]}")
}
