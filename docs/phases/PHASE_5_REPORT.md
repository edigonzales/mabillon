# PHASE_5_REPORT

- Status: SUCCESS
- Phase: 5 – Unterlagen und Storage
- Date: 2026-08-16
- Scope / Use cases: UC-014 bis UC-018

## Implemented

- `DocumentStorage`-SPI mit `stage`, `commit`, `open`, `exists` und `discard`.
- `FileSystemDocumentStorage` mit konfigurierbarem Root, Stagingbereich und
  objektbasierter Ablage unter `objects/<sha256-prefix>/<uuid>`.
- Originaldateinamen werden ausschließlich als Metadaten behandelt und nie zur
  Pfadbildung verwendet; Storage-URIs werden auf den Ablageroot begrenzt.
- SHA-256 und Dateigröße werden serverseitig beim Staging ermittelt und beim
  Commit erneut verifiziert.
- `UnterlageService.register` mit Dossierpflicht, optionalem Geschäftskontext,
  aktivem Unterlagentyp, Eingangs-/Ausgangsdaten, Metadaten, Journal und
  Aufräumen bei Persistenzfehlern.
- `UnterlageService.assignToGeschaeft` mit harter Cross-Dossier-Prüfung sowie
  nachvollziehbarem Storno statt physischer Löschung.
- `UnterlageQueryService` für Dossier, Geschäft und Einzelunterlage sowie
  `UnterlageContentService` für die Integritäts-/Existenzprüfung und das Öffnen.
- `EmailRegistrationService` für eingehende und ausgehende E-Mails ohne
  Mailboxintegration.
- Multipart-Registrierung am Dossier und Download mit MIME-Type,
  Content-Length und Content-Disposition.
- Dossier-/Geschäftsdetail zeigen verwaltete Dateien zum Download; importierte
  externe Fixture-URIs werden nicht fälschlich als lokale Downloads angeboten.

## Tests and gate evidence

- `scripts/validate-model.sh` und positive XTF-Fixtures bleiben PASS; das
  INTERLIS-Modell wurde in Phase 5 nicht verändert.
- `./gradlew check --no-daemon` – PASS; 24 PostgreSQL/Testcontainers-,
  Cayenne-, Security-, MVC/JTE-, Storage- und Fachregeltests.
- Storage-Integration: SHA-256, sichere Ablage trotz `../../`-Dateiname,
  Download-Bytes, Dossierkonsistenz und Cleanup nach fehlgeschlagener
  Unterlagenregistrierung – PASS.
- Browser-Smoke – PASS: Dossierdetail zeigt die Unterlagenliste ohne defekte
  Links für externe Fixture-Ablagen; verwaltete Downloads sind vorgesehen.
- Offizieller Cayenne-5.0-M2-MCP – PASS:
  `open_project` mit Modeler-Handshake;
  `dbimport_run` `up_to_date`, 0 Änderungen, JDBC-Verbindung validiert;
  `cgen_run` `up_to_date`, 44 Dateien betrachtet, 0 geschrieben, keine
  Warnungen.

## Acceptance criteria

| Criterion | Result |
|---|---|
| Dateiinhalt nicht als PostgreSQL-BLOB | PASS |
| Staging und Commit über DocumentStorage-SPI | PASS |
| Pfadtraversal über Originaldateiname ausgeschlossen | PASS |
| SHA-256 serverseitig berechnet und verifiziert | PASS |
| Unterlage zwingend einem Dossier zugeordnet | PASS |
| Optionaler Geschäftskontext mit Dossierkonsistenz | PASS |
| Upload-/Metadatenregistrierung und Journal | PASS |
| Download mit MIME-/Disposition-Headern | PASS |
| Eingehende und ausgehende E-Mail als Unterlage | PASS |
| Storno bleibt auditierbar, normale physische Löschung verboten | PASS |
| DB-/Storage-Fehler räumen Staging auf | PASS |
| INTERLIS-/PostgreSQL-/Cayenne-Konsistenz | PASS |

## Known limitations / gate decision

- Für bereits importierte externe `gever://`- oder `file://`-URIs gibt es in
  dieser Phase keinen externen Storage-Adapter; sie werden als nicht lokal
  verwaltete Inhalte behandelt und nicht zum lokalen Download verlinkt.
- Vollständige Dokumentversionierung bleibt gemäß Spezifikation außerhalb des
  MVP.
- Virenscan, OCR und Mailboxintegration sind nicht Bestandteil des Scopes.

Phase 5 ist vollständig grün und wird als `SUCCESS` abgeschlossen. Aufgrund
der ausdrücklichen Benutzerfreigabe zum autonomen Fortfahren wird anschließend
Phase 6 begonnen.
