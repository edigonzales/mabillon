# Phase 0 – Fachmodell und technische Machbarkeit

**Datum:** 2026-08-16  
**Status:** SUCCESS  
**Scope:** INTERLIS-Modell, XTF-Fixtures, ili2pg/PostgreSQL-Pipeline,
Cayenne-5.0-M2-Modeler-MCP/DB-Import/cgen, Minimal-Spike

## Ergebnis

Die fachlichen Phase-0-Modellergänzungen und der technische Baseline-Spike sind
implementiert:

- `Geschaeftsart.resultatErforderlich` ergänzt.
- Archivablieferung, SIP-Paket, Statuswerte, Beziehungen und neue Ereignistypen
  im INTERLIS-Modell ergänzt.
- Golden Path auf `AGI-D-2026-000007` und `AGI-G-2026-000421` aktualisiert.
- Vier positive XTF-Fixtures validiert, inklusive `ArchivAblieferung` und
  `SipPaket`.
- ili2pg-/ilivalidator-Skripte und vier Architektur-ADRs erstellt.
- Minimaler Spring-Boot-4.1.0-/JTE-3.2.4-/Cayenne-5.0-M2-Spike mit Gradle
  Wrapper erstellt. Spring startet, JTE rendert ein lokales Template und die
  Cayenne-5.0-M2-Runtime startet über die neue `CayenneRuntime`-Builder-API.
- HTMX 2.0.10 liegt lokal unter
  `src/main/resources/static/htmx-2.0.10.min.js`.
- Cayenne DataMap und Projektdescriptor wurden über den offiziellen
  Cayenne-5.0-M2-MCP-Server geprüft und für cgen konfiguriert.
- Die DataMap enthält 22 fachliche DB-Entities, 22 ObjEntities, 74
  DB-Relationships und 74 ObjRelationships; PostGIS-Views und
  `t_ili2db`-Techniktabellen sind ausgeschlossen.
- cgen erzeugt 44 Dateien für 22 ObjEntities unter
  `build/generated/sources/cayenne/`; alle generierten Dateien kompilieren.

## Gate-Nachweise

| Prüfung | Ergebnis |
|---|---|
| ili2pg 5.5.2 am vorgeschriebenen Pfad | PASS |
| ili2c 5.6.8 am vorgeschriebenen Pfad | PASS |
| ilivalidator 1.15.0 am vorgeschriebenen Pfad | PASS |
| `scripts/validate-model.sh` | PASS |
| `scripts/validate-xtf.sh` für alle vier positiven Fixtures | PASS |
| PostgreSQL-Schemaimport mit `--createTidCol --createBasketCol` | PASS |
| Import mit `--importTid --importBid` | PASS |
| TID-/BID-Erhalt, FK/Checks, Golden Path | PASS |
| Ungültiger XTF vor Import abgewiesen | PASS; Exit 1, Datenbank unverändert |
| Gültiger kombinierter ili2pg-XTF-Export | PASS; danach ilivalidator PASS |
| Spring-/JTE-/Cayenne-Minimal-Spike | PASS |
| Java-Kompilation und vollständiger `./gradlew build` | PASS |
| Agent-Skill-Syntaxprüfung | PASS; `OK: 8 skills` |
| Cayenne DB Import über den vorgeschriebenen Modeler-MCP | PASS; Erstimport 22 fachliche Entities, Folgelauf 0 Änderungen |
| Cayenne cgen und Mapping-Diff | PASS; 44 Dateien, 22 Paare, keine Warnungen |
| MCP Smoke Test | PASS; `initialize`, `tools/list`, `open_project`, `dbimport_run`, `cgen_run` |

## PostgreSQL-Nachweis

Der Lauf wurde in einer frischen PostgreSQL-16/PostGIS-Testdatenbank mit
`scripts/create-schema.sh` und anschließend in der Reihenfolge Kataloge,
Stammdaten, Geschäftsdaten importiert.

Ergebnis der bekannten Objekte:

