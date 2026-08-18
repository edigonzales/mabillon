# ADR 0004: Serverseitig gerendertes HTML mit Progressive Enhancement

## Status

Accepted

## Kontext

Mabillon ist eine fachliche Verwaltungsanwendung. Transparente HTTP-Flows, Zugänglichkeit und zuverlässiges Fallback-Verhalten sind wichtiger als ein grosser clientseitiger Runtime-State.

## Entscheidung

Die Weboberfläche verwendet Spring MVC, JTE und Vanilla CSS. HTMX ergänzt ausgewählte Interaktionen; normale HTTP-Requests bleiben der Fallback und benutzen denselben Application-Service-Pfad.

Controller verarbeiten HTTP-Eingaben, rufen Application-/Query-Services auf und bauen Form-/View-Modelle. Fachlogik gehört nicht in Controller. Templates erhalten keine Cayenne-Objekte.

React, Vue, Angular, Bootstrap oder Tailwind sind keine Baseline-Abhängigkeiten. Externe Font-/CSS-CDNs werden nicht vorausgesetzt.

## Konsequenzen

- Fachmutationen besitzen einen verständlichen normalen HTTP-Pfad.
- HTMX-Fragmente verwenden dieselben Komponenten und Fachservices wie Full Pages.
- MVC-Tests decken normale Requests, Validierung, Berechtigung und relevante HTMX-Fragmente ab.
