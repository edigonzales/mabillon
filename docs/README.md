# Mabillon Dokumentation

Die aktive Dokumentation beschreibt den **aktuellen Zustand** von Mabillon. Die Entstehungs- und Änderungshistorie wird durch Git und Pull Requests nachvollzogen und nicht parallel als Entwicklungsprotokoll gepflegt.

## Produkt

- [`product/overview.md`](product/overview.md) – Ziel, Nutzergruppen und Kernfunktionen
- [`product/use-cases.md`](product/use-cases.md) – fachliche Use Cases UC-001 bis UC-046
- [`product/scope.md`](product/scope.md) – bewusste Produktgrenzen und Nicht-Ziele

## Fachlichkeit

- [`domain/concepts.md`](domain/concepts.md) – Begriffe, Beziehungen und Identitäten
- [`domain/lifecycle-and-rules.md`](domain/lifecycle-and-rules.md) – Status, Abschluss, Nummerierung und Invarianten
- [`domain/documents.md`](domain/documents.md) – Unterlagen, E-Mail und Dokument-Storage
- [`domain/data-quality.md`](domain/data-quality.md) – DQ-001 bis DQ-013
- [`domain/archival-delivery.md`](domain/archival-delivery.md) – Aussonderung, Ablieferung und SIP

## Architektur

- [`architecture/README.md`](architecture/README.md) – Architekturüberblick und ADR-Index
- [`architecture/security.md`](architecture/security.md) – Rollen, Permissions, Identität und Enforcement
- [`architecture/storage.md`](architecture/storage.md) – Dokument-/SIP-Speicher und Konsistenz

## Schnittstellen

- [`interfaces/interlis.md`](interfaces/interlis.md) – INTERLIS-Modell, Import/Export und Identitätserhalt
- [`interfaces/archive/ech-0160-1.3.0/PROFILE.md`](interfaces/archive/ech-0160-1.3.0/PROFILE.md) – unterstütztes Archivprofil

## Entwicklung

- [`development/getting-started.md`](development/getting-started.md)
- [`development/testing.md`](development/testing.md)
- [`development/interlis-model-workflow.md`](development/interlis-model-workflow.md)
- [`development/cayenne.md`](development/cayenne.md)
- [`development/web-ui.md`](development/web-ui.md)

## Betrieb

- [`operations/deployment.md`](operations/deployment.md)
- [`operations/configuration.md`](operations/configuration.md)
- [`operations/security.md`](operations/security.md)
- [`operations/backup-restore.md`](operations/backup-restore.md)
- [`operations/observability.md`](operations/observability.md)
- [`operations/performance.md`](operations/performance.md)

## Dokumentationsprinzip

- Produkt- und Fachdoku beschreibt Verhalten und Regeln.
- ADRs beschreiben wesentliche Architekturentscheidungen und ihre Begründung.
- Entwicklungsdoku beschreibt reproduzierbare Arbeitsabläufe.
- Betriebsdoku beschreibt Konfiguration und Betrieb.
- Konkrete Entwicklungsstände, Branches, CI-Runs und Implementierungsetappen gehören nicht in die aktive Dokumentation.
