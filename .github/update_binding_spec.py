from pathlib import Path

path = Path('MABILLON_IMPLEMENTATION_SPEC.md')
text = path.read_text(encoding='utf-8')


def replace_between(value: str, start: str, end: str, replacement: str) -> str:
    i = value.index(start)
    j = value.index(end, i)
    return value[:i] + replacement.rstrip() + '\n\n' + value[j:]


text = text.replace(
    '**Status:** Entwurf v0.4  \n**Datum:** 2026-08-16  ',
    '**Status:** Entwurf v0.5  \n**Datum:** 2026-08-17  ')
text = text.replace(
    '- ili2pg 5.5.2 für INTERLIS → PostgreSQL und XTF-Import/-Export',
    '- ili2pg 5.5.1 für INTERLIS → PostgreSQL und XTF-Import/-Export')

old_flow = '''## 3.5 Datenbankschema aus INTERLIS

Referenzfluss:

```text
INTERLIS 2.4
    ↓
ili2c compile
    ↓
ili2pg --schemaimport
    ↓
PostgreSQL / Schema mabillon
    ↓
Cayenne DB Import
    ↓
Cayenne DataMap
    ↓
cgen
    ↓
generierte Persistenzklassen
```
'''
new_flow = '''## 3.5 Datenbankschema aus INTERLIS

Referenzfluss:

```text
INTERLIS 2.4
    ↓
ili2c Java API
    ↓
ili2pg Java API: Schemaimport
    ↓
PostgreSQL / Schema mabillon
    ↓
Cayenne DB Import
    ↓
Cayenne DataMap
    ↓
cgen
    ↓
generierte Persistenzklassen
```
'''
if old_flow not in text:
    raise SystemExit('Section 3.5 baseline not found')
text = text.replace(old_flow, new_flow)

