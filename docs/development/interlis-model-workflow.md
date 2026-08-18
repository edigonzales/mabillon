# INTERLIS-Modell ändern

## Grundregel

Eine fachliche persistente Schemaänderung beginnt in `model/SO_AGI_GEVER_20260707.ili`.

## Ablauf

```text
1. INTERLIS ändern
2. Modell mit ili2c Java API validieren
3. positives XTF-Fixture bei Bedarf aktualisieren und validieren
4. frisches PostgreSQL-Referenzschema via ili2pg erzeugen
5. erwartete Constraints/FKs/TID/Basket-Semantik prüfen
6. Cayenne DB Import gegen das Referenzschema
7. DataMap-Diff prüfen
8. cgen nach src/generated/java
9. Generated-Diff prüfen
10. kompilieren und Integrationstests ausführen
```

Die Skripte `scripts/validate-model.sh`, `scripts/validate-xtf.sh`, `scripts/create-schema.sh` und `scripts/import-xtf.sh` sind Convenience-Wrapper um die bereits von Gradle aufgelösten Java APIs. Sie definieren keine zweite Toolchain.

## Mandatory- und Optional-Semantik

Mabillon verwendet kein `createMandatoryChecks`. Die reguläre ili2db-Abbildung muss verpflichtende Attribute/Referenzen mit NOT NULL/FK-Strukturen durchsetzen und optionale Referenzen nullable lassen. `InterlisSchemaConstraintIntegrationTest` schützt diesen Vertrag.

`sqlEnableNull` wird nicht als Workaround verwendet.

## Bestehende Produktionsdatenbank

Nach einer Modelländerung wird nicht blind ein neues Schema über die Produktion gelegt. Stattdessen:

1. altes und neues modellgeneriertes Referenzschema vergleichen,
2. Migration bewusst erstellen und reviewen,
3. Migration gegen eine Kopie/Integrationstest ausführen,
4. Cayenne Mapping aktualisieren,
5. vollständige Regressionstests.

## Identitäten

Normale Imports erhalten TID und BID. Eine bewusste Identitätsremapping-Funktion müsste als eigener Fachvertrag definiert werden; sie ist kein Standardmodus.
