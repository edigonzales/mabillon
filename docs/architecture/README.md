# Architektur

Mabillon ist ein modularer Spring-Boot-Monolith mit serverseitig gerenderter Oberfläche. Die fachliche Persistenz ist modellgetrieben: INTERLIS erzeugt das PostgreSQL-Referenzschema, Cayenne importiert dieses Schema DB-first und stellt die Persistenzobjekte bereit.

## Hauptfluss

```text
Browser
  -> Spring MVC Controller
  -> Application/Query Service
  -> CayenneUnitOfWork
  -> Cayenne
  -> PostgreSQL

INTERLIS
  -> ili2c
  -> ili2pg Java API
  -> PostgreSQL
  -> Cayenne DB Import + cgen
```

JTE erhält View Models, keine Cayenne-PersistentObjects. HTMX ist eine progressive Ergänzung normaler HTTP-Flows.

## ADRs

1. [ADR 0001 – INTERLIS als fachliche Source of Truth](0001-interlis-source-of-truth.md)
2. [ADR 0002 – Cayenne DB-first Persistence Mapping](0002-cayenne-db-first.md)
3. [ADR 0003 – Reproduzierbare generierte Cayenne-Quellen](0003-generated-cayenne-sources.md)
4. [ADR 0004 – Serverseitig gerenderte UI](0004-server-rendered-ui.md)
5. [ADR 0005 – Mabillon UI-Designsprache](0005-ui-design-language.md)

## Weitere Architekturthemen

- [Security](security.md)
- [Storage und Konsistenz](storage.md)
- [INTERLIS-Schnittstelle](../interfaces/interlis.md)