section_72 = '''## 7.2 INTERLIS Java-API-Toolchain

Die INTERLIS-Werkzeuge sind normale Gradle-Abhängigkeiten und werden in Mabillon **in-process über ihre Java APIs** verwendet. Lokale Tool-JAR-Installationen und feste Rechnerpfade sind nicht Teil der Laufzeitarchitektur.

Verbindliche Versionen:

```text
ili2pg        5.5.1
ili2db        5.5.1
ili2c         5.6.8
ilivalidator  1.15.0
```

Die Abhängigkeiten werden zentral über Gradle aufgelöst. `settings.gradle` enthält dafür `mavenCentral()` und `https://jars.interlis.ch`. Die gemeinsam verwendeten INTERLIS-Bibliotheken müssen zu einem bewusst geprüften Satz konvergieren; der Build prüft den freigegebenen Stand mit `verifyInterlisDependencies`.

Freigegebener Phase-11-Baseline-Satz:

```text
ch.interlis:ili2pg:5.5.1
ch.interlis:ili2db:5.5.1
ch.interlis:ilivalidator:1.15.0
ch.interlis:ili2c-tool:5.6.8
ch.interlis:ili2c-core:5.6.8
ch.interlis:iox-ili:1.24.4
ch.interlis:iox-api:1.0.4
ch.ehi:ehibasics:1.4.1
```

Zielarchitektur:

```text
Spring Boot
    |
    +-- InterlisExchangeService
    |      |
    |      +-- Ili2pgRunner ----------------> ili2pg Java API
    |      +-- XtfValidator ----------------> ilivalidator Java API
    |
    +-- InterlisModelValidator -------------> ili2c Java API
```

Verbindliche Regeln:

- Der Spring-Boot-Runtimepfad startet keine externen INTERLIS-Prozesse.
- Es gibt keine Runtime-Konfiguration für lokale Tool-JAR-Pfade.
- Toolversionen stammen aus der Gradle-Auflösung, nicht aus separat installierten CLI-Werkzeugen.
- Modell- und Modellrepository-Konfiguration darf über `MABILLON_MODEL` und `MABILLON_MODEL_DIR` überschrieben werden.
- Das Deployment-Containerimage ist durch seine normalen Gradle-Runtime-Abhängigkeiten vollständig; separate INTERLIS-CLI-Installationen sind nicht erforderlich.
- Diagnosen werden über die Java-Adapter in strukturierte Mabillon-Ergebnisse überführt; Konsolenausgabe ist nicht der primäre Ergebnisvertrag.

Die zentralen Scripts bleiben als dünne Convenience-Wrapper bestehen:

```text
scripts/interlis-tools-env.sh
scripts/validate-model.sh
scripts/validate-xtf.sh
scripts/create-schema.sh
scripts/import-xtf.sh
```

Sie rufen `InterlisToolCli` über den bereits von Gradle aufgelösten Runtime-Classpath beziehungsweise den Gradle-Task `interlisTool` auf. Sie definieren **keine zweite Toolchain und keine eigenen Toolversionen**.

### 7.2.1 Modellvalidierung mit ili2c

`JavaApiInterlisModelValidator` kompiliert das Modell direkt über ili2c 5.6.8. Nach einer Modelländerung dürfen Schemaimport, Cayenne DB Import und `cgen` erst nach erfolgreicher Modellvalidierung ausgeführt werden. Ein Compilerfehler ist ein harter Phasenfehler.

### 7.2.2 XTF-Validierung mit ilivalidator

`JavaApiXtfValidator` validiert XTF direkt über ilivalidator 1.15.0.

Regeln:

1. Alle positiven XTF-Fixtures unter `model/testdata/` müssen fehlerfrei sein.
2. Jeder Anwendungs-Import validiert die XTF-Datei **vor** dem ili2pg-Import; bei Validierungsfehlern findet kein DB-Import statt.
3. Jeder von Mabillon erzeugte XTF-Export wird nach dem Export erneut validiert, bevor er als erfolgreich gemeldet oder zum Download angeboten wird.
4. Negativtests dürfen absichtlich ungültige XTFs verwenden; diese Fixtures liegen klar getrennt, z. B. unter `model/testdata/invalid/`, und der erwartete Validierungsfehler ist Teil des Tests.
5. Eine erfolgreiche Validator-Rückgabe allein genügt bei Roundtrip-Tests nicht: zusätzlich werden Objektzahlen, bekannte TIDs/BIDs und fachliche Referenzen geprüft.
'''
text = replace_between(
    text,
    '## 7.2 Feste lokale INTERLIS-Toolchain',
    '## 7.3 ili2pg Schemaimport',
    section_72)

section_73 = '''## 7.3 ili2pg Schemaimport

Der Schemaimport erfolgt über `JavaApiIli2pgRunner.schemaImport(...)` und die ili2db/ili2pg-Java-API. Die verbindliche Semantik entspricht:

```text
function = SCHEMAIMPORT
dbschema = mabillon
createFk = yes
createFkIdx = yes
createUniqueConstraints = true
createNumChecks = true
createTextChecks = true
createDateTimeChecks = true
createMetaInfo = true
createTidCol = true
createBasketCol = true
setupPgExt = true
```

### Mandatory-Constraint-Entscheid

Mabillon setzt mit ili2pg/ili2db **5.5.1 kein `createMandatoryChecks`**.

Die Phase-11-Prüfung hat für das konkrete Mabillon-Modell gezeigt:

- direkt abbildbare `MANDATORY`-Attribute und -Referenzen werden bereits durch normale PostgreSQL-`NOT NULL`-/FK-Strukturen erzwungen,
- die früher problematischen optionalen Referenzen und `{0..1}`-Rollen müssen nullable bleiben,
- zusätzliche Mandatory-CHECK-Constraints sind für Mabillon nicht erforderlich.

`sqlEnableNull` wird ebenfalls nicht verwendet, weil dies die normalen `NOT NULL`-Constraints global unterdrücken würde.

`InterlisSchemaConstraintIntegrationTest` ist der verbindliche Regressionstest. Er prüft positive und negative Mandatory-Fälle, die Nullable-Semantik der früher betroffenen optionalen Referenzen und dass keine zusätzlichen `IS NOT NULL`-CHECK-Constraints erzeugt werden.

Ein nachträglicher `SchemaConstraintRepair` ist nicht Teil der Architektur und darf nicht wieder eingeführt werden, solange kein reproduzierbarer, getesteter Bedarf vorliegt.

Credentials/Host werden über Anwendungskonfiguration beziehungsweise Umgebungsvariablen geliefert und nie geloggt. Scripts und CI verwenden denselben Java-API-/Gradle-Pfad; ili2pg-Optionen werden nicht als zweite, unabhängige CLI-Konfiguration dupliziert.
'''
text = replace_between(
    text,
    '## 7.3 ili2pg Schemaimport',
    '## 7.4 Baskets, TIDs und BIDs',
    section_73)

