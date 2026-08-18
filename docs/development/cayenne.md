# Cayenne-Entwicklung

## Rolle von Cayenne

Cayenne 5.0-M2 ist die Persistenztechnologie, nicht die Domänen- oder Webarchitektur.

```text
Controller -> Application Service -> CayenneUnitOfWork -> Cayenne -> PostgreSQL
```

## DB-first

Cayenne erzeugt das fachliche Schema nicht. Nach einer INTERLIS-Modelländerung:

1. frisches PostgreSQL-Schema erzeugen,
2. Cayenne-Projekt im Modeler öffnen,
3. DB Import ausführen,
4. DataMap-Diff prüfen,
5. cgen ausführen,
6. Generated-Diff prüfen,
7. kompilieren und Integrationstests ausführen.

Der lokale Cayenne Modeler MCP kann `open_project`, `dbimport_run` und `cgen_run` automatisieren. CI muss den Modeler/MCP nicht benötigen, weil das cgen-Ergebnis unter `src/generated/java` versioniert ist.

## Generated Code

Generierte Basisklassen niemals manuell ändern. Bei falschem Output zuerst INTERLIS, ili2pg-/DB-Schema, Cayenne DB Import und cgen-Konfiguration prüfen.

## ObjectContext

- kurzlebig pro Use Case/Unit of Work,
- kein `ObjectContext` in HTTP-Session,
- kein globaler mutable Context,
- Fachänderung und Journal im selben Context,
- Rollback bei Fach-/Persistenzfehlern.

## Queries

Typisierte Cayenne-Queries sind Standard. Für klar begründete technische Sequenzen, Metadatenprüfungen oder sehr komplexe read-only Reports darf explizites JDBC/SQL eingesetzt werden; solches SQL benötigt PostgreSQL-Integrationstests.
