# Web- und UI-Entwicklung

## Stack

Mabillon verwendet Spring MVC, JTE, HTMX 2.x und Vanilla CSS.

## HTML-first

Fachverhalten wird einmal im Application Service implementiert. Controller können für denselben Vorgang je nach Request eine Full Page/Redirect oder ein JTE-Fragment liefern; HTMX ist kein zweiter Fachpfad.

## Controller-Grenzen

Controller:

- parsen und validieren HTTP-Eingaben,
- rufen Application-/Query-Services auf,
- bauen Form-/View-Modelle,
- wählen Full Page oder Fragment.

Sie enthalten keine Fachregeln und verwenden keinen Cayenne-`ObjectContext` direkt. Templates erhalten View Models.

## Formulare

- normale HTML-Formulare und POST/Redirect/GET bleiben der robuste Basisweg,
- CSRF bleibt aktiv,
- Feldfehler und Summary-Fehler erhalten Benutzereingaben,
- Business Rules werden serverseitig validiert,
- stabile fachliche Nummern dürfen URL-Keys sein.

## Fehlersemantik

Der zentrale Web-Fehlervertrag unterscheidet:

- **400** – Validierung, fehlende/ungültige Parameter, Typkonvertierung,
- **403** – Anwendungsebene verweigert Berechtigung,
- **404** – Mabillon-Ressource oder Webressource nicht gefunden,
- **409** – formal gültiger Request verletzt eine Fachregel.

HTMX erhält für strukturierte Fehler ein kompaktes Alert-Fragment; normale Requests eine vollständige Fehlerseite. Unerwartete technische Fehler werden nicht als scheinbarer Fachfehler verschluckt.

## Design

Neue Seiten verwenden die bestehende Mabillon-Designsprache und vorhandene Komponenten/Tokens: kleine Radien, neutrale Flächen, klare Tabellen/Formulare, sichtbarer Fokus, zurückhaltende Akzentfarbe und keine externen CSS-/Font-CDNs.

Siehe [ADR UI-Designsprache](../architecture/0005-ui-design-language.md).
