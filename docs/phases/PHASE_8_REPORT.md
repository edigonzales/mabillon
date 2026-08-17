# PHASE_8_REPORT

- Status: `SUCCESS`
- Phase: 8 – INTERLIS-Datenaustausch
- Datum: 2026-08-16
- Scope: UC-034, UC-035, UC-036

## Umgesetzt

- `InterlisModelValidator`, `XtfValidator` und `Ili2pgRunner` als getrennte SPI.
- Sichere `ProcessBuilder`-Adapter für ili2c 5.6.8, ilivalidator 1.15.0 und ili2pg 5.5.2.
- Zentralisierte Toolpfade, Versionen und Model-/Modeldir-Konfiguration in `InterlisToolDefaults`.
- `InterlisExchangeService` für Kataloge, Stammdaten und Geschäftsdaten.
- Vor jedem Import: ilivalidator-Prüfung, Topic-Prüfung und anschliessend zwingend `--importTid --importBid`.
- Nach erfolgreichem Import: ili2pg-Datenvalidierung und verständlicher Fehlerhinweis bei fehlenden Cross-Topic-Abhängigkeiten.
- Exporte mit `--exportTid`, Topic-/Basket-Auswahl, anschliessender ilivalidator-Prüfung und Entfernung fehlerhafter Dateien.
- Schlanke Admin-Oberfläche unter `/admin/interlis` mit Multipart-Import und Download-Export.
- Reproduzierbarer CLI-Export über `scripts/export-xtf.sh`.

## Gate-Nachweise

- `scripts/validate-model.sh`: PASS mit ili2c 5.6.8.
- `scripts/validate-xtf.sh`: PASS für alle vier positiven XTF-Fixtures.
- `scripts/export-xtf.sh catalog /tmp/...`: PASS einschließlich anschließender ilivalidator-Prüfung.
- `./gradlew check --no-daemon`: PASS, 36 Tests, 0 Fehler.
- Adaptertests: ungültiges XTF erreicht ili2pg nicht; Importflags sind immer TID/BID=true; fehlerhafte Exporte werden entfernt; echtes Modell und positives XTF werden mit den lokalen Adaptern validiert.
- PostgreSQL-Integration: exportierter Katalog behält bekannte TID/BID und Objektbestand; der Export besteht ilivalidator.
- HTTP-Smoke-Test: `/admin/interlis` rendert für Administratoren.
- Cayenne-MCP: PASS; Server `cayenne-mcp-server 5.0-M2`, Projekt-Handshake erfolgreich, `dbimport_run` `up_to_date` mit 0 Änderungen, `cgen_run` `up_to_date` mit 44 geprüften und 0 geschriebenen Dateien.

## Acceptance Criteria

| Kriterium | Ergebnis |
|---|---|
| XTF vor jedem Import validiert | PASS |
| Export nach ili2pg erneut validiert | PASS |
| Katalog-, Stamm- und Geschäftsdatenpfad | PASS |
| `--importTid` und `--importBid` | PASS |
| Topic-/Basket-Zuordnung und bekannte Identitäten | PASS |
| Cross-Topic-Fehler werden nicht als erfolgreicher Import gemeldet | PASS |
| Ungültige Eingabe ohne ili2pg-Aufruf | PASS |
| Cayenne-/DB-Konsistenz nach MCP-Import und cgen | PASS |

## Abgrenzung

- Ein Export ist eine validierte XTF-Datei, kein Archiv-/SIP-Paket; SIP und Archivzielprofil gehören in Phase 9.
- Die produktive Benutzer-/Jobverwaltung bleibt bewusst schlank; Importresultate werden synchron als Ergebnis angezeigt.

Phase 8 ist vollständig abgeschlossen.
