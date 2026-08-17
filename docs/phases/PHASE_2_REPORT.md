# PHASE_2_REPORT

- Status: SUCCESS
- Phase: 2 – Security, Kataloge, Stammdaten und Registraturplan
- Date: 2026-08-16
- Scope / Use cases: UC-027 bis UC-033

## Implemented

- Spring-Security-Basis mit vier Mabillon-Rollen und dem vorgesehenen
  Permission-Modell.
- Definierte Dev-/Test-Identitäten für `admin` und `sachbearbeiter`; die
  produktive Authentifizierung bleibt von der Fachlogik getrennt und kann
  später durch OIDC ersetzt werden.
- `CurrentActor` und `AuthorizationService` als Service-Layer-Enforcement.
  Katalog-, Stammdaten- und Registraturplan-Schreibservices prüfen die
  Berechtigung unabhängig vom Controller.
- Katalog-Read-/Create-/Activate-/Deactivate-Flows für Geschäftsarten,
  Prozessstatus, Resultatstatus, Beteiligungsrollen, Unterlagentypen und
  Aufgabentypen.
- Katalogwerte werden bei Deaktivierung nicht gelöscht. Codes bleiben nach
  Erstellung unveränderlich; verwendete historische Werte bleiben lesbar.
- Prozessstatus werden einer Geschäftsart zugeordnet. Es wird höchstens ein
  Initialstatus angelegt, und `initialProcessStatus` erzwingt genau einen
  Initialstatus je Geschäftsart.
- Organisationseinheiten und Benutzer als Cayenne-Services mit aktiver /
  inaktiver Verwaltung, eindeutigen Codes und fachlichen Beziehungen.
- Registraturplan-Query als Baumdarstellung mit aktiven Blattpositionen für
  neue Dossiers.
- Registraturplan-Administration für Plan-/Positionsanlage, Statuspflege,
  Verschieben, Ersetzen und Deaktivieren. Eine Verschiebung in einen eigenen
  Nachfahren wird atomar abgewiesen; verwendete Positionen werden nicht
  physisch gelöscht.
- Geschützte Admin-URLs unter `/admin/**`, HTTP-Basic nur für die Dev-/Test-
  Identitäten, normale CSRF-Prüfung und lokale CSRF-Tokens in den Formularen.
- Admin-JTE-Seiten für Kataloge, Stammdaten und Registraturplan im bestehenden
  neutralen Vanilla-CSS-/ili2grails-Muster.

## Persistence / model consistency

- Es war keine fachliche INTERLIS-Änderung erforderlich: Das Phase-0-Modell
  enthält bereits alle benötigten Katalog-, Stamm- und Ordnungssystemobjekte.
- Neue Cayenne-Objekte verwenden den vorhandenen fachlich passenden Basket;
  `t_ili_tid` wird als UUID gesetzt. Generierte Cayenne-Basisklassen wurden
  nicht manuell verändert.
- Nach einem Clean-Lauf meldete Cayenne `dbimport_run` `up_to_date` mit 0
  Änderungen. Der anschließende `cgen_run` erzeugte 44 Dateien ohne Warnungen.

## Tests and commands executed

- `./gradlew compileJava --no-daemon` – PASS.
- `./gradlew test --no-daemon` – PASS; 13 PostgreSQL/Testcontainers-,
  Security-, MVC- und Cayenne-Tests.
- `./gradlew clean --no-daemon` – PASS.
- Offizieller Cayenne-5.0-M2-MCP:
  `open_project` – PASS, Modeler-Handshake bestätigt;
  `dbimport_run` – PASS, `up_to_date`, 0 Änderungen;
  `cgen_run` – PASS, 44 Dateien erzeugt.
- `./gradlew check --no-daemon` nach Clean und cgen – PASS.
- Browser-Smoke über die Playwright-Oberfläche des In-App-Browsers gegen den
  lokal gestarteten Boot-Server – PASS:
  - Startseite mit Mabillon und Golden-Path-Verweisen,
  - Dossier `AGI-D-2026-000007` mit Bodenrain, 1 Geschäft und 9 Unterlagen,
  - Geschäft `AGI-G-2026-000421` mit Dossierverknüpfung und 9 Unterlagen.
- Admin-Seiten und Admin-Schreibflows wurden zusätzlich über MockMvc mit
  HTTP-Basic, Rollenprüfung und CSRF getestet.

## Acceptance criteria

| Criterion | Result |
|---|---|
| Sachbearbeiter kann Adminänderung nicht durchführen | PASS; HTTP 403 |
| Admin kann Katalogwert erzeugen und deaktivieren | PASS |
| Deaktivierter/verwendeter Katalogwert bleibt referenzierbar | PASS |
| Genau ein Initialstatus je Geschäftsart | PASS |
| Registraturplan-Zyklus wird verhindert | PASS |
| Inaktive Position ist nicht für neue Dossiers auswählbar | PASS |
| Historisches Dossier mit alter Position bleibt lesbar | PASS |
| Admin-JTE-Seiten rendern gegen PostgreSQL | PASS |
| CSRF-Schutz auf schreibenden Admin-Requests | PASS |
| Vollständiger Check nach Clean und MCP-cgen | PASS |
| Browser-Smoke | PASS |

## Known limitations / gate decision

- Dev-/Test-Identitäten sind bewusst einfache In-Memory-Identitäten. Eine
  produktive OIDC-Anbindung und die Zuordnung fachlicher Benutzer zu externen
  Authorities gehören in die spätere Security-Integration.
- Die Admin-UI zeigt Stammdaten und Registraturplan bereits als Baum; die
  vollständigen Editierformulare für alle Stammdatenfelder werden bei Bedarf
  in den jeweiligen vertikalen Fach-Use-Cases erweitert. Die geprüften
  Schreibregeln liegen bereits im Service-Layer.
- Der Build erwartet weiterhin die nach Modelländerungen über den
  standardisierten Cayenne-MCP erzeugten Quellen unter
  `build/generated/sources/cayenne/`.

Phase 2 ist vollständig grün und wird als `SUCCESS` abgeschlossen. Aufgrund
der ausdrücklichen Benutzerfreigabe zum autonomen Fortfahren beginnt jetzt
Phase 3 mit Dossier-/Geschäft-Kern, Statusvalidierung und Journal.
