# PHASE_4_REPORT

- Status: SUCCESS
- Phase: 4 – Beteiligte, Aufgaben, Meine Arbeit
- Date: 2026-08-16
- Scope / Use cases: UC-001, UC-012, UC-013, UC-019 bis UC-022, UC-044 teilweise

## Implemented

- Beteiligte erfassen, ändern, suchen und anzeigen; unterstützt Personen,
  Organisationen und interne Organisationseinheiten.
- Beteiligungen mit aktiver Rollenvalidierung, Gültigkeitsintervall,
  Rollenbezeichnung, Bemerkung, Beendigung und Journalereignis.
- Aufgaben mit fachlichem Aufgabentyp, offenem Status, Fälligkeit, Priorität,
  Benutzer-/Organisationseinheitszuweisung und Clock-basierter Zeitführung.
- Aufgaben bearbeiten, starten, delegieren, erledigen und abbrechen; erledigte
  oder abgebrochene Aufgaben sind nicht normal editierbar.
- Atomare Aufgabenjournalisierung für Erstellung, Statuswechsel, Zuweisung,
  Änderung und Erledigung.
- `AufgabeQueryService` für Geschäft, eigene offene Aufgaben und überfällige
  Aufgaben; Sortierung nach Fälligkeit, Priorität und Titel.
- „Meine Arbeit“ mit offenen/überfälligen Aufgaben, aktiven Geschäften,
  fälligen Geschäften und zuletzt geänderten Geschäften.
- Teilansicht `GeschaeftskontrolleQueryService` mit offenen und überfälligen
  Geschäften/Aufgaben, Prozessstatusverteilung und inaktiven Fällen.
- Geschäftsdetail mit Aufgabenliste, Aufgaben-Erfassung sowie Start-/Erledigt-
  Aktionen; Beteiligungen werden im Detail angezeigt.

## Tests and gate evidence

- `scripts/validate-model.sh` und positive XTF-Fixtures bleiben PASS.
- Frischer PostgreSQL/Testcontainers-Import und Cayenne-Persistenztests PASS.
- `./gradlew check --no-daemon` – PASS; 22 Tests inklusive
  PostgreSQL/Cayenne-Integration, MVC/JTE, Security, Rollenvalidierung,
  Aufgabenstatus, Journalisierung und Dashboard-Kontrolle.
- Browser-Smoke – PASS: Dashboard im Lesemodus, authentifizierte Dashboard-
  Ausgabe per HTTP-Basic mit „Meine offenen Aufgaben“, „Überfällige Aufgaben“
  und „Aktive Geschäfte“ sowie Geschäftsdetail mit acht fixture-basierten
  Aufgaben und sechs Beteiligungen.
- Offizieller Cayenne-5.0-M2-MCP – PASS:
  `open_project` mit Modeler-Handshake;
  `dbimport_run` `up_to_date`, 0 Änderungen, JDBC-Verbindung validiert;
  `cgen_run` `up_to_date`, 44 Dateien betrachtet, 0 geschrieben, keine
  Warnungen.

## Acceptance criteria

| Criterion | Result |
|---|---|
| Beteiligte erfassen | PASS |
| Beteiligung nur mit aktiver Rolle und gültigem Zeitraum | PASS |
| Aufgabe zwingend an ein Geschäft binden | PASS |
| Aufgabe erstellen, bearbeiten und delegieren | PASS |
| Aufgabe erledigen mit `erledigtAm` und Journal | PASS |
| Erledigte/abgebrochene Aufgaben nicht normal editieren | PASS |
| Eigene offene und überfällige Aufgaben | PASS |
| „Meine Arbeit“ nur mit relevanten Daten | PASS |
| Geschäftskontrolle teilweise | PASS |
| UI-Integration im Geschäftsdetail | PASS |
| INTERLIS-/PostgreSQL-/Cayenne-Konsistenz | PASS |

## Known limitations / gate decision

- Globale Duplikaterkennung für Beteiligte bleibt bewusst ein Hinweis-/Folge-
  thema; es gibt keinen automatischen globalen Merge.
- Die produktive Benutzeridentität bleibt bis zur späteren Security-Integration
  von der Dev-Authentifizierung getrennt; Journal-Fallback ist dokumentiert.
- Dokumentdateien, Uploads, Fachsystemreferenzen, globale Suche und vollständige
  Abschluss-/Datenqualitätsfunktionen bleiben in den dafür vorgesehenen Phasen.

Phase 4 ist vollständig grün und wird als `SUCCESS` abgeschlossen. Aufgrund
der ausdrücklichen Benutzerfreigabe zum autonomen Fortfahren wird anschließend
Phase 5 begonnen.
