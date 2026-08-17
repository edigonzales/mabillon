# Mabillon Backup und Restore

Die Datenbank und der Dokument-/SIP-Speicher bilden gemeinsam den wiederherzustellenden Zustand. Ein Datenbankdump ohne Storage-Snapshot ist unvollständig.

## Backup

1. Ablieferungen und laufende Schreibvorgänge in Mabillon kontrolliert anhalten.
2. PostgreSQL als Custom-Format sichern:

```bash
pg_dump --format=custom --file=mabillon-$(date +%Y%m%dT%H%M%S).dump \
  --host="$PGHOST" --port="$PGPORT" --username="$PGUSER" --dbname="$PGDATABASE"
```

3. `MABILLON_STORAGE_ROOT` und `MABILLON_SIP_ROOT` mit einem konsistenten, versionierten Dateisystem-/Object-Storage-Snapshot sichern.
4. Dump und beide Snapshots mit SHA-256 prüfen und getrennt vom Primärsystem aufbewahren.

## Restore

1. Zielsystem isolieren und leere Datenbank bereitstellen.
2. Dump wiederherstellen:

```bash
pg_restore --clean --if-exists --no-owner \
  --host="$PGHOST" --port="$PGPORT" --username="$PGUSER" --dbname="$PGDATABASE" \
  mabillon-YYYYMMDDTHHMMSS.dump
```

3. Storage-Snapshots an die konfigurierten Pfade zurückspielen.
4. `/actuator/health` prüfen, danach einen Dossier-Download und die Referenz auf ein validiertes SIP kontrollieren.
5. Erst nach erfolgreicher Stichprobe wieder Schreibzugriff freigeben.

RPO, RTO, Aufbewahrungsfristen und der konkrete Snapshot-Dienst sind Betreiberentscheidungen und müssen vor Produktion verbindlich dokumentiert werden.
