# Observability

## Actuator

Mabillon verwendet Spring Boot Actuator für Health, Info und Metrics.

- `/actuator/health` ist für Healthchecks vorgesehen.
- Health Probes sind aktiviert.
- Health-Details werden nur bei entsprechender Autorisierung gezeigt.
- `/actuator/info` und `/actuator/metrics` sind über die Anwendungssicherheit geschützt.

Eigene Health-Prüfungen berücksichtigen neben der Anwendung auch zentrale Infrastruktur wie Cayenne/DB und Storage.

## Logging

- Root-Level über `MABILLON_LOG_LEVEL` konfigurierbar.
- Apache Cayenne ist im Produktivlog standardmässig auf `WARN` begrenzt.
- Passwörter, Tokens und andere Secrets dürfen nicht geloggt werden.
- Unerwartete technische Fehler sollen als echte Serverfehler sichtbar bleiben und nicht in generische Fachfehler umgewandelt werden.

## Build-Information

Actuator Info exponiert Anwendungsname, Build-Version und konfiguriertes Environment. Die konkrete Betriebsplattform entscheidet über Metrik-Scraping, Logaggregation und Alerting.

## Mindestmonitoring

Für einen produktiven Betrieb sollten mindestens überwacht werden:

- Anwendung/Health,
- PostgreSQL-Verfügbarkeit und Poolauslastung,
- freier Dokument-/SIP-Speicher,
- HTTP-Fehlerrate und Antwortzeiten,
- Backup-/Restore-Erfolg,
- wiederkehrende Datenqualitäts-/Storagefehler, sofern betrieblich aufbereitet.
