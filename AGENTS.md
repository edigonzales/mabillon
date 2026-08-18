# AGENTS.md – Arbeitsregeln für Mabillon

Diese Datei enthält die verbindlichen Leitplanken für Coding Agents. Fachliches und technisches Wissen wird nicht hier dupliziert, sondern in der normalen Projektdokumentation gepflegt.

## 1. Vor jeder Änderung

1. Lies `docs/README.md` und die für den Auftrag relevanten Dokumente.
2. Prüfe den bestehenden Code und die zugehörigen Tests, bevor du eine Lösung entwirfst.
3. Identifiziere die betroffenen Fachregeln, Berechtigungen, Persistenz- und Schnittstellenverträge.
4. Wähle die kleinste kohärente Änderung, die das Problem vollständig löst.

Bei Widersprüchen gilt in dieser Reihenfolge:

1. aktuelles fachliches INTERLIS-Modell für persistente Fachdaten,
2. akzeptierte ADRs und aktuelle Projektdokumentation,
3. bestehende explizite Fachregeln und Tests,
4. Agent-Skills.

## 2. Nicht verhandelbare Architekturregeln

- Produkt: **Mabillon – Einfache und transparente Geschäftsverwaltung**.
- Java-Basispaket: `guru.interlis.mabillon`.
- Modularer Monolith, keine Microservices ohne begründete Architekturentscheidung.
- Fachliches persistentes Schema beginnt im INTERLIS-Modell `model/SO_AGI_GEVER_20260707.ili`.
- INTERLIS-Tools werden über die in Gradle aufgelösten Java APIs verwendet; keine zweite lokale JAR-Toolchain einführen.
- PostgreSQL-Schema `mabillon` enthält fachliche Daten; `mabillon_app` ist rein technischen Anwendungstabellen vorbehalten.
- Cayenne wird DB-first eingesetzt. Generierte Basisklassen niemals manuell ändern.
- Controller enthalten keine Fachlogik und geben keine Cayenne-Objekte an JTE weiter.
- Schreibende Use Cases verwenden einen kurzen Cayenne-`ObjectContext`/`CayenneUnitOfWork`; Fachänderung und Journalereignis committen atomar.
- Kein `ObjectContext` in HTTP-Sessions oder globalem mutablem Zustand.
- HTMX ist progressive enhancement; normale HTTP-Flows bleiben fachlich gleichwertig.
- Security wird nicht nur im Controller, sondern im Service Layer über Permissions durchgesetzt.
- Audit-Akteure müssen eindeutig aus der authentifizierten fachlichen Identität bestimmt werden; bei fehlender Zuordnung fail closed.

## 3. Zentrale Fachinvarianten

Die kanonischen Regeln stehen unter `docs/domain/`.

Besonders wichtig:

- Ein Geschäft gehört zu genau einem Dossier.
- Eine Unterlage gehört zu genau einem Dossier.
- Hat eine Unterlage einen Geschäftskontext, muss das Geschäft zum selben Dossier gehören.
- Prozess- und Resultatstatus müssen zur Geschäftsart passen.
- Abschlussregeln dürfen nicht umgangen oder stillschweigend repariert werden.
- Aktenrelevante registrierte Unterlagen werden im normalen Fachprozess nicht physisch gelöscht.
- Journal/Audit ist fachlicher Bestandteil jeder dafür vorgesehenen Änderung.

## 4. INTERLIS und Cayenne

Bei Änderungen am fachlichen Datenmodell gilt:

```text
INTERLIS ändern
-> ili2c validieren
-> frisches Referenzschema via ili2pg Java API erzeugen
-> Schemaänderung prüfen
-> Cayenne DB Import
-> cgen
-> Generated Diff prüfen
-> Integrationstests
```

Details: `docs/interfaces/interlis.md`, `docs/development/interlis-model-workflow.md`, `docs/development/cayenne.md`.

## 5. Tests

Testing gehört zur Implementierung.

- Business Rules: positiver sowie relevante negative/boundary Fälle.
- Schreibende Persistenz-Use-Cases: echtes PostgreSQL + Cayenne.
- Web/Formulare: Spring MVC inkl. Validation und Authorization.
- Kritische User Journeys: Playwright Java.
- INTERLIS: Modell-/XTF-Validierung, Import/Export und semantischer Roundtrip.
- Tests müssen unabhängig von Ausführungsreihenfolge und gemeinsamem mutablem Persistenzzustand sein.

Verboten sind insbesondere: failing Tests löschen, `@Disabled` als Problemlösung, Assertions abschwächen, H2 als Ersatz für PostgreSQL oder Persistenz mocken, wenn Mapping/SQL selbst geprüft werden muss.

Details: `docs/development/testing.md`.

## 6. Generated Code und Fehlerdiagnose

Wenn generierter Cayenne-Code oder Mapping falsch ist, untersuche die Ursache in dieser Reihenfolge:

1. INTERLIS,
2. ili2pg/DB-Schema/FKs,
3. Cayenne DB Import/DataMap,
4. cgen-Konfiguration.

Unerwartete Diffs sind Diagnosegründe, nicht Anlass für Blind-Patches.

## 7. Scope-Disziplin

Nicht ohne ausdrückliche Produkt-/Architekturentscheidung einführen:

- BPMN-/Workflow-Engine,
- Dokumentversionierung,
- SPA-Frameworks,
- Elasticsearch/OpenSearch,
- automatische Mailboxintegration,
- OCR/KI-Klassifikation,
- komplexe Record-Level-ACL,
- elektronische Signaturen,
- Microservices.

Der aktuelle Produktumfang und bewusste Abgrenzungen stehen in `docs/product/scope.md`.

## 8. Änderung abschliessen

Vor Abschluss einer Änderung:

1. gezielte Tests ausführen,
2. relevante vollständige Tests ausführen,
3. INTERLIS/DB/Cayenne-Konsistenz prüfen, falls betroffen,
4. Diff auf unbeabsichtigte Änderungen prüfen,
5. aktuelle Dokumentation anpassen, wenn Verhalten, Architektur, Betrieb oder Schnittstellen geändert wurden.

Die Git-Historie dokumentiert die Entstehung. Aktive Dokumentation beschreibt den aktuellen Zustand.

## 9. Projekt-Skills

Projektlokale Skills liegen unter `.agents/skills`:

- `mabillon-development-workflow`
- `mabillon-domain-model`
- `mabillon-interlis-ili2pg`
- `mabillon-cayenne-mcp`
- `mabillon-spring-jte-htmx`
- `mabillon-ui-design`
- `mabillon-testing`
- `mabillon-archive-sip`

Skills sind kurze Arbeitsanweisungen und verweisen auf die kanonische Dokumentation; sie ersetzen diese nicht.
