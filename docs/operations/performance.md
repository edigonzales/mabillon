# Performance-Baseline

## Zweck

Die Baseline liefert eine reproduzierbare Grössenordnung für zentrale PostgreSQL-Abfragen. Sie ist kein SLA und kein Ersatz für Messungen auf der konkreten Produktionsumgebung.

`scripts/performance-baseline.sh` erzeugt eine verworfene Kopie der konfigurierten Datenbank und ergänzt ungefähr:

| Objekt | Zielmenge |
|---|---:|
| Dossiers | 100'000 |
| Geschäfte | 100'000 |
| Unterlagen | 1'000'000 |
| Aufgaben | 500'000 |

Die Quelldatenbank wird nicht verändert. Die vorhandenen Indizes werden vor der Messung nicht künstlich erweitert.

## Referenzmessung

Eine Referenzmessung vom 16.08.2026 ergab bei insgesamt 100'002 Dossiers, 100'002 Geschäften, 1'000'010 Unterlagen und 500'009 Aufgaben:

| Abfrage | Execution time |
|---|---:|
| Dossier-Lookup über eindeutige Dossiernummer | 0.037 ms |
| Geschäft mit Dokument-/Aufgabenzählung | 0.102 ms |
| Dokumente eines Dossiers | 0.126 ms |
| Aufgabenübersicht Top 50 | 46.298 ms |
| globale Unterlagentitelsuche | 110.444 ms |

Die Werte sind nur zusammen mit Datenvolumen, Datenbankzustand und Ausführungsplan interpretierbar. Das Script schreibt deshalb vollständige `EXPLAIN (ANALYZE, BUFFERS)`-Ausgaben nach `build/reports/performance-baseline-*.txt`.

## Sucharchitektur

Listen-/Suchpfade führen Filter, Counts, Sortierung und Pagination soweit fachlich sinnvoll in Cayenne/PostgreSQL aus. Die globale Suche paginiert über typenspezifische Resultatblöcke, ohne alle Tabellen in Java zu materialisieren.

Unbegrenzte INTERLIS-`LangerText`-Felder sind bewusst nicht Teil des generischen case-insensitive Freitextprädikats, weil Cayenne 5.0-M2 hierfür problematische `VARCHAR`-Casts erzeugen kann. Strukturierte IDs, Titel, Namen, Organisation, Katalogwerte und Fachsystemfelder bleiben suchbar.

## Ausführen

```bash
PGHOST=localhost PGPORT=55432 PGDATABASE=mabillon PGUSER=mabillon \
PGPASSWORD="$MABILLON_DB_PASSWORD" \
scripts/performance-baseline.sh
```
