# Phase 10 Performance-Baseline

Die Baseline wurde am 16.08.2026 gegen eine temporäre PostgreSQL-Kopie des
lokalen Testschemas gemessen. Die Quelldatenbank blieb unverändert; die
temporäre Datenbank wurde nach dem Lauf automatisch entfernt.

## Fixture

Die Ausführung erfolgte mit:

```bash
PGHOST=localhost PGPORT=55432 PGDATABASE=mabillon PGUSER=mabillon \
PGPASSWORD="$MABILLON_DB_PASSWORD" \
scripts/performance-baseline.sh
```

`scripts/performance-baseline.sh` erzeugt zusätzlich zu den vorhandenen
Golden-Path-Daten:

| Tabelle | Zielmenge | Gemessene Gesamtmenge |
|---|---:|---:|
| `dossier` | 100'000 | 100'002 |
| `geschaeft` | 100'000 | 100'002 |
| `unterlage` | 1'000'000 | 1'000'010 |
| `aufgabe` | 500'000 | 500'009 |

Die vorhandenen ili2pg-Indizes wurden unverändert verwendet. Es wurden keine
Performance-Indizes vor der Messung ergänzt.

## Representative SQL measurements

| Query | Execution time |
|---|---:|
| Dossier lookup über eindeutige Dossiernummer | 0.037 ms |
| Geschäft mit Dokument-/Aufgabenzählung | 0.102 ms |
| Dokumente eines Dossiers | 0.126 ms |
| Offene Aufgaben, sortiert, Top 50 | 46.298 ms |
| Globale Unterlagentitelsuche mit `%50000%` | 110.444 ms |

Die Rohdaten mit vollständigem `EXPLAIN (ANALYZE, BUFFERS)` liegen im
generierten Build-Artefakt `build/reports/phase10-performance-baseline.txt`.
Die beiden letzten Abfragen zeigen die erwartbaren Vollscans für die aktuelle
fachliche Such-/Sortierlogik. Eine Optimierung wird erst auf Basis dieser
Messwerte und eines fachlich bestätigten Suchprofils vorgenommen.
