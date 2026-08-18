# ADR 0002: Cayenne DB-first Persistence Mapping

## Status

Accepted

## Kontext

Das PostgreSQL-Schema wird aus INTERLIS erzeugt. Cayenne soll die tatsächlich generierten Tabellen, Fremdschlüssel, Basket-Metadaten und Transferidentitäten abbilden, ohne ein zweites handgepflegtes Domänenmapping einzuführen.

## Entscheidung

Apache Cayenne 5.0-M2 wird DB-first verwendet. Bei einer Modelländerung wird zuerst ein frisches PostgreSQL-Referenzschema mit ili2pg erzeugt. Danach folgen Cayenne DB Import, Review des DataMap-Diffs und cgen.

Generierte Cayenne-Basisklassen werden nie manuell geändert. Fachverhalten gehört in Feature Packages und explizite Application Services.

Die Webschicht exponiert keine Cayenne-Objekte an Templates und speichert keinen `ObjectContext` in der HTTP-Session. Schreibende Use Cases verwenden einen Unit of Work, damit Fachänderung und Journalereignis atomar committen oder gemeinsam zurückrollen.

## Konsequenzen

- Unerwartete Schema-/Mappingänderungen werden an der Quelle diagnostiziert.
- `t_id`, `t_ili_tid` und `t_basket` bleiben technische Identitäten/Metadaten und werden nicht als Fachnummern verwendet.
- Cayenne ist Persistenztechnologie, nicht Anwendungsarchitektur.
