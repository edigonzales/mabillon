# PHASE_10_REPORT

- Status: `SUCCESS`
- Phase: 10 – Produktionshärtung
- Date: 2026-08-16
- Scope: Health/Metrics, Logging, Security Headers, CSRF mit HTMX,
  Uploadlimits, Cayenne-Pooling, Backup/Restore, Storage-Backup,
  Fehlerseiten, Observability, Performance-Baselines, Container und SBOM

## Cayenne Modeler MCP session check

Der konfigurierte offizielle Server
`/Users/stefan/apps/CayenneModeler.app/Contents/Resources/mcp/CayenneMCPServer.jar`
funktioniert in der neu geladenen Session. Der JSON-Lines-MCP-Handshake
lieferte:

- `initialize`: `cayenne-mcp-server 5.0-M2`, Protocol `2024-11-05`
- `open_project`: `launched`, Projekt-Handshake erfolgreich
- `cgen_run`: `generated`, 44 Dateien geschrieben, keine Warnungen

Der cgen-Zielpfad wurde auf `src/generated/java` verlegt. Die Dateien sind
weiterhin ausschließlich MCP-generierte Artefakte; keine generierte
Cayenne-Basisklasse wurde manuell geändert. Dadurch ist
`./gradlew clean check` ohne vorausgesetzten lokalen Build-Output möglich.

## Implemented

- Spring Boot Actuator mit sicherem Health-/Info-/Metrics-Zugriff und eigenem
  Cayenne-/Storage-Health-Indicator.
- Externe, CSP-kompatible HTMX-CSRF-Unterstützung über `mabillon.js`.
- Security Headers: CSP, `X-Frame-Options`, Referrer-Policy und
  Content-Type-Schutz.
- Multipart- und Streaming-Limits mit Bereinigung abgebrochener Uploads.
- Konfigurierbares Cayenne-Connection-Pooling mit validierten Min-/Max-/Wait-
  Werten.
- Fehlerseite ohne Stacktraces oder interne Daten sowie Behandlung unbekannter
  Ressourcen.
- Strukturierte Console-Logs mit konfigurierbaren Leveln; Cayenne bleibt im
  Produktivlog auf `WARN`.
- Backup-/Restore-Dokumentation für PostgreSQL sowie Dokument-/SIP-Storage.
- Dockerfile, Compose-Laufzeitdefinition und Deployment-Hinweise mit
  Nicht-Root-User und Secret-Übergabe über die Umgebung.
- CycloneDX-SBOM über `cyclonedxBom`.
- Reproduzierbarer Cayenne-Generated-Source-Pfad gemäß ADR 0003.
- Performance-Skript und Messreport mit den geforderten Mindestmengen:
  100k Dossiers, 100k Geschäfte, 1M Unterlagen und 500k Aufgaben.

## Performance result

Die Messung lief gegen eine automatisch verworfene PostgreSQL-Kopie. Die
Gesamtmengen waren 100'002 Dossiers, 100'002 Geschäfte, 1'000'010 Unterlagen
und 500'009 Aufgaben. Representative SQL execution times:

| Query | Result |
|---|---:|
| Dossier lookup | 0.037 ms |
| Geschäft mit Dokument-/Aufgabenzählung | 0.102 ms |
| Dokumente eines Dossiers | 0.126 ms |
| Aufgabenübersicht Top 50 | 46.298 ms |
| Globale Unterlagentitelsuche | 110.444 ms |

Die Vollscan-Werte der beiden letzten Abfragen sind bewusst dokumentiert und
nicht vor der Messung durch künstliche Indizes verändert.

## Verification / phase gate

| Gate | Result |
|---|---|
| `./gradlew clean check bootJar cyclonedxBom --no-daemon` | PASS |
| JUnit-/Spring-MVC-/PostgreSQL-Testcontainer-Suite | PASS |
| Phase-10 Health, Header, CSRF und Fehlerseiten-Tests | PASS |
| `scripts/validate-model.sh` | PASS |
| Alle drei positiven XTF-Fixtures mit `scripts/validate-xtf.sh` | PASS |
| `docker compose config` mit gesetzten Secrets | PASS |
| `docker build --tag mabillon:phase10-local .` | PASS |
| CycloneDX `build/reports/bom.json` | PASS |
| Performance-Baseline | PASS |
| Cayenne MCP open/cgen und Generated-Diff | PASS; 44 Dateien, keine Warnungen |

Offen bleibt ausschließlich die Betreiberaufgabe, das fachliche PostgreSQL-
Schema vor dem Containerstart über die freigegebene INTERLIS-/ili2pg-Pipeline
zu provisionieren. `compose.yaml` übernimmt diese absichtliche
Schema-Provisionierung nicht.

Phase 10 ist vollständig grün und wird als `SUCCESS` abgeschlossen.
