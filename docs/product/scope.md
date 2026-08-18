# Produktumfang und bewusste Grenzen

## Bestandteil des aktuellen Produkts

Mabillon unterstützt:

- Registraturplan, Kataloge, Organisationseinheiten und fachliche Benutzer
- Dossiers und Geschäfte inklusive Suche, Bearbeitung und Abschluss
- Beteiligte und Beteiligungen
- Aufgaben und persönliche Arbeitsübersicht
- Unterlagen mit Dateiablage, E-Mail-Registrierung und Geschäftskontext
- Fachsystemreferenzen
- Journal/Audit
- strukturierte globale Suche und Geschäftskontrolle
- Datenqualitätsregeln DQ-001 bis DQ-013
- INTERLIS-Import/-Export für Kataloge, Stammdaten und Geschäftsdaten
- Aussonderung, Archivablieferung, SIP-Erzeugung und -Validierung
- produktionsorientierte Health-, Security-, Backup- und Container-Grundlagen

## Bewusst nicht Bestandteil

Nicht ohne neue fachliche oder architektonische Entscheidung einführen:

- BPMN- oder frei konfigurierbare Workflow-Engine
- frei konfigurierbare Prozessgraphen
- Dokumentversionierung oder kollaborativer Office-Editor
- automatische Exchange-/IMAP-Mailboxintegration
- OCR oder KI-Klassifikation
- Volltextsuche in Binärdokumenten
- elektronische Signaturen als allgemeines Signaturmodul
- komplexe Record-Level-ACL
- Aufbewahrungsfrist-/Archivwürdigkeitsregelengine
- harte Mandantentrennung
- Microservices oder Event Sourcing

## Suchgrenze

Die globale Suche ist PostgreSQL-basiert und arbeitet auf strukturierten fachlichen Feldern und Metadaten. Binärinhalte werden nicht volltextindexiert. Sehr lange narrative INTERLIS-`LangerText`-Felder sind nicht Teil des generischen Freitextprädikats; strukturierte Filter bleiben davon unberührt.

## E-Mail

E-Mails werden als Unterlagen registriert. Mabillon stellt dafür keine Mailboxintegration bereit; EML-Inhalte können als `message/rfc822` abgelegt werden.

## Archivierung

Das mitgelieferte Profil unterstützt eCH-0160 1.3.0. Mabillon behauptet damit keine automatische Kompatibilität mit einem spezifischen Archiv, dessen zusätzliche Profilvorgaben nicht explizit implementiert und validiert sind.