section_74 = '''## 7.4 Baskets, TIDs und BIDs

Die Topic-/Basket-Trennung ist Teil des Datenmodells und muss beim Datenaustausch erhalten bleiben.

Für Test-/Entwicklungsdaten werden mindestens drei definierte Baskets erzeugt/importiert:

- Kataloge,
- Stammdaten,
- Geschäftsdaten.

Die öffentlichen Mabillon-Importpfade übergeben an `ImportXtfRequest` verbindlich:

```text
importTid = true
importBid = true
```

`JavaApiIli2pgRunner` überträgt diese Werte direkt auf die ili2db-Konfiguration. Ein Importmodus ohne Übernahme dieser IDs muss als eigener, fachlich begründeter Use Case spezifiziert und freigegeben werden.

Beim Export ist `exportTid = true`; Topics und optional ausgewählte Basket-IDs werden explizit an die Java-API übergeben.

Pflichttests:

1. bekannte XTF-OIDs erscheinen nach Import als erwartete `t_ili_tid`-Werte,
2. bekannte Basket-IDs bleiben über `t_basket` nachvollziehbar,
3. Referenzen zwischen den drei Topics funktionieren nach Import,
4. Reimport/Export-Tests dürfen nicht unbemerkt neue Identitäten erzeugen.
'''
text = replace_between(
    text,
    '## 7.4 Baskets, TIDs und BIDs',
    '## 7.5 DB-Änderungen',
    section_74)

text = text.replace(
    '.ili → ili2pg --schemaimport → leere DB',
    '.ili → ili2c Java API → ili2pg Java API (Schemaimport) → leere DB')
text = text.replace(
    'Nach erfolgreichem `ili2pg --schemaimport` gegen eine frische lokale Referenz-DB:',
    'Nach erfolgreichem Schemaimport über die ili2pg-Java-API gegen eine frische lokale Referenz-DB:')

