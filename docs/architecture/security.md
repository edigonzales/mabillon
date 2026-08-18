# Security-Architektur

## Grundsätze

- Fachliche HTTP-Routen sind standardmässig authentifizierungspflichtig.
- `/admin/**` und nicht öffentliche Actuator-Endpunkte sind zusätzlich rollengeschützt.
- Berechtigungen werden im Service Layer erneut geprüft; UI-/Route-Schutz allein genügt nicht.
- Fachlicher Benutzer und technische Authentifizierung sind getrennte Konzepte.
- Kann eine authentifizierte Identität keinem fachlichen Akteur zugeordnet werden, dürfen journalpflichtige Fachaktionen nicht stillschweigend unter einem anderen Benutzer laufen.

## Rollen und Permissions

| Rolle | Permissions |
|---|---|
| `SACHBEARBEITER` | VIEW_MABILLON, EDIT_GESCHAEFT, EDIT_DOSSIER, EDIT_UNTERLAGE, EDIT_AUFGABE, MANAGE_INTERLIS_EXCHANGE |
| `GEVER_VERANTWORTLICHER` | wie Sachbearbeitung plus CLOSE_DOSSIER und RUN_DATA_QUALITY |
| `ARCHIVVERANTWORTLICHER` | VIEW_MABILLON, RUN_DATA_QUALITY, MANAGE_ARCHIVE_DELIVERY |
| `ADMIN` | alle Permissions |

Weitere explizite Permissions sind `MANAGE_CATALOGS`, `MANAGE_MASTERDATA` und `MANAGE_REGISTRATURPLAN`; sie sind durch die Admin-Rolle abgedeckt.

## Authentifizierung

`dev` und `test` stellen einfache lokale Test-/Entwicklungsidentitäten bereit. Alle anderen Profile besitzen keinen lokalen User Store und verhalten sich fail-closed. Produktion erwartet daher eine externe Spring-Security-Integration, beispielsweise OIDC, ohne dass Fachservices an einen konkreten Identity Provider gekoppelt werden.

## CurrentActor

Fachservices greifen über `CurrentActor` auf die aktuelle Identität und Rollen zu. Technische Login-Aliasse dürfen deterministisch auf fachliche Benutzer abgebildet werden; es gibt keinen generischen „Fallback-Benutzer“ für Audit.

## Web-Schutz

Die Anwendung verwendet unter anderem:

- CSRF-Schutz mit Cookie-CSRF-Token für normale Formulare und HTMX,
- Content-Security-Policy,
- `X-Frame-Options: DENY`,
- No-Referrer-Policy,
- Content-Type-Schutz durch Spring Security.

Betriebliche Details: [operations/security.md](../operations/security.md).
