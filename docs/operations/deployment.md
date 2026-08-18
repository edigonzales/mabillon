# Deployment

## Build

Der Container wird aus einem vollständig geprüften `bootJar` gebaut:

```bash
./gradlew clean check bootJar cyclonedxBom --no-daemon
docker build --tag mabillon:local .
```

Das Image enthält die Anwendung, ihre Java-Abhängigkeiten und das versionierte eCH-Archivprofil. Separate INTERLIS-CLI-JARs sind nicht erforderlich.

## Datenbank

`compose.yaml` stellt PostgreSQL und die Anwendung bereit, provisioniert aber bewusst **nicht** das fachliche Schema. Vor dem App-Start muss das PostgreSQL-Schema `mabillon` aus dem freigegebenen INTERLIS-Modell erzeugt bzw. eine bestehende Datenbank korrekt migriert sein.

Ein leerer PostgreSQL-Container ist kein gültiger produktiver Mabillon-Zustand.

## Secrets

Das Image enthält keine Datenbankpasswörter. Secrets werden über Umgebung bzw. Secret Store des Orchestrators bereitgestellt.

## Produktionsprofil

Das Dockerimage setzt `SPRING_PROFILES_ACTIVE=prod`. Unter `prod` gibt es keine lokalen Mabillon-Benutzer; ohne extern konfigurierte Authentifizierung verhält sich die Anwendung fail-closed.

## Persistente Volumes

Mindestens gemeinsam berücksichtigen:

- PostgreSQL-Daten,
- Dokument-Storage (`MABILLON_STORAGE_ROOT`),
- SIP-Storage (`MABILLON_SIP_ROOT`).

## Freigabecheck

Vor produktiver Freigabe:

- `/actuator/health` meldet `UP`,
- nicht öffentliche Actuator-Endpunkte sind nur für Administratoren erreichbar,
- externer Identity Provider ist korrekt integriert,
- lokale Dev/Test-Logins funktionieren unter `prod` nicht,
- Upload-/Proxy-/Storage-Limits sind aufeinander abgestimmt,
- CycloneDX-SBOM wird archiviert,
- Datenbank- und Storage-Backup/Restore sind gemeinsam getestet.

Siehe [Konfiguration](configuration.md), [Security](security.md) und [Backup/Restore](backup-restore.md).
