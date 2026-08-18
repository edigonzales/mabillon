# Storage und Konsistenz

## Dokument-Storage

Dateiinhalte werden ausserhalb PostgreSQL gespeichert. Die Datenbank enthält fachliche Metadaten, `storageUri`, Dateigrösse und SHA-256.

`DocumentStorage` bildet die Anwendungsgrenze; die aktuelle Implementierung speichert Dateien im Dateisystem unter objektbasierten Pfaden. Originaldateinamen werden nie als Dateisystempfad verwendet.

## Schreibreihenfolge

Die Registrierung einer Unterlage verwendet Staging:

```text
Upload
 -> stage + Hash/Grösse
 -> fachliche Validierung
 -> DB + Journal commit
 -> finaler Storage move
```

Fehler dürfen nicht stillschweigend einen als erfolgreich akzeptierten Zustand mit fehlender Datei erzeugen. Staging wird bei Persistenzfehlern best-effort verworfen; Datenqualitätsprüfungen erkennen fehlende Dateien und Hashabweichungen.

## SIP-Storage

SIP-Pakete und Validierungsberichte liegen in einem separat konfigurierbaren Root. Datenbank, Dokument-Storage und SIP-Storage bilden zusammen den wiederherzustellenden Betriebszustand.

## Sicherheit

- keine Pfadbildung aus Benutzerdateinamen,
- Storage-URIs werden auf den konfigurierten Root begrenzt,
- MIME-Type ist Metadatum und kein Berechtigungs- oder Sicherheitsbeweis,
- SHA-256 wird serverseitig berechnet und kann bei Qualitäts-/Archivprüfungen erneut verifiziert werden.

Siehe [Unterlagen](../domain/documents.md) und [Backup/Restore](../operations/backup-restore.md).