- `geschaeft`: 1, `dossier`: 1, `unterlage`: 9
- `archivablieferung`: 1, `sippaket`: 1
- `benutzer`: 3, `organisationseinheit`: 4, `geschaeftsart`: 2
- Basket-TIDs: `c4dbb2a2-9b06-525d-b2d9-e69b8d9e7013`,
  `8f63ea47-4dd4-5dc9-9fd2-ab9a9f9f40fe`,
  `ada09d02-2110-5e46-afa6-ea7426d960bc`
- Fremdschlüssel: 60
- Check-Constraints: 133
- `geschaeft` enthält die technischen Spalten `t_id`, `t_basket` und
  `t_ili_tid`; die fachliche Nummer bleibt `AGI-G-2026-000421`.

Der kombinierte Export wurde mit den drei Basket-IDs als semikolongetrenntem
`--baskets`-Wert erstellt und von ilivalidator 1.15.0 akzeptiert. Die
bekannten TIDs des Dossiers, Geschäfts, der Ablieferung und des SIP-Pakets
blieben im Export erhalten.

## Cayenne-MCP-Nachweis

Der konfigurierte offizielle stdio-MCP-Server meldet `cayenne-mcp-server
5.0-M2` und liefert die drei vorgesehenen Funktionen `open_project`,
`dbimport_run` und `cgen_run`.

- `open_project` startete CayenneModeler 5.0-M2 und bestätigte den MCP-
  Handshake.
- `dbimport_run` validierte DataMap, DBConnector, JDBC-Treiber und
  PostgreSQL-Verbindung. Der bereinigte Erstimport enthält nur die 22
  Tabellen im Schema `mabillon`; der anschließende Lauf war `up_to_date` mit
  0 Änderungen.
- `cgen_run` erzeugte 44 Dateien ohne Warnungen. Der Folgelauf war
  `up_to_date` mit 44 betrachteten und 0 geschriebenen Dateien.

Die Desktop-Toolkatalogisierung zeigt den Server in dieser Session weiterhin
nicht als direkt callable Nested Tool. Das ist eine Katalogisierungsgrenze der
Desktop-Umgebung, kein MCP-Serverfehler: der konfigurierte stdio-Endpunkt hat
alle drei Aktionen tatsächlich ausgeführt.

## Technische Notiz

ili2pg 5.5.2 erzeugt mit dem geforderten `--createMandatoryChecks` auch für
fachlich optionale Referenzattribute und `{0..1}`-Rollen `IS NOT NULL`-Checks.
`--sqlEnableNull` wäre kein geeigneter Workaround, weil damit auch die echten
Mandatory-Constraints entfallen würden. `scripts/create-schema.sh` führt daher
nach dem Schemaimport die deterministische
`SchemaConstraintRepair.java`-Korrektur aus und entfernt ausschließlich die
18 bekannten falschen Optional-Referenz-Checks. Die Pflicht-Constraints aus
ili2pg bleiben erhalten. Ein frischer PostgreSQL-Lauf mit anschließendem
Fixture-Import bestätigt, dass optionale Prozess-/Resultatstatuswerte und
optionale Kontextreferenzen nun fachlich korrekt `NULL` sein können.

## Known limitations / gate decision

- Der DB-Import meldet PostgreSQLs `uuid`-Spalte `t_ili_tid` über den JDBC-
  Treiber als `OTHER`. Für die ObjAttributes ist deshalb explizit
  `java.util.UUID` konfiguriert. Keine generierte Cayenne-Basisklasse wurde
  manuell geändert.
- Der tatsächliche Cayenne-ObjectContext-Lese-/Schreib-/Rollback-Smoke-Test
  gehört zum vorgesehenen Phase-1-Persistence-Slice.
- Seit Phase 10 werden die MCP-generierten Quellen unter `src/generated/java/`
  reproduzierbar abgelegt. Sie bleiben generierte Artefakte und werden nach
  jeder Modell-/Mappingänderung ausschließlich durch DB-Import und cgen über
  MCP aktualisiert.

Alle Phase-0-Acceptance-Criteria sind grün. Phase 0 wird als `SUCCESS`
abgeschlossen. Zum Zeitpunkt dieses Reports wurde noch keine Phase-1-
Fachfunktion begonnen; die anschließende Phase-1-Arbeit erfolgt aufgrund der
expliziten Benutzerfreigabe zum autonomen Fortfahren.
