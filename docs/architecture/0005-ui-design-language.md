# ADR 0005: Mabillon UI-Designsprache

## Status

Accepted

## Kontext

Mabillon benötigt eine ruhige, informationsdichte und konsistente Oberfläche für Fachverwaltungen. Normative visuelle Referenz ist das Designsystem von `ili2grails` am gepinnten Commit
`3e133a976a0ed1c704f38e81a6493501e0568ec4`. Damit bleibt die Designentscheidung reproduzierbar, auch wenn sich das Nachbarprojekt weiterentwickelt.

Die Referenz ist eine Design- und keine Laufzeitabhängigkeit. Mabillon übernimmt visuelle Grammatik, App Shell und Interaktionsmuster, nicht Bootstrap, Grails oder ili2grails-spezifische Fachfunktionen.

## Entscheidung

Mabillon implementiert die gepinnte ili2grails-Designsprache mit anwendungseigenem Vanilla CSS und wiederverwendbaren JTE-Komponenten.

Verbindlich ist die `balanced`-Palette:

| Bedeutung | Token | Wert |
|---|---|---|
| Primärfarbe | `--mabillon-color-primary` | `#4299E1` |
| aktive Fläche | `--mabillon-color-active-bg` | `#ECF5FC` |
| Text | `--mabillon-neutral-ink` | `#3F4B55` |
| hervorgehobener Text | `--mabillon-neutral-emphasis` | `#27333D` |
| Sekundärtext | `--mabillon-neutral-muted` | `#5E6D79` |
| Rahmen | `--mabillon-neutral-border` | `#D3DDE5` |
| Oberfläche | `--mabillon-neutral-surface` | `#FFFFFF` |
| Canvas | `--mabillon-neutral-canvas` | `#F5F7F9` |
| Tabellen-/Bereichskopf | `--mabillon-neutral-header` | `#EDF2F5` |
| Hoverfläche | `--mabillon-neutral-hover` | `#E8F1F7` |

Weitere verbindliche Eigenschaften sind:

- lokal gebündelte Fira Sans Regular (400) und SemiBold (600),
- klare 1px-Rahmen, der Referenzschatten `0 1px 3px rgba(39, 51, 61, 0.08)` und durchgehend 3px-Radien,
- das Abstandsraster 0.5/0.75/1/1.5 rem,
- eine 4rem hohe Topbar, eine 17rem breite Desktop-Sidebar und maximal 1440px breiter Inhalt,
- App Shell mit realer globaler Suche, Navigation, Breadcrumbs, Page Header und Content-Bereich,
- konsistente Form Sections, Feld-/Summary-Fehler und Form Actions,
- Such-/Filterleisten, aktive Filter, semantische Tabellen, Row Actions und Pagination,
- sichtbare Fokuszustände und verständliche Textlabels,
- zugängliche Offcanvas-Navigation auf schmalen Viewports und einen sichtbaren No-JS-Navigationsfallback,
- Bootstrap-kompatible semantische Farben für Information, Erfolg, Warnung und Gefahr, umgesetzt als eigene Komponenten,
- keine extern geladenen Fonts, Icons oder CSS-Ressourcen.

Alle anwendungseigenen UI-Verträge verwenden einheitlich den Namespace `mabillon-*`. Das gilt für generische und fachliche Komponenten, Custom Properties, technische Zustände und eigene DOM-Hooks. Das gemeinsame Vokabular umfasst insbesondere Shell, Breadcrumbs, Page Header, Actions, Tiles, Buttons, Form Sections, Filterleisten, Tabellen, Definition Rows, Record Lists, Badges, Notices, Empty States und Pagination. Fremdverträge wie `hx-*`, `htmx-*` und `aria-*` bleiben in ihrem standardisierten Namespace.

Bootstrap bleibt ausserhalb des Mabillon-Runtimes. Alternative ili2grails-Paletten, Dark Mode und ein separater Mabillon-Akzent sind nicht Teil dieser Entscheidung.

## Konsequenzen

Neue UI-Funktionen müssen vorhandene Tokens und Komponenten wiederverwenden. Lokale Einzelfarben, abweichende Radien, CDN-Ressourcen und parallel gepflegte Komponentenvarianten sind zu vermeiden. Grundlegende visuelle Abweichungen oder ein Wechsel der gepinnten ili2grails-Referenz benötigen eine bewusste Design-/Architekturentscheidung.
