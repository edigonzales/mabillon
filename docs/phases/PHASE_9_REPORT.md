# PHASE_9_REPORT

- Status: `SUCCESS`
- Phase: 9 – Aussonderung, Archivablieferung, SIP
- Datum: 2026-08-16
- Scope: UC-037 bis UC-042

## Profilentscheid

Das erste Zielprofil ist `ech-0160-1.3.0`. Die offizielle eCH-Spezifikation 1.3.0 ist als versionierte Projektentscheidung dokumentiert; ihr XSD-Set liegt lokal unter `docs/archive/profiles/ech-0160-1.3.0/xsd/`. Das XSD verwendet intern `schemaVersion="5.1"`. Das BAR-spezifische SIP-Profil bleibt eine spätere Profilvariante und wird nicht unberechtigt als BAR-Kompatibilität behauptet.

- [eCH-0160 Archivische Ablieferungsschnittstelle 1.3.0](https://www.ech.ch/sites/default/files/imce/eCH-Dossier/eCH-Dossier_PDF_Publikationen/Hauptdokument/STAN_d_DEF_2024-07-18_eCH-0160_V1.3.0_ArchivischeAblieferungsschnittstelle.pdf)
- [Bundesarchiv: Digitale Unterlagen abliefern](https://www.bar.admin.ch/de/digitale-unterlagen-abliefern)

## Umgesetzt

- `AussonderungQueryService` für geschlossene, nicht übernommene/vernichtete und datenqualitätsgeprüfte Dossiers.
- `ArchivAblieferungService` für Erstellen, Zusammenstellen, Bereitmarkieren, Übergabe, Übernahme und Ablehnung.
- `SipGenerator`/`SipValidator` als erweiterbare SPI mit konkretem eCH-0160-Generator und XSD-/Datei-/Hash-Validator.
- Strukturierte SIP-Pakete mit `header/metadata.xml`, lokalem `header/xsd/` und `content/`; kein reiner ZIP-Export.
- Persistente `Sippaket`-Erzeugungsversuche mit Laufnummer, Status, Hash, Grösse, Storage-URI und Validierungsbericht-URI.
- DQ-013 als ERROR für nicht geschlossene Dossiers in Archivablieferungen.
- Transaktionale Journalierung von SIP-Erzeugung, Validierung, Übergabe, Übernahme und Ablehnung.
- Nach erfolgreicher Übernahme werden Archivierungsstatus von Dossier/Geschäft atomar aktualisiert.
- Weboberfläche unter `/archivierung` und `/archivierung/{ablieferungsnummer}` mit normalen HTTP-Fallbacks.
- Golden-Path-/Negativtests: ungültiges SIP, Korrekturstatus, zweiter Erzeugungsversuch, XSD-Validierung, Hash-/Dateiprüfung und Ablehnung eines offenen Dossiers.

## Gate-Nachweise

- `./gradlew check --no-daemon`: PASS, 39 Tests, 0 Fehler.
- Phase-9-PostgreSQL-Integration: Nomenklaturtestdossier wird als strukturiertes SIP erzeugt, absichtlich ungültig gemacht, als Korrekturfall erkannt, erneut erzeugt, lokal gegen das eCH-XSD validiert und anschliessend übernommen.
- Das validierte SIP enthält die XSD-Beilagen, Metadaten, Content-Datei und passende SHA-256-Prüfsumme.
- Mehrere SIP-Versuche bleiben persistent nachvollziehbar.
- `scripts/validate-model.sh`: PASS mit ili2c 5.6.8.
- `scripts/validate-xtf.sh`: PASS für alle drei vorhandenen positiven XTF-Fixtures mit ilivalidator 1.15.0.
- Cayenne-MCP: PASS; Server `cayenne-mcp-server 5.0-M2`, Projekt-Handshake erfolgreich, `dbimport_run` `up_to_date` mit 0 Änderungen und `cgen_run` `up_to_date` mit 44 geprüften und 0 geschriebenen Dateien.

## Acceptance Criteria

| Kriterium | Ergebnis |
|---|---|
| Nur geschlossene Dossiers | PASS |
| DQ-Errors blockieren SIP/Ablieferung | PASS |
| Alle Dateien im SIP vorhanden | PASS |
| Hashes stimmen | PASS |
| XML gegen lokales Zielprofil-XSD gültig | PASS |
| Absichtlich defektes SIP wird ungültig erkannt | PASS |
| Mehrere Erzeugungsversuche nachvollziehbar | PASS |
| Übernahme aktualisiert Dossier/Geschäft und Journal atomar | PASS |
| Cayenne-/DB-Konsistenz nach MCP-Import und cgen | PASS |

Phase 9 ist vollständig abgeschlossen.
