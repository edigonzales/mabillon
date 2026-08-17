# PHASE_6_REPORT

- Status: SUCCESS
- Phase: 6 – Fachsystemreferenzen, Suche, Geschäftskontrolle
- Date: 2026-08-16
- Scope / Use cases: UC-023, UC-043, UC-044

## Implemented

- `FachsystemReferenzService` für Referenzen an Dossier und Geschäft mit
  Systemcode, Objekttyp, Objekt-ID, Mutations-ID, Link, Beschreibung,
  Dossier-/Geschäftskontext und Journalisierung.
- Fachsystemreferenzen werden bei Dossierabfragen inklusive der Referenzen
  seiner Geschäfte angezeigt; geschlossene Geschäfts-/Dossierobjekte bleiben
  schreibgeschützt.
- Normale HTTP-Formulare für Fachsystemreferenzen in Dossier- und
  Geschäftsdetail; externe Links werden als solche geöffnet.
- `GlobalSearchService` mit Freitext, Geschäfts-/Dossiernummer, Titel,
  Beteiligtem, Organisation, Geschäftsart, Prozessstatus, Unterlagentitel und
  Fachsystem-ID sowie stabiler Pagination.
- Globale HTML-Suche unter `/suche`, ohne Volltextsuche in Binärdateien und
  ohne zusätzliche Suchplattform.
- `GeschaeftskontrolleQueryService` für offene und überfällige Geschäfte und
  Aufgaben, Prozessstatusverteilung sowie seit einer konfigurierbaren Anzahl
  Tagen inaktive Geschäfte.
- Leitungssicht unter `/geschaeftskontrolle` mit Kennzahlen und Listen.
- Dashboard-Sortierung so angepasst, dass die neuesten aktiven Geschäfte in
  der begrenzten „Meine Arbeit“-Ansicht sichtbar bleiben.

## Tests and gate evidence

- `scripts/validate-model.sh` und positive XTF-Fixtures bleiben PASS; das
  INTERLIS-Modell wurde in Phase 6 nicht verändert.
- `./gradlew test --tests 'guru.interlis.mabillon.Phase0CompatibilityTest.phaseSix*' --no-daemon` – PASS; 3 gezielte Phase-6-Tests.
- `./gradlew check --no-daemon` – PASS; 27 PostgreSQL/Testcontainers-,
  Cayenne-, Security-, MVC/JTE-, Storage- und Fachregeltests.
- MVC-/JTE-Abdeckung: Suche und Geschäftskontrolle rendern über normale
  HTTP-Anfragen; Service-Integration deckt Referenzanlage, Dossier-/Geschäfts-
  zuordnung, Journal und strukturierte Suche mit Pagination ab.
- Browser-Smoke – PASS: `/suche?q=Bodenrain` zeigt Dossier, Geschäft und
  Unterlagen des Golden Path; `/geschaeftskontrolle` rendert die
  Leitungssicht inklusive leerer Kennzahlen bei ausschliesslich geschlossenen
  Fixture-Geschäften.
- Offizieller Cayenne-5.0-M2-MCP – PASS:
  `initialize` meldet `cayenne-mcp-server 5.0-M2`;
  `open_project` bestätigt Modeler-Handshake;
  `dbimport_run` mit `dataMap=mabillon` meldet `up_to_date`, 0 Änderungen und
  eine erfolgreiche JDBC-Verbindung zu `jdbc:postgresql://localhost:55432/mabillon`;
  `cgen_run` meldet `up_to_date`, 44 Dateien betrachtet, 0 geschrieben und
  keine Warnungen.

## Acceptance criteria

| Criterion | Result |
|---|---|
| Fachsystemreferenz an Geschäft erfassen | PASS |
| Fachsystemreferenz an Dossier erfassen | PASS |
| Referenzdaten und externe Links anzeigen | PASS |
| Referenzaktionen journalisieren | PASS |
| Systemweite strukturierte Suche | PASS |
| Filter und Pagination | PASS |
| Keine Binär-Volltextsuche | PASS |
| Offene und überfällige Geschäfte | PASS |
| Offene und überfällige Aufgaben | PASS |
| Prozessstatusverteilung | PASS |
| Inaktive Geschäfte seit n Tagen | PASS |
| Leitungssicht über normale HTTP-Anfrage | PASS |
| INTERLIS-/PostgreSQL-/Cayenne-Konsistenz | PASS |

## Known limitations / gate decision

- Die Suche indexiert keine Binärinhalte; gesucht werden persistierte
  Metadaten und fachliche Referenzfelder gemäß Scope.
- Die produktive Identitäts-/Rollenverwaltung bleibt bis zur vorgesehenen
  Security-Integration bei der lokalen Dev-Authentifizierung.
- Vollständige Abschluss-, Datenqualitäts-, Austausch- und Archivfunktionen
  bleiben in den dafür vorgesehenen Folgephasen.

Phase 6 ist vollständig grün und wird als `SUCCESS` abgeschlossen. Aufgrund
der ausdrücklichen Benutzerfreigabe zum autonomen Fortfahren wird anschließend
Phase 7 begonnen.
