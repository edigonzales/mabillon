# Mabillon

**Einfache und transparente Geschäftsverwaltung**

Mabillon ist eine bewusst schlanke GEVER-Anwendung für Fachverwaltungen. Sie verbindet Dossiers, Geschäfte, Aufgaben, Beteiligte und Unterlagen mit nachvollziehbaren Fachregeln, Journal/Audit, INTERLIS-Datenaustausch und einem Ablauf bis zur Archivablieferung.

## Funktionsumfang

- Dossiers und Geschäfte eröffnen, bearbeiten, suchen und abschliessen
- Registraturplan, Kataloge, Organisationseinheiten und Benutzer verwalten
- Beteiligte und Beteiligungen führen
- Aufgaben erstellen, delegieren und erledigen; Dashboard „Meine Arbeit“
- Unterlagen und E-Mails registrieren, sicher speichern und herunterladen
- strukturierte globale Suche und Geschäftskontrolle
- Datenqualitätsprüfungen mit DQ-001 bis DQ-013
- vollständiges Journal fachlich relevanter Änderungen
- INTERLIS-Import/-Export für Katalog-, Stamm- und Geschäftsdaten
- Aussonderung, Archivablieferung und eCH-0160-basiertes SIP

## Architektur in Kürze

```text
Browser
  -> Spring MVC + JTE + HTMX
  -> Application Services
  -> Cayenne Unit of Work
  -> PostgreSQL

INTERLIS 2.4
  -> ili2c / ilivalidator / ili2pg Java APIs
  -> PostgreSQL-Schema mabillon
  -> Cayenne DB-first Mapping
```

Das fachliche persistente Modell beginnt in `model/SO_AGI_GEVER_20260707.ili`. Fachliche Schemaänderungen werden nicht zuerst in PostgreSQL oder Cayenne eingeführt.

## Technologie

Java 25, Spring Boot 4.1, Spring MVC, JTE, HTMX, PostgreSQL, Apache Cayenne 5.0-M2, INTERLIS/ili2db, Gradle, Testcontainers und Playwright Java.

Die tatsächlich verwendeten Bibliotheksversionen sind in `build.gradle` definiert.

## Dokumentation

Der Einstieg in die Projektdokumentation ist [`docs/README.md`](docs/README.md).

- [Produkt und Use Cases](docs/product/overview.md)
- [Fachliches Modell](docs/domain/concepts.md)
- [Architekturentscheidungen](docs/architecture/README.md)
- [INTERLIS-Schnittstelle](docs/interfaces/interlis.md)
- [Entwicklung](docs/development/getting-started.md)
- [Betrieb](docs/operations/deployment.md)

## Entwicklung

Die lokale Entwicklungsumgebung mit PostgreSQL/PostGIS, INTERLIS-Schema und Testdaten wird vorbereitet mit:

```bash
source scripts/dev-up.sh
./gradlew bootRun
```

`dev-up.sh` startet nur die Infrastruktur und setzt die benötigten Umgebungsvariablen; `bootRun` bleibt ein separater manueller Schritt. Für einen frischen lokalen Stand:

```bash
source scripts/dev-up.sh --reset
./gradlew bootRun
```

Ein vollständiger Testlauf ist:

```bash
./gradlew test
```

Für den Browser-E2E-Test muss Chromium einmal installiert werden:

```bash
./gradlew playwrightInstall
./gradlew test
```

Modell- und XTF-Werkzeuge laufen über dieselben Gradle-Abhängigkeiten wie die Anwendung; eine separat installierte INTERLIS-CLI-Toolchain ist nicht erforderlich.

Weitere Hinweise stehen unter [`docs/development/`](docs/development/).

## Lizenz

Siehe [LICENSE](LICENSE).
