# Entwicklungsumgebung

## Voraussetzungen

- Java 25
- Docker für PostgreSQL/Testcontainers
- Git

INTERLIS-Tools müssen nicht separat installiert werden; sie sind Gradle-Abhängigkeiten.

## Erster Check

```bash
./gradlew test
```

Die Integrationstests starten ihre PostgreSQL/PostGIS-Instanzen über Testcontainers und erzeugen/importieren die benötigten INTERLIS-Fixtures reproduzierbar.

Für den Browser-E2E-Test:

```bash
./gradlew playwrightInstall
./gradlew test
```

## Anwendung lokal starten

Lokale Anwendungspasswörter existieren nur unter dem Profil `dev` bzw. `test`.

```bash
export SPRING_PROFILES_ACTIVE=dev
```

Die Anwendung benötigt eine PostgreSQL-Datenbank mit dem aus INTERLIS erzeugten Schema. Die Standard-Cayenne-Verbindung ist `jdbc:postgresql://localhost:55432/mabillon`, Benutzer `mabillon`; URL, Benutzer und Passwort können über `MABILLON_CAYENNE_URL`, `MABILLON_CAYENNE_USERNAME` und `MABILLON_CAYENNE_PASSWORD` gesetzt werden.

Hilfsskripte unter `scripts/` verwenden dieselbe Java-API-/Gradle-Toolchain wie die Anwendung.

## Wichtige Verzeichnisse

```text
model/                         INTERLIS-Modell und XTF-Fixtures
src/main/java/                 Anwendungscode
src/generated/java/            durch Cayenne cgen erzeugte Quellen
src/main/jte/                  JTE-Templates
src/main/resources/            Konfiguration und statische Assets
docs/                          aktuelle Projektdokumentation
scripts/                       reproduzierbare Hilfsabläufe
.agents/skills/                Coding-Agent-Workflows
```

## Vor Änderungen

Lies `AGENTS.md` sowie die relevanten Dokumente in `docs/domain`, `docs/architecture` und `docs/development`.
