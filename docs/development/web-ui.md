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

Normative visuelle Referenz ist ili2grails am Commit `3e133a976a0ed1c704f38e81a6493501e0568ec4`. Mabillon bildet dessen `balanced`-Designsprache in eigenem Vanilla CSS ab; Bootstrap gehört nicht zum Runtime-Stack.

Die zentralen Verträge sind:

- Fira Sans Regular/SemiBold aus lokalen Assets,
- Primärfarbe `#4299E1`, aktive Navigation `#ECF5FC`, Text `#3F4B55`, Rahmen `#D3DDE5`, Tabellenkopf `#EDF2F5` und Hover `#E8F1F7`,
- 3px-Radien, subtile Referenzschatten und ein 0.5/0.75/1/1.5-rem-Abstandsraster,
- 64px-Topbar, 272px-Sidebar und höchstens 1440px breiter Inhaltsbereich,
- ein gemeinsamer `mabillon-*`-Namespace für generische und fachliche Komponenten, Custom Properties, technische Zustände und eigene DOM-Hooks,
- sichtbarer Tastaturfokus, Reduced Motion, druckbare Seiten und kein horizontaler Seitenoverflow,
- keine CDN-Fonts, CDN-Icons, externen Stylesheets oder lokal nachgebauten Parallelvarianten.

Die Shell enthält nur reale Funktionen. Die globale Suche sendet `GET /suche?q=…`; Navigation und fachliche Formulare bleiben ohne JavaScript verwendbar. `mabillon.js` ergänzt nur das mobile Shell-Verhalten. HTMX-Fragmente behalten ihre bestehenden fachlichen Grenzen und enthalten keine App Shell.

Siehe [ADR UI-Designsprache](../architecture/0005-ui-design-language.md).
