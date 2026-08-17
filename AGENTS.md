# AGENTS.md – Verbindliche Anweisung für den Mabillon Coding Agent

Diese Datei ist vor jeder Änderung zu lesen. Die vollständige fachliche und technische Spezifikation steht in `MABILLON_IMPLEMENTATION_SPEC.md`.

## 0.1 Projekt-Skills

Projektlokale Skills liegen unter `.agents/skills` und sind für Codex und OpenCode bestimmt. Nutze sie passend zum Auftrag; bei Implementierungsarbeit ist `mabillon-phase-workflow` immer relevant.

Verfügbare Skills:

- `mabillon-phase-workflow`: Phasengates, Scope und Reporting.
- `mabillon-domain-model`: GEVER-Domäneninvarianten und Use-Case-Zuordnung.
- `mabillon-interlis-ili2pg`: INTERLIS, ili2pg 5.5.2, Schemaimport, XTF/TID/BID/Basket.
- `mabillon-cayenne-mcp`: Cayenne 5.0-M2 DB-first, Modeler MCP und cgen.
- `mabillon-spring-jte-htmx`: Spring MVC/JTE/HTMX HTML-first-Konventionen.
- `mabillon-ui-design`: normative ili2grails-Designsprache.
- `mabillon-testing`: Testpyramide, PostgreSQL/Testcontainers, Golden Path.
- `mabillon-archive-sip`: Aussonderung, Archivablieferung, SIP und Validierung.

Bei Widerspruch gilt: diese `AGENTS.md` vor `MABILLON_IMPLEMENTATION_SPEC.md` vor Skill.

## 1. Primärer Auftrag

Implementiere **Mabillon – Einfache und transparente Geschäftsverwaltung** phasenweise gemäss Spezifikation. Arbeite immer nur an der aktuell vom Benutzer freigegebenen Phase.

## 1.1 Produkt- und Code-Naming

Verbindlich:

```text
Produkt: Mabillon
Claim: Einfache und transparente Geschäftsverwaltung
Gradle group: guru.interlis
Java base package: guru.interlis.mabillon
Artifact/Repository: mabillon
DB schemas: mabillon, mabillon_app
```

Das bestehende INTERLIS-Modell `SO_AGI_GEVER_20260707` wird nicht allein aus Branding-Gründen umbenannt. Modell-/URI-/XTF-Naming nur nach expliziter Freigabe ändern.

## 2. Harte Phasenregel

**Du darfst niemals selbstständig die nächste Phase beginnen.**

Am Ende jeder Phase:

1. alle Acceptance Criteria prüfen,
2. alle Unit-/Integration-/MVC-/E2E-Tests der Phase ausführen,
3. vollständigen Build ausführen,
4. INTERLIS/DB/Cayenne-Konsistenz prüfen, falls betroffen,
5. `docs/phases/PHASE_N_REPORT.md` erstellen,
6. Status `SUCCESS` nur bei vollständig grünem Gate setzen,
7. dem Benutzer Bericht geben,
8. STOPP.

Erst eine neue explizite Benutzeranweisung erlaubt die nächste Phase.

## 3. Source of Truth

Fachliches persistentes Modell:

```text
model/SO_AGI_GEVER_20260707.ili
```

Pipeline:

```text
INTERLIS
→ ili2c 5.6.8
→ ili2pg 5.5.2 --schemaimport
→ PostgreSQL schema mabillon
→ Cayenne DB Import
→ Cayenne DataMap
→ cgen
→ Java
```

Keine fachliche DB-Spalte direkt per Flyway erfinden.

Verbindliche lokale INTERLIS-Toolchain:

```text
ili2pg 5.5.2:
/Users/stefan/apps/ili2pg-5.5.2/ili2pg-5.5.2.jar

ili2c 5.6.8:
/Users/stefan/apps/ili2c-5.6.8/ili2c.jar

ilivalidator 1.15.0:
/Users/stefan/apps/ilivalidator-1.15.0/ilivalidator-1.15.0.jar
```

Overrides: `ILI2PG_JAR`, `ILI2C_JAR`, `ILIVALIDATOR_JAR`.

Jede `.ili`-Änderung wird vor Schema-/Cayenne-Arbeiten mit ili2c validiert. Jeder positive XTF-Input wird vor Import und jeder erzeugte XTF-Output nach Export mit ilivalidator validiert. Bei einem Validierungsfehler wird nicht importiert bzw. der Export nicht als erfolgreich ausgeliefert.

Schemaimport verwendet sicher `--createTidCol --createBasketCol`. Jeder reguläre XTF-Import verwendet sicher `--importTid --importBid`. Der korrekte Optionsname ist `--createBasketCol`, nicht `--createBaskeCol`.

Rein technische Tabellen dürfen in `mabillon_app` liegen, wenn die Spezifikation sie erlaubt.

## 4. Cayenne MCP

Cayenne 5.0-M2 Modeler MCP ist Teil des vorgesehenen lokalen Agent-Workflows.

Bei Mappingänderungen:

1. DB aus aktuellem `.ili` frisch erzeugen.
2. MCP: Projekt öffnen.
3. MCP: DB Import.
4. Mapping-Diff prüfen.
5. MCP: cgen.
6. Generated-Diff prüfen.
7. kompilieren/testen.

Ein unerwarteter Diff ist ein Diagnosegrund, kein Anlass zum blinden Patchen.

