# Security im Betrieb

## Profile

- `dev` und `test`: lokale In-Memory-Identitäten für Entwicklung/Tests.
- alle anderen Profile, insbesondere `prod`: keine lokalen Benutzer; Loginauflösung fail-closed.

Ein produktiver Identity Provider wird als Spring-Security-Integration bereitgestellt. Die Fachlogik arbeitet weiterhin über `CurrentActor` und Permissions und bleibt vom konkreten Provider entkoppelt.

## Lokale Credentials

Defaults wie `admin/admin` oder `sachbearbeiter/sachbearbeiter` sind ausschliesslich Entwicklungs-/Testhilfen. Sie dürfen unter `prod` nicht auf fachliche Seiten zugreifen.

## Secrets

- Datenbankpasswörter, Tokens und Identity-Provider-Secrets nicht im Repository oder Image hinterlegen.
- Secrets über den Orchestrator/Secret Store bereitstellen.
- Passwörter und Connection-Argumente nicht in Logs ausgeben.

## HTTP-Schutz

Security-Konfiguration aktiviert CSRF und restriktive Header. Reverse Proxy und TLS-Terminierung müssen diese Schutzwirkung erhalten. Bei Proxybetrieb ist das Forward-Header-Verhalten korrekt zu konfigurieren.

## Actuator

`health` ist für Betriebsprüfungen erreichbar; detailliertere Actuator-Endpunkte sind administrativ geschützt. Externe Exposition sollte am Reverse Proxy zusätzlich auf die tatsächlich benötigten Pfade begrenzt werden.

## Audit

Authentifizierung ist nur die technische Identität. Journalpflichtige Aktionen benötigen zusätzlich eine eindeutige Zuordnung zu einem fachlichen Mabillon-Benutzer. Fehlt sie, muss der Vorgang abbrechen statt einen anderen Akteur zu verwenden.

Architekturdetails: [architecture/security.md](../architecture/security.md).
