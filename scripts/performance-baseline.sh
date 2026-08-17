#!/usr/bin/env bash
set -euo pipefail

# Runs the Phase 10 baseline against a disposable clone of the configured
# PostgreSQL database. The source database is never modified.
PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5432}"
PGDATABASE="${PGDATABASE:-mabillon}"
PGUSER="${PGUSER:-mabillon}"
PERFORMANCE_DB="${MABILLON_PERFORMANCE_DB:-mabillon_perf_$(date +%Y%m%d%H%M%S)}"
REPORT="${MABILLON_PERFORMANCE_REPORT:-build/reports/performance-baseline-${PERFORMANCE_DB}.txt}"

if command -v psql >/dev/null; then
  PSQL=(psql)
else
  PERFORMANCE_CONTAINER="${MABILLON_POSTGRES_CONTAINER:-mabillon-phase0-postgres}"
  command -v docker >/dev/null || { echo "psql or docker is required" >&2; exit 1; }
  docker inspect "$PERFORMANCE_CONTAINER" >/dev/null 2>&1 || {
    echo "psql is unavailable and PostgreSQL container ${PERFORMANCE_CONTAINER} was not found" >&2
    exit 1
  }
  PSQL=(docker exec -i -e "PGPASSWORD=${PGPASSWORD:-}" "$PERFORMANCE_CONTAINER" psql)
  PGHOST=localhost
  PGPORT=5432
fi
mkdir -p "$(dirname "$REPORT")"

