# Konfiguration

Die Tabelle dokumentiert die wichtigsten betrieblichen Einstellungen. Spring-Properties können über die üblichen Spring-Mechanismen gesetzt werden; die genannten Umgebungsvariablen sind die vorgesehenen Container-/Betriebsnamen.

| Bereich | Umgebungsvariable | Default | Zweck |
|---|---|---|---|
| Profil | `SPRING_PROFILES_ACTIVE` | im Image `prod` | aktiviert Laufzeitprofil |
| DB | `MABILLON_CAYENNE_URL` | `jdbc:postgresql://localhost:55432/mabillon` | Cayenne JDBC URL |
| DB | `MABILLON_CAYENNE_USERNAME` | `mabillon` | DB-Benutzer |
| DB | `MABILLON_CAYENNE_PASSWORD` | leer | DB-Passwort |
| Pool | `MABILLON_DB_POOL_MIN` | `2` | minimale Connections |
| Pool | `MABILLON_DB_POOL_MAX` | `12` | maximale Connections |
| Pool | `MABILLON_DB_POOL_WAIT_MS` | `5000` | maximale Queue-Wartezeit |
| Upload | `MABILLON_MAX_FILE_SIZE` | `50MB` | Spring Multipart-Dateilimit |
| Upload | `MABILLON_MAX_REQUEST_SIZE` | `55MB` | Spring Request-Limit |
| Upload | `MABILLON_MAX_FILE_SIZE_BYTES` | `52428800` | serverseitiges Storage-Limit |
| Dokumente | `MABILLON_STORAGE_ROOT` | laufzeitabhängig | Root des Dokument-Storages |
| SIP | `MABILLON_SIP_ROOT` | Temp-Verzeichnis bzw. Containerpfad | SIP-Root |
| SIP | `MABILLON_ECH_XSD_ROOT` | `docs/interfaces/archive/ech-0160-1.3.0/xsd` | eCH-XSD-Root |
| Umgebung | `MABILLON_ENVIRONMENT` | `development` | Info/Observability-Label |
| Logging | `MABILLON_LOG_LEVEL` | `INFO` | Root-Loglevel |
| Actor Mapping | `MABILLON_SECURITY_ADMIN_ACTOR_USERNAME` | `anna.mueller` | fachlicher Actor für lokalen Admin-Login |
| Actor Mapping | `MABILLON_SECURITY_SACHBEARBEITER_ACTOR_USERNAME` | `a.keller` | fachlicher Actor für lokalen Sachbearbeiter-Login |

Für INTERLIS-Operationen existieren zusätzlich DB-Parameter `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`; sie konfigurieren die Austausch-/Schemaoperationen und dürfen nicht geloggt werden.

Lokale Benutzername/Passwort-Properties sind nur für `dev` und `test` vorgesehen. In Produktion ist ein externer Identity Provider zu integrieren.

Die tatsächlichen Defaults der Anwendung sind zusätzlich in `src/main/resources/application.properties` und den jeweiligen `@Value`-Konfigurationen sichtbar; diese Datei soll mit Änderungen daran synchron gehalten werden.