## 5. Generated Code

Generierte Basisklassen niemals manuell ändern.

Wenn generierter Code falsch ist, Ursache in dieser Reihenfolge prüfen:

1. INTERLIS,
2. ili2pg Mapping/Optionen,
3. PostgreSQL-Schema/FKs,
4. Cayenne DB Import,
5. cgen-Konfiguration.

## 6. Technologie

Verbindlich:

```text
Java 25
Spring Boot 4.1.0
Spring MVC
JTE
HTMX 2.x (initial 2.0.10)
Vanilla CSS / Mabillon-Designsprache
PostgreSQL
Apache Cayenne 5.0-M2
Gradle Groovy DSL
JUnit Jupiter
AssertJ
Testcontainers PostgreSQL
```

Nicht ohne Freigabe ersetzen.

## 7. Architektur

Feature Packages, modularer Monolith.

Controller enthalten keine Fachlogik.

Controller erhalten/liefern Form-/View-Modelle.

Cayenne-Objekte nicht an Templates geben.

Kein `ObjectContext` in HTTP Session.

Schreibende Use Cases verwenden `CayenneUnitOfWork` und committen Fachänderung + Journal atomar.

## 8. Tests

Keine Phase ohne ausreichende Tests.

Verboten:

- failing Test löschen,
- `@Disabled` als Problemlösung,
- Assertions abschwächen, damit Build grün wird,
- H2 statt PostgreSQL,
- Integrationstest durch Mock ersetzen, wenn Mapping/SQL geprüft werden muss.

Für jede Business Rule:

- mindestens positiver Test,
- mindestens relevanter negativer Test.

Für jeden schreibenden Use Case:

- PostgreSQL+Cayenne Integrationstest.

Für kritische UI-Flows:

- MVC-Test,
- ab vorgesehener Phase Playwright E2E.

## 9. Nomenklatur-Golden-Path

Referenzfall:

```text
Gemeinde Musterwil
"Im alten Boden" → "Bodenrain"
```

Dieser Fall muss als durchgängiges Fixture erhalten bleiben.

## 10. Dossier/Geschäft/Unterlage-Regel

```text
Dossier = Akte
Geschäft = bearbeiteter Vorgang
Unterlage = Aktenstück
```

- Geschäft gehört zu genau einem Dossier.
- Unterlage gehört zu genau einem Dossier.
- Unterlage kann optional ein Geschäft als Geschäftskontext haben.
- Wenn Geschäft gesetzt ist, muss es zum selben Dossier gehören.

Diese Regel darf weder im UI noch im Import umgangen werden.

## 11. Keine Scope-Erweiterung

Nicht eigenmächtig implementieren:

- BPMN,
- Dokumentversionierung,
- React/Vue/Angular,
- Elasticsearch,
- Mailserverintegration,
- OCR,
- KI,
- komplexe ACL,
- elektronische Signaturen,
- Microservices.

Wenn eine solche Funktion nötig erscheint: STOPP und begründe sie dem Benutzer.

## 12. HTMX

HTMX ist progressive enhancement.

Businesslogik ist identisch für HTMX und normale Requests.

Normale HTTP-Fallbacks bevorzugen.

Keine unnötigen JS-Komponenten bauen.

## 12.1 Designsprache

Die normative UI-Referenz ist `edigonzales/ili2grails` auf Commit `3e133a976a0ed1c704f38e81a6493501e0568ec4`, insbesondere `ili-modern.css` und die fünf Mockups unter `mockups/`.

- Struktur, Dichte, Neutralpalette, kleine Radien, subtile Schatten, Form-/Tabellen-/Filtermuster übernehmen.
- Bootstrap ist **keine** Pflichtabhängigkeit; bevorzugt Vanilla CSS.
- Keine Utility-Class-Suppe.
- Keine externen Font-/CSS-CDNs.
- Bei UI-Arbeit Skill `mabillon-ui-design` lesen.

## 13. Sicherheit

- Permission Checks auch im Service Layer.
- Keine Pfade aus Originaldateinamen.
- Keine DB-/OID-Information als Berechtigungsersatz.
- Keine Passwörter/Tokens in Logs.
- Uploads serverseitig validieren.

## 14. Archiv/SIP

SIP ist kein „ZIP Download“.

Vor SIP-Implementierung:

- gültiges Zielprofil verifizieren,
- XSD/Validierungsanforderungen festlegen,
- Testfixture definieren.

Ungültige oder unvollständige Dossiers dürfen keine ablieferungsbereite SIP-Ausgabe erzeugen.

## 15. Änderungsdisziplin

Vor Änderung:

- relevanten Use Case lesen,
- betroffene Klassen identifizieren,
- betroffene Tests benennen.

Nach Änderung:

- gezielte Tests,
- vollständiger Phase-Build,
- Diff reviewen.

Keine massenhaften „Cleanup“-Refactorings ausserhalb des Phase-Scopes.

## 16. Wenn etwas unklar ist

Falls die Spezifikation zwei plausible fachliche Interpretationen zulässt und die Wahl Datenmodell/API dauerhaft beeinflusst:

- nicht raten,
- keine spätere Phase anfangen,
- die Unsicherheit im Phasenbericht markieren und Benutzerentscheidung einholen.

Bei kleinen implementierungsinternen Details darf die einfachste robuste Variante gewählt und dokumentiert werden.
