# Produktüberblick

## Zweck

Mabillon ist eine einfache, transparente Geschäftsverwaltung (GEVER) für Fachverwaltungen. Die Anwendung unterstützt den Weg von der Eröffnung eines Geschäfts und seiner Akte über Aufgaben, Beteiligte und Unterlagen bis zu Abschluss, Datenqualitätsprüfung und Archivablieferung.

Mabillon ist bewusst kein generisches BPM-System und kein möglichst umfassendes Enterprise-GEVER. Fachlichkeit soll explizit, nachvollziehbar und mit wenig technischer Magie umgesetzt werden.

## Zentrale Eigenschaften

- **Dossier als Akte:** klassifiziert im Registraturplan und Container für Geschäfte und Unterlagen.
- **Geschäft als Vorgang:** Geschäftsart, Prozessstatus, Verantwortlichkeit, Aufgaben und Resultat.
- **Unterlage als Aktenstück:** Datei plus fachliche Metadaten, zwingend einem Dossier und optional einem Geschäft zugeordnet.
- **Nachvollziehbarkeit:** relevante Änderungen werden atomar mit einem Journalereignis protokolliert.
- **Datenqualität:** explizite Regeln erkennen fachliche und technische Inkonsistenzen.
- **Modellgetriebene Persistenz:** INTERLIS ist Source of Truth für persistente Fachdaten.
- **Archivfähigkeit:** geschlossene, qualitätsgeprüfte Dossiers können in einer Archivablieferung zusammengefasst und als validiertes SIP erzeugt werden.
- **HTML-first:** serverseitig gerenderte Oberfläche mit HTMX als progressiver Verbesserung.

## Typische Nutzerrollen

- **Sachbearbeitung** bearbeitet Dossiers, Geschäfte, Unterlagen und Aufgaben.
- **GEVER-Verantwortliche** besitzen zusätzliche Rechte für Abschluss und Datenqualität.
- **Archivverantwortliche** führen Aussonderung und Archivablieferungen durch.
- **Administratoren** verwalten zusätzlich Kataloge, Stammdaten, Registraturplan und technische Austauschfunktionen.

## Referenzfall

Der stabile fachliche Testfall ist eine Nomenklaturmutation:

> Gemeinde Musterwil beantragt die Umbenennung des Flurnamens „Im alten Boden“ zu „Bodenrain“.

Er verbindet Dossier, Geschäft, Antragstellerin, Unterlagen, Aufgaben, Entscheid, Abschluss und Archivierung zu einem durchgängigen Beispiel.

## Weiterführend

- [Use Cases](use-cases.md)
- [Produktgrenzen](scope.md)
- [Fachliche Begriffe](../domain/concepts.md)
