#!/usr/bin/env bash

_mabillon_sourced=0
if [ -n "${ZSH_VERSION:-}" ]; then
    case "${ZSH_EVAL_CONTEXT:-}" in
        *:file) _mabillon_sourced=1 ;;
    esac
elif [ -n "${BASH_VERSION:-}" ]; then
    if [ "${BASH_SOURCE[0]}" != "$0" ]; then
        _mabillon_sourced=1
    fi
fi

if [ "$_mabillon_sourced" -ne 1 ]; then
    echo "Dieses Script muss in die aktuelle Shell geladen werden:" >&2
    echo "  source scripts/dev-up.sh" >&2
    exit 2
fi

_mabillon_dev_up() {
    local reset=0

    case "${1:-}" in
        "") ;;
        --reset) reset=1 ;;
        -h|--help)
            echo "Usage: source scripts/dev-up.sh [--reset]"
            echo
            echo "Startet die lokale PostgreSQL/PostGIS-Datenbank, erzeugt bei Bedarf"
            echo "das INTERLIS-Schema und importiert die Golden-Path-Testdaten."
            echo "--reset entfernt die lokale Dev-Datenbank und den lokalen Dev-Dateispeicher zuerst."
            return 0
            ;;
        *)
            echo "Unbekannte Option: $1" >&2
            echo "Usage: source scripts/dev-up.sh [--reset]" >&2
            return 2
            ;;
    esac

    if [ "$#" -gt 1 ]; then
        echo "Usage: source scripts/dev-up.sh [--reset]" >&2
        return 2
    fi

    if ! command -v git >/dev/null 2>&1; then
        echo "git wurde nicht gefunden." >&2
        return 1
    fi
    if ! command -v docker >/dev/null 2>&1; then
        echo "Docker wurde nicht gefunden." >&2
        return 1
    fi
    if ! docker compose version >/dev/null 2>&1; then
        echo "Docker Compose v2 wurde nicht gefunden." >&2
        return 1
    fi

    export MABILLON_ROOT
    MABILLON_ROOT="$(git rev-parse --show-toplevel 2>/dev/null)" || {
        echo "Kein Git-Repository gefunden. Starte das Script innerhalb des Mabillon-Repositories." >&2
        return 1
    }

    export SPRING_PROFILES_ACTIVE="dev"

    export POSTGRES_DB="${POSTGRES_DB:-mabillon}"
    export POSTGRES_USER="${POSTGRES_USER:-mabillon}"
    export POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-mabillon}"
    export MABILLON_DEV_DB_PORT="${MABILLON_DEV_DB_PORT:-55432}"

    export PGHOST="localhost"
    export PGPORT="$MABILLON_DEV_DB_PORT"
    export PGDATABASE="$POSTGRES_DB"
    export PGUSER="$POSTGRES_USER"
    export PGPASSWORD="$POSTGRES_PASSWORD"

    export MABILLON_CAYENNE_URL="jdbc:postgresql://${PGHOST}:${PGPORT}/${PGDATABASE}"
    export MABILLON_CAYENNE_USERNAME="$PGUSER"
    export MABILLON_CAYENNE_PASSWORD="$PGPASSWORD"

    export MABILLON_STORAGE_ROOT="${MABILLON_STORAGE_ROOT:-${MABILLON_ROOT}/build/dev-data/documents}"
    export MABILLON_SIP_ROOT="${MABILLON_SIP_ROOT:-${MABILLON_ROOT}/build/dev-data/sips}"
    export MABILLON_ENVIRONMENT="development"

    export MABILLON_SECURITY_ADMIN_USERNAME="${MABILLON_SECURITY_ADMIN_USERNAME:-admin}"
    export MABILLON_SECURITY_ADMIN_PASSWORD="${MABILLON_SECURITY_ADMIN_PASSWORD:-admin}"
    export MABILLON_SECURITY_SACHBEARBEITER_USERNAME="${MABILLON_SECURITY_SACHBEARBEITER_USERNAME:-sachbearbeiter}"
    export MABILLON_SECURITY_SACHBEARBEITER_PASSWORD="${MABILLON_SECURITY_SACHBEARBEITER_PASSWORD:-sachbearbeiter}"

    _mabillon_compose() {
        (cd "$MABILLON_ROOT" && docker compose -f compose.yaml -f compose.dev.yaml "$@")
    }

    if [ "$reset" -eq 1 ]; then
        echo "Setze lokale Mabillon-Entwicklungsumgebung zurück ..."
        _mabillon_compose rm -sf postgres >/dev/null 2>&1 || true
        docker volume rm -f mabillon-postgres-dev >/dev/null 2>&1 || true
        rm -rf "${MABILLON_ROOT}/build/dev-data"
    fi

    mkdir -p "$MABILLON_STORAGE_ROOT" "$MABILLON_SIP_ROOT" || return 1

    echo "Starte PostgreSQL/PostGIS ..."
    _mabillon_compose up -d postgres || return 1

    local attempts=0
    while ! _mabillon_compose exec -T postgres pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" >/dev/null 2>&1; do
        attempts=$((attempts + 1))
        if [ "$attempts" -ge 30 ]; then
            echo "PostgreSQL wurde nicht rechtzeitig bereit." >&2
            return 1
        fi
        sleep 1
    done

    local schema_exists
    schema_exists="$(_mabillon_compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tAc \
        "SELECT CASE WHEN EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = 'mabillon') THEN '1' ELSE '0' END;")" || return 1
    schema_exists="$(printf '%s' "$schema_exists" | tr -d '[:space:]')"

    if [ "$schema_exists" != "1" ]; then
        echo "Erzeuge INTERLIS-Schema ..."
        bash "${MABILLON_ROOT}/scripts/create-schema.sh" || return 1
    fi

    local business_table_exists
    business_table_exists="$(_mabillon_compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tAc \
        "SELECT CASE WHEN to_regclass('mabillon.geschaeft') IS NOT NULL THEN '1' ELSE '0' END;")" || return 1
    business_table_exists="$(printf '%s' "$business_table_exists" | tr -d '[:space:]')"

    local fixture_ready=0
    if [ "$business_table_exists" = "1" ]; then
        fixture_ready="$(_mabillon_compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tAc \
            "SELECT CASE WHEN EXISTS (SELECT 1 FROM mabillon.geschaeft WHERE geschaeftsnummer = 'AGI-G-2026-000421') THEN '1' ELSE '0' END;")" || return 1
        fixture_ready="$(printf '%s' "$fixture_ready" | tr -d '[:space:]')"
    fi

    if [ "$fixture_ready" != "1" ]; then
        echo "Importiere lokale Testdaten ..."
        bash "${MABILLON_ROOT}/scripts/import-xtf.sh" \
            "model/testdata/01_SO_AGI_GEVER_20260707_testdaten_kataloge.xtf" || return 1
        bash "${MABILLON_ROOT}/scripts/import-xtf.sh" \
            "model/testdata/02_SO_AGI_GEVER_20260707_testdaten_stammdaten.xtf" || return 1
        bash "${MABILLON_ROOT}/scripts/import-xtf.sh" \
            "model/testdata/03_SO_AGI_GEVER_20260707_testdaten_geschaeftsdaten.xtf" || {
                echo "Testdatenimport fehlgeschlagen. Bei einem teilinitialisierten Stand hilft: source scripts/dev-up.sh --reset" >&2
                return 1
            }
    else
        echo "Schema und Testdaten sind bereits vorhanden."
    fi

    echo
    echo "Mabillon-Entwicklungsumgebung ist bereit."
    echo
    echo "Spring-Profil: ${SPRING_PROFILES_ACTIVE}"
    echo "JDBC-URL:      ${MABILLON_CAYENNE_URL}"
    echo "DB-Benutzer:   ${MABILLON_CAYENNE_USERNAME}"
    echo "DB-Passwort:   ${MABILLON_CAYENNE_PASSWORD}"
    echo
    echo "Lokale Mabillon-Logins:"
    echo "  Admin:           ${MABILLON_SECURITY_ADMIN_USERNAME} / ${MABILLON_SECURITY_ADMIN_PASSWORD}"
    echo "  Sachbearbeiter:  ${MABILLON_SECURITY_SACHBEARBEITER_USERNAME} / ${MABILLON_SECURITY_SACHBEARBEITER_PASSWORD}"
    echo
    echo "Dokumentspeicher: ${MABILLON_STORAGE_ROOT}"
    echo "SIP-Speicher:     ${MABILLON_SIP_ROOT}"
    echo
    echo "Mabillon starten mit:"
    echo "  ./gradlew bootRun"
}

_mabillon_dev_up "$@"
_mabillon_status=$?
if [ -n "${ZSH_VERSION:-}" ]; then
    unfunction _mabillon_dev_up _mabillon_compose 2>/dev/null || true
else
    unset -f _mabillon_dev_up _mabillon_compose 2>/dev/null || true
fi
unset _mabillon_sourced
return "$_mabillon_status"