cleanup() {
  PGPASSWORD="${PGPASSWORD:-}" "${PSQL[@]}" --no-psqlrc -X -v ON_ERROR_STOP=1 \
    -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d postgres \
    -c "DROP DATABASE IF EXISTS \"$PERFORMANCE_DB\";" >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "Creating disposable performance database ${PERFORMANCE_DB} from ${PGDATABASE}..."
PGPASSWORD="${PGPASSWORD:-}" "${PSQL[@]}" --no-psqlrc -X -v ON_ERROR_STOP=1 \
  -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d postgres \
  -c "CREATE DATABASE \"$PERFORMANCE_DB\" TEMPLATE \"$PGDATABASE\";"

echo "Generating 100k dossiers, 100k businesses, 1M documents and 500k tasks..."
PGPASSWORD="${PGPASSWORD:-}" "${PSQL[@]}" --no-psqlrc -X -v ON_ERROR_STOP=1 \
  -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PERFORMANCE_DB" <<'SQL'
SET synchronous_commit = off;

INSERT INTO mabillon.dossier
    (t_basket, dossiernummer, titel, ordnungssystemposition, astatus, eroeffnetam)
SELECT (SELECT min(t_id) FROM mabillon.t_ili2db_basket),
       format('PERF-D-%s', lpad(i::text, 6, '0')),
       format('Performance-Dossier %s', i),
       (SELECT min(t_id) FROM mabillon.ordnungssystemposition),
       'Geschlossen',
       DATE '2026-01-01' + (i % 365)
FROM generate_series(1, 100000) AS values(i);

INSERT INTO mabillon.geschaeft
    (t_basket, geschaeftsnummer, titel, geschaeftsart, lifecyclestatus,
     prozessstatus, resultatstatus, federfuehrung, verantwortlicher,
     erstelltam, geschaeftsdossier)
SELECT (SELECT min(t_id) FROM mabillon.t_ili2db_basket),
       format('PERF-G-%s', lpad(i::text, 6, '0')),
       format('Performance-Geschäft %s', i),
       (SELECT min(t_id) FROM mabillon.geschaeftsart),
       'Abgeschlossen',
       (SELECT min(t_id) FROM mabillon.prozessstatus),
       (SELECT min(t_id) FROM mabillon.resultatstatus),
       (SELECT min(t_id) FROM mabillon.organisationseinheit),
       (SELECT min(t_id) FROM mabillon.benutzer),
       TIMESTAMP '2026-01-01 08:00:00' + make_interval(days => (i % 365)),
       dossier.t_id
FROM generate_series(1, 100000) AS values(i)
JOIN LATERAL (
    SELECT t_id
    FROM mabillon.dossier
    WHERE dossiernummer = format('PERF-D-%s', lpad(i::text, 6, '0'))
) AS dossier ON true;

INSERT INTO mabillon.unterlage
    (t_basket, titel, typ, registriertvon, aktenrelevant, astatus, ablagedossier)
SELECT (SELECT min(t_id) FROM mabillon.t_ili2db_basket),
       format('Performance-Unterlage %s-%s', d.i, u.i),
       (SELECT min(t_id) FROM mabillon.unterlagentyp),
       (SELECT min(t_id) FROM mabillon.benutzer),
       true,
       'Final',
       dossier.t_id
FROM generate_series(1, 100000) AS d(i)
CROSS JOIN generate_series(1, 10) AS u(i)
JOIN LATERAL (
    SELECT t_id
    FROM mabillon.dossier
    WHERE dossiernummer = format('PERF-D-%s', lpad(d.i::text, 6, '0'))
) AS dossier ON true;

INSERT INTO mabillon.aufgabe
    (t_basket, titel, typ, astatus, prioritaet, erstelltam, aufgabengeschaeft)
SELECT (SELECT min(t_id) FROM mabillon.t_ili2db_basket),
       format('Performance-Aufgabe %s-%s', g.i, a.i),
       (SELECT min(t_id) FROM mabillon.aufgabentyp),
       'Registriert',
       (a.i % 6),
       TIMESTAMP '2026-01-01 08:00:00' + make_interval(days => (g.i % 365)),
       geschaeft.t_id
FROM generate_series(1, 100000) AS g(i)
CROSS JOIN generate_series(1, 5) AS a(i)
JOIN LATERAL (
    SELECT t_id
    FROM mabillon.geschaeft
    WHERE geschaeftsnummer = format('PERF-G-%s', lpad(g.i::text, 6, '0'))
) AS geschaeft ON true;

ANALYZE mabillon.dossier;
ANALYZE mabillon.geschaeft;
ANALYZE mabillon.unterlage;
ANALYZE mabillon.aufgabe;
SQL

echo "Running representative SQL plans; report: ${REPORT}"
{
  echo "Mabillon Phase 10 performance baseline"
  echo "database=${PERFORMANCE_DB} host=${PGHOST} port=${PGPORT}"
  date -u '+timestamp=%Y-%m-%dT%H:%M:%SZ'
  echo
  echo "row counts"
  PGPASSWORD="${PGPASSWORD:-}" "${PSQL[@]}" --no-psqlrc -X -v ON_ERROR_STOP=1 -At \
    -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PERFORMANCE_DB" \
    -c "SELECT 'dossier=' || count(*) FROM mabillon.dossier; SELECT 'geschaeft=' || count(*) FROM mabillon.geschaeft; SELECT 'unterlage=' || count(*) FROM mabillon.unterlage; SELECT 'aufgabe=' || count(*) FROM mabillon.aufgabe;"
  echo
  echo "existing indexes"
  PGPASSWORD="${PGPASSWORD:-}" "${PSQL[@]}" --no-psqlrc -X -v ON_ERROR_STOP=1 -At \
    -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PERFORMANCE_DB" \
    -c "SELECT schemaname || '.' || tablename || ' -> ' || indexname FROM pg_indexes WHERE schemaname = 'mabillon' AND tablename IN ('dossier','geschaeft','unterlage','aufgabe') ORDER BY tablename,indexname;"
  echo
  echo "dossier lookup by unique business number"
  PGPASSWORD="${PGPASSWORD:-}" "${PSQL[@]}" --no-psqlrc -X -v ON_ERROR_STOP=1 \
    -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PERFORMANCE_DB" \
    -c "EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT) SELECT t_id, dossiernummer, titel FROM mabillon.dossier WHERE dossiernummer = 'PERF-D-050000';"
  echo
  echo "business detail and document/task counts"
  PGPASSWORD="${PGPASSWORD:-}" "${PSQL[@]}" --no-psqlrc -X -v ON_ERROR_STOP=1 \
    -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PERFORMANCE_DB" \
    -c "EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT) SELECT g.t_id, g.geschaeftsnummer, count(DISTINCT u.t_id) AS documents, count(DISTINCT a.t_id) AS tasks FROM mabillon.geschaeft g LEFT JOIN mabillon.unterlage u ON u.geschaeftskontext = g.t_id LEFT JOIN mabillon.aufgabe a ON a.aufgabengeschaeft = g.t_id WHERE g.geschaeftsnummer = 'PERF-G-050000' GROUP BY g.t_id, g.geschaeftsnummer;"
  echo
  echo "documents for one dossier"
  PGPASSWORD="${PGPASSWORD:-}" "${PSQL[@]}" --no-psqlrc -X -v ON_ERROR_STOP=1 \
    -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PERFORMANCE_DB" \
    -c "EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT) SELECT t_id, titel FROM mabillon.unterlage WHERE ablagedossier = (SELECT t_id FROM mabillon.dossier WHERE dossiernummer = 'PERF-D-050000') ORDER BY titel LIMIT 50;"
  echo
  echo "open tasks dashboard slice"
  PGPASSWORD="${PGPASSWORD:-}" "${PSQL[@]}" --no-psqlrc -X -v ON_ERROR_STOP=1 \
    -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PERFORMANCE_DB" \
    -c "EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT) SELECT t_id, titel, faelligam, prioritaet FROM mabillon.aufgabe WHERE astatus = 'Registriert' ORDER BY faelligam NULLS LAST, prioritaet DESC, titel LIMIT 50;"
  echo
  echo "global document title search slice"
  PGPASSWORD="${PGPASSWORD:-}" "${PSQL[@]}" --no-psqlrc -X -v ON_ERROR_STOP=1 \
    -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PERFORMANCE_DB" \
    -c "EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT) SELECT t_id, titel FROM mabillon.unterlage WHERE titel ILIKE '%50000%' LIMIT 50;"
} > "$REPORT"

cat "$REPORT"