section_1414 = '''# 14.14 INTERLIS Import/Export

## Java-API-Adapter

Die fachlichen Adapterinterfaces bleiben die stabile Anwendungsgrenze:

```java
public interface InterlisModelValidator {
    ValidationResult validate(Path iliModel);
}

public interface XtfValidator {
    ValidationResult validate(Path xtf);
}

public interface Ili2pgRunner {
    Ili2pgResult schemaImport(SchemaImportRequest request);
    Ili2pgResult importXtf(ImportXtfRequest request);
    Ili2pgResult exportXtf(ExportXtfRequest request);
    Ili2pgResult validate(ValidateRequest request);
}
```

Produktive Implementierungen:

```text
JavaApiIli2pgRunner
JavaApiInterlisModelValidator
JavaApiXtfValidator
```

Die Adapter rufen ili2pg/ili2db, ili2c und ilivalidator direkt im Anwendungsprozess auf. Externe Prozessstarts, separat installierte Tool-JARs und lokale JAR-Pfad-Defaults sind nicht Bestandteil der Produktarchitektur.

`InterlisToolDefaults` enthält ausschliesslich gemeinsame fachlich-technische Defaults wie Modellname, Modellpfad, Modellrepositories und die erwarteten Toolversionen. Tool-Binaries oder lokale Installationspfade gehören nicht hinein.

Die öffentlichen Import-Use-Cases verwenden immer `importTid=true` und `importBid=true`:

```java
public record ImportXtfRequest(
    Path xtf,
    ImportScope scope,
    boolean importTid,
    boolean importBid
) {}
```

Verbindlicher Importalgorithmus:

```text
XtfValidator.validate(xtf)
  → invalid: STOP, keine DB-Änderung
  → valid: Topic prüfen
  → Ili2pgRunner.importXtf(... importTid=true, importBid=true)
  → Ili2pgRunner.validate(...)
  → fachliche Post-Import-Checks
```

Verbindlicher Exportalgorithmus:

```text
Ili2pgRunner.exportXtf(... exportTid=true)
  → XtfValidator.validate(exportedXtf)
  → invalid: Export als FAILED behandeln und Datei nicht ausliefern
  → valid: fachliche Roundtrip-/Count-Prüfungen, danach Erfolg
```

Passwörter werden nie geloggt. Fehler und Diagnosen werden über `ValidationResult`, `Ili2pgResult` und die Exchange-Ergebnisse transportiert.

## Application Service

```java
@Service
public final class InterlisExchangeService {
    ExchangeResult importCatalog(Path xtf);
    ExchangeResult importMasterData(Path xtf);
    ExchangeResult importBusinessData(Path xtf);
    Path exportCatalog(ExportSelection selection);
    Path exportMasterData(ExportSelection selection);
    Path exportBusinessData(ExportSelection selection);
    ValidationReport validateTopic(TopicSelection selection);
}
```

Importreihenfolge:

```text
Kataloge → Stammdaten → Geschäftsdaten
```
'''
text = replace_between(
    text,
    '# 14.14 INTERLIS Import/Export',
    '# 14.15 Archivierung und SIP',
    section_1414)

phase0 = text.index('## Phase 0 – Fachmodell und technische Machbarkeit einfrieren')
gate_start = text.index('### Tests/Gate', phase0)
gate_end = text.index('### STOP', gate_start)
phase0_gate = '''### Tests/Gate

- `settings.gradle` löst die INTERLIS-Bibliotheken zentral über Maven Central und `https://jars.interlis.ch` auf,
- `verifyInterlisDependencies` bestätigt den freigegebenen Satz mit ili2pg/ili2db 5.5.1, ili2c 5.6.8 und ilivalidator 1.15.0,
- `scripts/validate-model.sh` validiert das Modell über die ili2c-Java-API erfolgreich,
- alle positiven XTF-Testdaten sind über `scripts/validate-xtf.sh` / ilivalidator-Java-API grün,
- ein absichtlich ungültiges XTF wird vor dem DB-Import abgewiesen,
- Schemaimport über die ili2pg-Java-API gegen eine frische PostgreSQL-Testinstanz ist grün, inklusive TID- und Basket-Spalten,
- der Schemaimport verwendet **kein `createMandatoryChecks`**; `InterlisSchemaConstraintIntegrationTest` beweist die erforderlichen Mandatory- und Nullable-Semantiken,
- XTF-Testdatenimport über die Java API ist grün mit `importTid=true` und `importBid=true`,
- erwartete TIDs und BIDs bleiben nach Import erhalten,
- alle erwarteten FK/Checks vorhanden,
- Cayenne DB Import erfolgreich,
- cgen erfolgreich,
- Java compile erfolgreich,
- MCP smoke test dokumentiert,
- keine unerklärten Mapping-Diffs,
- `ili2grails`-Designreferenz auf Commit `3e133a976a0ed1c704f38e81a6493501e0568ec4` geprüft und Design-ADR erstellt,
- Agent-Skills unter `.agents/skills` syntaktisch geprüft und von Codex/OpenCode auffindbar,
- Runtime, Tests und Scripts benötigen keine separat installierten INTERLIS-Tool-JARs.

'''
text = text[:gate_start] + phase0_gate + text[gate_end:]

