# Aussonderung, Archivablieferung und SIP

## Aussonderung

Als Kandidaten werden Dossiers betrachtet, die mindestens:

- geschlossen sind,
- nicht bereits erfolgreich übernommen oder vernichtet wurden,
- die relevanten Datenqualitätsprüfungen ohne blockierende Fehler bestehen.

## Archivablieferung

Eine `ArchivAblieferung` gruppiert Dossiers für eine konkrete Übergabe. Dossiers können im Entwurf hinzugefügt und entfernt werden.

Typische Statusfolge:

```text
Entwurf
  -> Bereit
  -> SIP_Erstellt
  -> Validiert
  -> Uebergeben
  -> Uebernommen
```

Bei Korrekturbedarf kann die Ablieferung in `Korrektur_erforderlich`, bei Ablehnung in `Abgelehnt` wechseln. Ein korrigiertes SIP wird als neuer Erzeugungsversuch geführt; frühere Versuche bleiben nachvollziehbar.

## SIP-Paket

Ein SIP ist kein blosses ZIP der Dateien. Mabillon erzeugt einen strukturierten Paketbaum nach dem konfigurierten Archivprofil. Beim eCH-0160-Profil gehören mindestens Metadaten, XSDs und Content zusammen.

Die `metadata.xml` wird aus den versionierten eCH-0160-XSDs über daraus generierte Jakarta-XML-Binding-Typen erzeugt und bereits beim Schreiben gegen das Schema validiert. Das fertige SIP wird anschliessend nochmals unabhängig validiert.

Jeder Erzeugungsversuch besitzt unter anderem:

- Laufnummer und Status,
- Erstellungszeitpunkt/Akteur,
- Storage-URI,
- Grösse und SHA-256,
- Validierungsstatus und -bericht.

## Validierung

Vor einer erfolgreichen Übergabe werden mindestens geprüft:

- nur zulässige/geschlossene Dossiers,
- relevante Datenqualitätsregeln,
- erwartete Dokumentdateien vorhanden,
- Hashes konsistent,
- Paket-/XML-Struktur gegen das Zielprofil gültig.

Ein ungültiges Paket wird nicht als erfolgreich validiert oder übergeben.

## Übernahme

Bei erfolgreicher Übernahme werden Ablieferung, Archivreferenz/-signatur und betroffene Dossier-/Geschäftsstatus im dafür vorgesehenen Unit-of-Work aktualisiert und journalisiert. Eine Übernahme bedeutet nicht automatische Vernichtung.

## Zielprofil

Aktuell mitgeliefert: [eCH-0160 1.3.0](../interfaces/archive/ech-0160-1.3.0/PROFILE.md). Zusätzliche Archivprofile müssen separat definiert und validiert werden.
