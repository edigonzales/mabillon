# Backup und Restore

Die Datenbank und der Dokument-/SIP-Speicher bilden gemeinsam den wiederherzustellenden Zustand. Ein Datenbankdump ohne zugehörigen Storage-Snapshot ist unvollständig.

## Backup

1. Laufende Schreibvorgänge und Archivablieferungen kontrolliert anhalten bzw. einen konsistenten Snapshot-Zeitpunkt sicherstellen.
2. PostgreSQL im Custom-Format sichern:

```bash
pg_dump --format=custom --file=mabillon-$(date +%Y%m%dT%H%M%S).dump \
  --host="$PGHOST" --port="$PGPORT" --username="$PGUSER" --dbname="$PGDATABASE"
```

3. Dokument- und SIP-Storage konsistent und versioniert sichern.
4. Dump und Storage-Snapshots mit SHA-256 prüfen und getrennt vom Primärsystem aufbewahren.

## Restore

1. Zielsystem isolieren und leere Datenbank bereitstellen.
2. Dump wiederherstellen:

```bash
pg_restore --clean --if-exists --no-owner \
  --host="$PGHOST" --port="$PGPORT" --username="$PGUSER" --dbname="$PGDATABASE" \
  mabillon-YYYYMMDDTHHMMSS.dump
```

3. Dokument- und SIP-Storage aus demselben Sicherungsstand zurückspielen.
4. `/actuator/health` prüfen.
5. Stichproben: verwalteten Unterlagen-Download sowie Referenz auf ein validiertes SIP prüfen.
6. Erst danach Schreibzugriff freigeben.

## Betreiberentscheidungen

RPO, RTO, Aufbewahrungsfristen, Verschlüsselung, Offsite-Strategie und konkreter Snapshot-Dienst müssen für die jeweilige Installation verbindlich festgelegt werden.
