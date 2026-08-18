# INTERLIS-Schnittstelle

## Fachliches Modell

Die persistente fachliche Source of Truth ist:

```text
model/SO_AGI_GEVER_20260707.ili
```

Das Modell enthält die Topics:

```text
Kataloge
Stammdaten
Geschaeftsdaten
```

Der Produktname Mabillon ändert Modellname, Modell-URI, Topic-Namen oder XTF-QNames nicht automatisch. Solche Änderungen wären eine explizite Schnittstellenmigration.

## Toolchain

Mabillon verwendet ili2c, ilivalidator und ili2pg **in-process über Java APIs**. Die Abhängigkeiten werden durch Gradle aufgelöst; `build.gradle` ist die technische Source of Truth für Versionen. Die Dependency-Verifikation verhindert unbeabsichtigten Drift der gemeinsam verwendeten INTERLIS-Bibliotheken.

Es gibt keine produktive Abhängigkeit auf lokal installierte Tool-JARs oder `java -jar`-Subprozesse.

## Schemaerzeugung

```text
INTERLIS
 -> ili2c
 -> ili2pg Java API (schema import)
 -> PostgreSQL schema mabillon
 -> Cayenne DB Import
 -> cgen
```

Der Schemaimport erzeugt u. a. FKs, FK-Indizes, Unique-/Num-/Text-/DateTime-Checks, MetaInfo sowie TID- und Basket-Spalten. Mabillon verwendet kein zusätzliches `createMandatoryChecks`: direkt abbildbare `MANDATORY`-Semantik wird durch die regulären NOT-NULL-/FK-Strukturen abgedeckt; optionale Referenzen müssen nullable bleiben.

## Importvertrag

Vor jedem normalen Import:

1. XTF mit ilivalidator Java API validieren,
2. erwartetes Topic prüfen,
3. ili2pg-Import ausführen,
4. Transferidentitäten und Basketidentitäten übernehmen,
5. ili2pg-/fachliche Post-Import-Validierung ausführen.

Öffentliche Importpfade verwenden `importTid=true` und `importBid=true`.

Abhängige Topics werden in dieser Reihenfolge importiert:

```text
Kataloge -> Stammdaten -> Geschaeftsdaten
```

Eine ungültige XTF-Datei darf den DB-Import nicht erreichen.

## Exportvertrag

1. ili2pg exportiert mit Transferidentitäten,
2. das erzeugte XTF wird mit ilivalidator geprüft,
3. ein ungültiger Export wird nicht als erfolgreich ausgeliefert,
4. relevante fachliche Counts/Identitäten werden in Regressionstests geprüft.

## Semantischer Roundtrip

Der Regressionstest überträgt Katalog-, Stamm- und Geschäftsdaten in eine frische PostgreSQL-Datenbank und vergleicht den XTF-Datengraph semantisch. Er schützt insbesondere:

- BID der Baskets,
- TID der Objekte,
- fachliche Attribute,
- REF-Beziehungen,
- Cross-Topic-Referenzen.

PostgreSQL-interne `t_id`-Werte dürfen sich unterscheiden; sie sind kein Transfervertrag.

## Neue versus bestehende Datenbank

- **Neu/Test:** Schema immer aus dem aktuellen `.ili` generieren.
- **Bestehende Produktion:** neues Referenzschema generieren, Schema-Diff prüfen und daraus eine bewusste Migration ableiten. Flyway kann Migrationen ausführen, ist aber nicht Source of Truth.
