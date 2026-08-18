# ADR 0005: Mabillon UI-Designsprache

## Status

Accepted

## Kontext

Mabillon benötigt eine ruhige, informationsdichte und konsistente Oberfläche für Fachverwaltungen. Die implementierte Oberfläche selbst ist die aktuelle Referenz; externe Projekte sind keine normative Laufzeit- oder Versionsabhängigkeit.

## Entscheidung

Mabillon verwendet semantische Vanilla-CSS-Tokens und wiederverwendbare JTE-Komponenten mit folgenden Prinzipien:

- ruhige neutrale Flächen und klare 1px-Rahmen,
- sehr subtile Schatten,
- kleine Radien statt stark gerundeter Karten,
- klare typografische Hierarchie und kompakte Informationsdichte,
- App Shell mit Navigation, Breadcrumbs, Page Header und Content-Bereich,
- konsistente Form Sections, Feld-/Summary-Fehler und Form Actions,
- Such-/Filterleisten, aktive Filter, semantische Tabellen, Row Actions und Pagination,
- sichtbare Fokuszustände und verständliche Textlabels,
- eine zurückhaltende, tokenisierte Akzentfarbe,
- keine extern geladenen Fonts oder CSS-Frameworks.

Generische Komponenten dürfen `ili-*`, fachliche Mabillon-Komponenten `mabillon-*` verwenden, sofern das Vokabular konsistent bleibt.

## Konsequenzen

Neue UI-Funktionen sollen vorhandene Tokens und Komponenten wiederverwenden. Grundlegende visuelle Abweichungen benötigen eine bewusste Design-/Architekturentscheidung statt lokaler Einzelfarben oder Ad-hoc-Komponenten.
