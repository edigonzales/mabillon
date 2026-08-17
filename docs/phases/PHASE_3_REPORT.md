# PHASE_3_REPORT

- Status: SUCCESS
- Phase: 3 – Dossier und Geschäft Kern
- Date: 2026-08-16
- Scope / Use cases: UC-002 bis UC-011, ohne Dokumentdateien

## Implemented

- Dossier-Eröffnung mit automatischer `AGI-D-YYYY-NNNNNN`-Nummer,
  aktiver Blattposition, offenem Status, Verantwortlichkeit und Journal.
- Geschäftseröffnung mit automatischer `ORG-G-YYYY-NNNNNN`-Nummer,
  Dossierbindung, aktiver Geschäftsart, genau einem aktiven Initialstatus,
  Lifecycle `Eroeffnet` und Journal.
- Explizite Dossier-/Geschäftsänderungen mit Service-Layer-Validierung und
  unveränderlicher Nummer/Geschäftsart.
- Prozessstatus- und Resultatstatuswechsel mit Aktivitäts- und
  Geschäftsartprüfung sowie atomarem Journal.
- Lifecycle-Regeln für Bearbeitung, Sistierung, Fortsetzung und Abschluss;
  Abschlussprüfungen für Aufgaben, terminalen Prozessstatus, Resultatpflicht
  und aktenrelevante Unterlagen.
- PostgreSQL-Nummernsequenz in `mabillon_app.number_sequence` mit atomarem
  `UPDATE ... RETURNING` und Unique-Key pro Organisation, Objekttyp und Jahr.
- Dossier-/Geschäftssuche mit Filtern und Pagination als Service- und
  HTTP-Listen; Eröffnungs- und Bearbeitungsformulare sowie Resultaterfassung.
- Normale HTTP-Fallbacks und HTMX-Fragment für Prozessstatus/Resultat.
- `Clock`-Bean für alle fachlichen Zeitwerte.
- Reproduzierbare Korrektur der bekannten ili2pg-5.5.2-Falschchecks für
  optionale Referenzen, ohne `--sqlEnableNull` und ohne Entfernen echter
  Mandatory-Constraints.

## Tests and gate evidence

- `scripts/validate-model.sh` – PASS.
- Alle vier positiven XTF-Fixtures mit `scripts/validate-xtf.sh` – PASS.
- Frischer PostgreSQL-16/PostGIS-Schemaimport und Import in der Reihenfolge
  Kataloge, Stammdaten, Geschäftsdaten – PASS.
- Nach der Constraint-Korrektur: 115 echte Check-Constraints; die 18
  bekannten falschen optionalen Referenzchecks sind entfernt; Golden Path mit
  1 Dossier, 1 Geschäft und 9 Unterlagen importiert – PASS.
- `./gradlew check --no-daemon` – PASS; 18 PostgreSQL/Testcontainers-,
  Cayenne-, Security-, MVC- und Fachregeltests.
- Kritische Tests: parallele Nummernvergabe, falsche Geschäftsart bei Prozess-
  und Resultatstatus, atomare Journalisierung sowie Full-Page-/HTMX-Fallback
  – PASS.
- Offizieller Cayenne-5.0-M2-MCP nach frischem Schema:
  `open_project` – PASS mit Modeler-Handshake;
  `dbimport_run` – `up_to_date`, 0 Änderungen, JDBC-Verbindung validiert;
  `cgen_run` – `up_to_date`, 44 Dateien betrachtet, 0 geschrieben, keine
  Warnungen.
- Browser-Smoke mit dem lokalen Boot-Server – PASS: Dossierliste,
  Geschäftsliste, beide Eröffnungsformulare, Dossierdetail und
  Geschäftsdetail mit Status-/Resultatpanel.

## Acceptance criteria

| Criterion | Result |
|---|---|
| Geschäft suchen, filtern und paginieren | PASS |
| Dossier suchen, filtern und paginieren | PASS |
| Dossier eröffnen und automatisch nummerieren | PASS |
| Geschäft eröffnen, Dossierbindung und Initialstatus | PASS |
| Geschäftsart-/Prozessstatusbindung | PASS |
| Resultatstatusbindung und Resultatpflicht bei Abschluss | PASS |
| Lifecycle inkl. Sistieren/Fortsetzen/Abschlussregeln | PASS |
| Journal atomar mit Fachänderung | PASS |
| Nummernvergabe unter Konkurrenz | PASS |
| Full-Page- und HTMX-Fallback | PASS |
| UI-Listen und Eröffnungs-/Änderungsflows | PASS |
| INTERLIS-/PostgreSQL-/Cayenne-Konsistenz | PASS |

## Known limitations / gate decision

- Beteiligte, Aufgaben und „Meine Arbeit“ bleiben wie spezifiziert in Phase 4.
- Dokumentdateien, Uploads und Unterlagen-Use-Cases bleiben wie spezifiziert
  ausserhalb dieser Phase.
- Journal-Akteure verwenden für technische Dev-/Fixture-Anfragen den
  konfigurierbaren Fallback-Benutzer `anna.mueller`; eine produktive
  Identitätszuordnung gehört in die spätere Security-Integration.

Phase 3 ist vollständig grün und wird als `SUCCESS` abgeschlossen. Aufgrund
der ausdrücklichen Benutzerfreigabe zum autonomen Fortfahren wird anschließend
Phase 4 begonnen.
