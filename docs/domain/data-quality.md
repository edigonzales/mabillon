# Datenqualität

Mabillon prüft Dossiers, Geschäfte und Archivablieferungen mit expliziten Regeln. Ein Finding enthält Regelcode, Severity, Objekttyp, Objekt-ID und verständliche Meldung.

## Regeln

| Code | Severity | Bedeutung |
|---|---|---|
| DQ-001 | WARNING | Dossier hat keine gültige aktive Registraturplanposition. |
| DQ-002 | WARNING | Geschäft ist keinem Dossier zugeordnet. |
| DQ-003 | WARNING | Geschäft hat keine gültige aktive Geschäftsart. |
| DQ-004 | WARNING | Unterlage ist keinem Dossier zugeordnet. |
| DQ-005 | ERROR | Unterlage verweist auf ein Geschäft eines anderen Dossiers. |
| DQ-006 | ERROR | Abgeschlossenes Geschäft enthält offene Aufgaben. |
| DQ-007 | ERROR | Geschlossenes/terminales Dossier enthält ein nicht abgeschlossenes Geschäft. |
| DQ-008 | ERROR | Prozessstatus fehlt oder passt nicht zur Geschäftsart. |
| DQ-009 | ERROR | Gesetzter Resultatstatus passt nicht zur Geschäftsart. |
| DQ-010 | ERROR | Resultatpflichtiges abgeschlossenes Geschäft hat kein Resultat. |
| DQ-011 | ERROR | Aktenrelevante, nicht stornierte Unterlage hat keine vorhandene Datei. |
| DQ-012 | WARNING | Gespeicherter SHA-256 stimmt nicht mit dem verwalteten Storage-Inhalt überein. |
| DQ-013 | ERROR | Archivablieferung enthält ein nicht geschlossenes Dossier. |

## Schema versus defensive Regeln

Im gültigen INTERLIS-/PostgreSQL-Schema sind Geschäft→Dossier und Unterlage→Dossier verpflichtend. DQ-002 und DQ-004 bleiben trotzdem als defensive Prüfungen für inkonsistente, externe oder ausserhalb normaler Anwendungswege entstandene Zustände bestehen.

## Einsatz bei Abschluss und Archivierung

Datenqualität ist nicht nur ein Report:

- Dossierabschluss wird durch relevante `ERROR`-Befunde blockiert.
- Archivkandidaten müssen die dafür relevanten Qualitätsanforderungen erfüllen.
- SIP-Erzeugung prüft zusätzlich Dateien und Hashes.
- Eine Archivablieferung mit einem nicht geschlossenen Dossier ist ungültig.

## Berechtigung

Die Ausführung ist eine explizite Permission (`RUN_DATA_QUALITY`). Der Service prüft sie unabhängig von der Weboberfläche.
