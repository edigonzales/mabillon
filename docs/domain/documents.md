# Unterlagen und Dokumente

## Begriff

Eine **Unterlage** ist das fachliche Aktenstück. Die Datei ist ihr Inhalt; Metadaten, Dossierzuordnung und optionaler Geschäftskontext sind fachliche Eigenschaften der Unterlage.

Dateiinhalte werden nicht als PostgreSQL-BLOB gespeichert.

## Zuordnung

- Jede Unterlage gehört genau einem Dossier.
- Ein Geschäftskontext ist optional.
- Wird ein Geschäft zugeordnet, muss es zum selben Dossier gehören.

## Lifecycle

Für nicht unmittelbar aktenrelevant registrierte Unterlagen gilt:

```text
In_Arbeit -> Final -> Registriert
    |          |          |
    +----------+----------+-> Storniert
```

Regeln:

- nur `In_Arbeit` kann finalisiert werden,
- nur `Final` kann anschliessend aktenrelevant registriert werden,
- aktenrelevante Registrierung setzt `aktenrelevant=true` und `Registriert`,
- eine bereits bei der Erfassung aktenrelevante Unterlage wird direkt `Registriert`,
- `Storniert` ist für normale Mutationen terminal,
- Storno ist auditierbar und ersetzt die normale physische Löschung.

## Speicherung

`DocumentStorage` trennt Fachlogik vom konkreten Storage. Die aktuelle Dateisystemimplementierung verwendet:

1. `stage` – Inhalt temporär aufnehmen und SHA-256/Grösse berechnen,
2. Fachobjekt und Journal im DB-Unit-of-Work persistieren,
3. `commit` – Datei in den finalen Objektpfad verschieben,
4. bei Fehlern Staging best-effort bereinigen und Inkonsistenzen sichtbar machen.

Finale Pfade werden nie aus dem vom Benutzer gelieferten Dateinamen erzeugt. Der Originalname bleibt Metadatum.

Beispiel:

```text
<storage-root>/objects/<hash-prefix>/<uuid>
```

Beim Öffnen einer verwalteten Datei können Existenz und SHA-256 gegen die gespeicherten Metadaten geprüft werden.

## Download

Der Download liefert fachlich passenden MIME-Type, Content-Length und Content-Disposition. Importierte externe Storage-URIs werden nicht automatisch als lokal verwaltete Dateien behandelt.

## E-Mail

Eingehende und ausgehende E-Mails können als Unterlage registriert werden. EML wird typischerweise als `message/rfc822` gespeichert. Eine automatische Mailboxintegration oder Parent/Attachment-Domänenstruktur ist nicht Teil des aktuellen Produkts.

## Physische Löschung

Physische Löschung ist nur für nicht registriertes Staging, explizite Vernichtungs-/Reparaturprozesse mit Audit oder vergleichbare technische Bereinigung zulässig. Aktenrelevante registrierte Unterlagen werden im normalen UI storniert, nicht gelöscht.
