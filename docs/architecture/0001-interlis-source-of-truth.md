# ADR 0001: INTERLIS als fachliche Source of Truth

## Status

Accepted

## Kontext

Mabillon tauscht langlebige GEVER-Fachdaten aus. Datenbank, Transfermodell und generierte Persistenzabbildung dürfen nicht unabhängig voneinander driften.

## Entscheidung

`model/SO_AGI_GEVER_20260707.ili` ist die Source of Truth für die persistente fachliche Struktur.

Der reproduzierbare Fluss ist:

```text
INTERLIS -> ili2c -> ili2pg Java API -> PostgreSQL -> Cayenne DB Import/cgen -> Java
```

Fachliche Tabellen oder Spalten werden nicht zuerst durch Flyway, handgeschriebenes SQL oder Cayenne eingeführt. Rein technische Anwendungstabellen dürfen im separaten Schema `mabillon_app` liegen, wenn ihre technische Natur klar ist.

## Konsequenzen

- Modelländerungen werden vor Schemaarbeiten mit ili2c validiert.
- Positive XTF-Eingaben werden vor Import validiert.
- PostgreSQL-Constraints und Transfermetadaten werden aus dem INTERLIS-Modell abgeleitet.
- Änderungen am Modell erfordern ein frisch generiertes Referenzschema, einen überprüften Schema-/Mapping-Diff und aktualisiertes cgen.
- Migrationen bestehender produktiver Datenbanken werden bewusst aus der Differenz zweier modellgenerierter Schemata abgeleitet; Flyway bleibt Migrationsmechanismus, nicht Fachmodell.