text = text.replace(
    '- `--importTid` erhält die vorgegebenen Transfer-IDs,',
    '- `importTid=true` erhält die vorgegebenen Transfer-IDs,')
text = text.replace(
    '- `--importBid` erhält die vorgegebenen Basket-IDs,',
    '- `importBid=true` erhält die vorgegebenen Basket-IDs,')

section_24 = '''# 24. Technische Verifikation vor Phase 1

Der Agent muss in Phase 0 explizit verifizieren und dokumentieren:

1. Spring Boot 4.1.0 startet mit Java 25.
2. JTE Spring Boot 4 Starter kompiliert und rendert ein Template.
3. HTMX 2.0.10 wird lokal versioniert/ausgeliefert; kein CDN-Zwang.
4. Cayenne 5.0-M2 Runtime startet.
5. Cayenne `ObjectContext` kann lesen, schreiben, committen und rollbacken.
6. Cayenne Modeler MCP ist vom lokalen Agent-Code erreichbar.
7. MCP kann das Projekt öffnen.
8. MCP kann DB Import ausführen.
9. MCP kann cgen ausführen.
10. ili2pg-generierte FKs werden durch Cayenne als erwartete Relationships erkannt.
11. Association `Unterlage_Geschaeft` erzeugt ein brauchbares Mapping.
12. Topic-/Basket-Metadaten behindern Cayenne nicht.
13. `t_id`, `t_ili_tid`, `t_basket` werden korrekt verstanden und nicht als Fachnummern missbraucht.
14. Gradle löst ili2pg 5.5.1, ili2db 5.5.1, ili2c 5.6.8 und ilivalidator 1.15.0 aus der zentralen Repository-/Dependency-Konfiguration auf; `verifyInterlisDependencies` ist grün.
15. Modellkompilation, XTF-Validierung und ili2pg-Operationen funktionieren über die produktiven Java-API-Adapter ohne separat installierte Tool-JARs.
16. Ein absichtlich ungültiges XTF wird von ilivalidator erkannt und gelangt nicht in den ili2pg-Import.
17. Ein gültiger Mabillon-XTF-Export besteht die ilivalidator-Prüfung.
18. Schemaimport erzeugt TID- und Basket-Spalten, verwendet kein `createMandatoryChecks` und erfüllt die in `InterlisSchemaConstraintIntegrationTest` geprüften Mandatory-/Nullable-Regeln.
19. Testdatenimport verwendet `importTid=true` und `importBid=true` und erhält die erwarteten IDs.
20. Die `ili2grails`-Designreferenz ist auf den in Abschnitt 2.2 genannten Commit gepinnt und dokumentiert.
21. Die projektspezifischen Agent-Skills liegen unter `.agents/skills/<name>/SKILL.md` und werden in der lokalen Codex- und OpenCode-Konfiguration entdeckt.

Wenn einer dieser Punkte scheitert, endet Phase 0 mit Status FAILED. Der Agent darf keine Workarounds in Phase 1 verstecken.
'''
text = replace_between(
    text,
    '# 24. Technische Verifikation vor Phase 1',
    '# 25. Leitprinzip für alle weiteren Entscheidungen',
    section_24)

forbidden = [
    'ili2pg 5.5.2',
    '/Users/stefan/apps/ili2pg',
    '/Users/stefan/apps/ili2c',
    '/Users/stefan/apps/ilivalidator',
    'ILI2PG_JAR',
    'ILI2C_JAR',
    'ILIVALIDATOR_JAR',
    'ProcessBuilderIli2pgRunner',
    'ProcessBuilderInterlisModelValidator',
    'ProcessBuilderXtfValidator',
]
leftovers = [item for item in forbidden if item in text]
if leftovers:
    raise SystemExit(
        'Stale binding-spec requirements remain: ' + ', '.join(leftovers))

path.write_text(text, encoding='utf-8')
