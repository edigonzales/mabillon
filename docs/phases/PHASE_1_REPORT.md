# PHASE_1_REPORT

- Status: SUCCESS
- Phase: 1 – Projektbasis, Persistence und Read-only Vertical Slice
- Date: 2026-08-16
- Scope / Use cases: Spring-Boot-Projektbasis, CayenneRuntime,
  CayenneUnitOfWork, PostgreSQL-Testcontainer, read-only Dossier/Geschäft,
  JTE-Layout, Vanilla-CSS und HTMX-Progressive-Enhancement

## Implemented

- `CayenneConfiguration` baut die Cayenne-5.0-M2-Runtime über die neue
  `CayenneRuntime`-Builder-API und lädt den Projekt-Descriptor aus dem
  Classpath.
- `CayenneUnitOfWork` kapselt kurzlebige ObjectContexts. Read-Operationen
  erhalten einen eigenen Context; Write-Operationen committen und rollen bei
  Runtime-Exceptions zurück.
- `DossierQueryService` und `GeschaeftQueryService` verwenden typisierte
  Cayenne-ObjectSelect-Abfragen und bilden sofort in View-Modelle ab.
- `DossierController` und `GeschaeftController` liefern normale Full-Page-
  Antworten sowie bei `HX-Request: true` wiederverwendbare JTE-Fragmente.
- `HtmxRequest` zentralisiert die Erkennung des HTMX-Headers.
- Die Startseite und die beiden read-only Detailseiten sind vorhanden:
  `/`, `/dossiers/{number}` und `/geschaefte/{number}`.
- JTE-Layout, Navigation, Dossier-/Geschäftsdetail und eine eigene Vanilla-
  CSS-Grundsprache wurden nach der gepinnten ili2grails-Designsprache angelegt.

## Persistence / UI details

- Das Runtime-Override verwendet URL, Benutzer und Passwort aus Spring-
  Properties; keine Zugangsdaten sind im Repository hinterlegt.
- Die Integrationstests starten `sogis/postgis:16-3.5` über Testcontainers,
  deklarieren es explizit als kompatiblen PostgreSQL-Ersatz und erzeugen das
  Schema ausschließlich über die vorhandenen ili2pg-Skripte.
- Der Golden Path wird in jedem Testlauf frisch aus Katalog-, Stamm- und
  Geschäftsdaten-Fixture importiert.
- Templates erhalten ausschließlich View-Modelle, niemals Cayenne-Objekte.

## Tests added

- Spring-Boot-/JTE-Context- und MVC-Tests.
- Echter Cayenne-Datasource-Test gegen PostgreSQL/PostGIS-Testcontainer.
- Nomenklatur-Dossier-Abfrage mit 1 Geschäft und 9 Unterlagen.
- Nomenklatur-Geschäft-Abfrage mit Dossierverknüpfung und Unterlagen.
- Full-Page- und HTMX-Fragment-Fallback-Test.
- Read-Unit-of-Work-Smoke-Test.
- Browser-Smoke über die Playwright-Oberfläche des In-App-Browsers gegen den
  lokal gestarteten Boot-Server: Startseite, Dossier und Geschäft geladen.

## Tests and commands executed

- `./gradlew compileJava --no-daemon` – PASS.
- `./gradlew testClasses --no-daemon` – PASS.
- `./gradlew test --no-daemon` – PASS; 6 Tests.
- `./gradlew clean --no-daemon` – PASS.
- Cayenne `cgen_run` über den konfigurierten offiziellen MCP-Server nach dem
  Clean-Lauf – PASS; 44 Dateien betrachtet und 44 geschrieben.
- `./gradlew check --no-daemon` – PASS.
- Lokaler `bootRun` auf Port 18080 mit dem Phase-0-PostgreSQL-Testschema –
  PASS.
- Browser-Smoke gegen `http://127.0.0.1:18080/` – PASS:
  - Übersicht rendert Mabillon und beide Golden-Path-Schnellzugriffe.
  - Dossier `AGI-D-2026-000007` rendert 1 Geschäft und 9 Unterlagen.
  - Geschäft `AGI-G-2026-000421` rendert die Dossierverknüpfung und 9
    Unterlagen.

## Acceptance criteria

| Criterion | Result |
|---|---|
| Spring Boot 4.1.0 / Java 25 / JTE / Cayenne 5.0-M2 | PASS |
| Cayenne Runtime über neue Builder-API | PASS |
| Explizite CayenneUnitOfWork-Abstraktion | PASS |
| PostgreSQL-Testcontainers statt H2 | PASS |
| Read-only Dossier-Zugriff | PASS |
| Read-only Geschäft-Zugriff | PASS |
| Startseite `/` | PASS |
| JTE-Layout und Vanilla-CSS | PASS |
| HTMX eingebunden, Full-Page-Fallback erhalten | PASS |
| Context-, Runtime-, Query- und MVC-Tests | PASS |
| Browser-Smoke | PASS |
| Vollständiger Check nach Clean und MCP-cgen | PASS |

## Known limitations / gate decision

- Schreibende Fach-Use-Cases, Security/Rollen, Katalogpflege und
  Registraturplanpflege gehören zu den folgenden Phasen und wurden nicht
  vorgezogen.
- Die MCP-generierten Quellen liegen seit Phase 10 unter
  `src/generated/java/`, damit `./gradlew clean check` auch in CI und im
  Deployment reproduzierbar ist. Die Dateien bleiben generierte Artefakte und
  dürfen nicht manuell verändert werden; Änderungen erfolgen ausschließlich
  über den DB-first-MCP-Workflow.

Phase 1 ist vollständig grün und wird als `SUCCESS` abgeschlossen. Zum
Zeitpunkt dieses Reports wurde noch keine Phase-2-Fachfunktion begonnen.
