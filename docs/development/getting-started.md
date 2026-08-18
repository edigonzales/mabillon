# Entwicklungsumgebung

## Voraussetzungen

- Java 25
- Docker mit Docker Compose v2
- Git

INTERLIS-Tools müssen nicht separat installiert werden; sie sind Gradle-Abhängigkeiten.

## Lokale Entwicklungsumgebung starten

Für die normale lokale Entwicklung genügt:

```bash
source scripts/dev-up.sh
./gradlew bootRun
```

`dev-up.sh` startet **nur** PostgreSQL/PostGIS und lässt `bootRun` bewusst dem Entwickler. Weil das Script mit `source` geladen wird, bleiben das Spring-Profil und die Datenbankvariablen für den anschliessenden Gradle-Aufruf in der aktuellen Shell gesetzt.

Das Script:

- setzt `SPRING_PROFILES_ACTIVE=dev`,
- startet PostgreSQL/PostGIS auf `localhost:55432`,
- übergibt die lokalen DB-Credentials an Docker sowie Cayenne/INTERLIS,
- wartet auf die Datenbank,
- erzeugt bei Bedarf das INTERLIS-Schema,
- importiert die drei Golden-Path-Testdatensätze `01_...`, `02_...` und `03_...`,
- bereitet den lokalen Dokument- und SIP-Speicher unter `build/dev-data/` vor,
- gibt am Ende JDBC-URL, DB-Credentials und die lokalen Mabillon-Logins aus.

Die Standardwerte für die lokale Entwicklung sind:

```text
Spring-Profil: dev
JDBC-URL:      jdbc:postgresql://localhost:55432/mabillon
DB-Benutzer:   mabillon
DB-Passwort:   mabillon

Admin:          admin / admin
Sachbearbeiter: sachbearbeiter / sachbearbeiter
```

Die Werte können vor dem `source`-Aufruf über die entsprechenden Umgebungsvariablen überschrieben werden.

Für einen vollständig frischen lokalen Dev-Stand inklusive neuer Datenbank, neuen Testdaten und leerem lokalen Dokument-/SIP-Speicher:

```bash
source scripts/dev-up.sh --reset
./gradlew bootRun
```

`compose.dev.yaml` ist ausschliesslich für die lokale Entwicklung gedacht. Das reguläre `compose.yaml` bleibt davon unberührt.

## Tests

Ein vollständiger Testlauf ist:

```bash
./gradlew test
```

Die Integrationstests starten ihre eigenen PostgreSQL/PostGIS-Instanzen über Testcontainers und erzeugen/importieren die benötigten INTERLIS-Fixtures reproduzierbar. Sie verwenden nicht die mit `dev-up.sh` gestartete lokale Datenbank.

Beim Kompilieren erzeugt XJC aus den versionierten eCH-0160-XSDs automatisch die Jakarta-XML-Binding-Klassen unter `build/generated/sources/ech0160/java`. Die generierten Quellen sind Build-Artefakte und werden nicht versioniert.

Für den Browser-E2E-Test:

```bash
./gradlew playwrightInstall
./gradlew test
```

## Anwendung manuell konfigurieren

Wer die lokale Infrastruktur nicht über `dev-up.sh` starten will, kann die benötigten Werte weiterhin direkt setzen. Die Anwendung benötigt eine PostgreSQL-Datenbank mit dem aus INTERLIS erzeugten Schema. Die Standard-Cayenne-Verbindung ist `jdbc:postgresql://localhost:55432/mabillon`, Benutzer `mabillon`.

Hilfsskripte unter `scripts/` verwenden dieselbe Java-API-/Gradle-Toolchain wie die Anwendung.

## Wichtige Verzeichnisse

```text
model/                                      INTERLIS-Modell und XTF-Fixtures
src/main/java/                              Anwendungscode
src/generated/java/                         durch Cayenne cgen erzeugte Quellen
build/generated/sources/ech0160/java/       durch XJC erzeugte eCH-0160-Bindings
build/dev-data/                             lokaler Dokument-/SIP-Speicher
src/main/jte/                               JTE-Templates
src/main/resources/                         Konfiguration und statische Assets
docs/                                       aktuelle Projektdokumentation
scripts/                                    reproduzierbare Hilfsabläufe
.agents/skills/                             Coding-Agent-Workflows
```

## Vor Änderungen

Lies `AGENTS.md` sowie die relevanten Dokumente in `docs/domain`, `docs/architecture` und `docs/development`.
