# ADR 0003: Reproduzierbare generierte Cayenne-Quellen

## Status

Accepted

## Entscheidung

Cayenne `cgen` schreibt die generierten Entity-Paare nach `src/generated/java`. Gradle kompiliert dieses Verzeichnis als Generated Source Set.

Die Dateien sind generierte Artefakte und dürfen nicht manuell verändert werden. Mappingänderungen folgen weiterhin:

```text
INTERLIS -> ili2pg -> PostgreSQL -> Cayenne DB Import -> cgen
```

## Begründung

Ein Clean Checkout und CI-Build sollen nicht von einem vorherigen lokalen Modeler-Lauf oder einem nicht versionierten `build/`-Verzeichnis abhängen. Das versionierte cgen-Ergebnis macht den Build reproduzierbar und lässt gleichzeitig jeden Mapping-Diff reviewbar bleiben.
