---
name: mabillon-cayenne-mcp
description: Work with Apache Cayenne 5.0-M2 persistence, Cayenne Modeler MCP, DB Import, DataMap, cgen, ObjectContext, or Cayenne integration tests. Use after ili2pg schema changes and for persistence implementation. Never hand-edit generated Cayenne base classes.
compatibility: Codex and OpenCode
metadata:
  project: mabillon
  category: persistence
---

# Cayenne 5.0-M2 DB-first workflow

## Architecture boundary

Cayenne is a persistence mechanism, not the application architecture.

`Controller -> Application Service -> persistence adapter / CayenneUnitOfWork -> Cayenne`

Do not expose Cayenne PersistentObjects or ObjectContext to JTE templates or HTTP sessions.

## Model update workflow

After any INTERLIS model change:

1. Use the `mabillon-interlis-ili2pg` workflow to recreate the reference PostgreSQL schema.
2. Open the Cayenne project in Cayenne Modeler via the configured MCP server.
3. Run DB Import via MCP.
4. Inspect the DataMap diff. Do not blindly accept unexpected tables, duplicate relationships, wrong cardinalities, or ili2db metadata mapped as business objects.
5. Run cgen via MCP.
6. Inspect generated diff.
7. Compile.
8. Run PostgreSQL+Cayenne integration tests.

If MCP is unavailable in Phase 0, mark the gate failed/blocked. Do not silently replace the mandated MCP verification with manual Modeler work.

## Generated code

Never manually edit generated Cayenne base classes. Put custom behavior only in designated non-generated subclasses/adapters/services.

If generated code is wrong, diagnose in this order:

1. INTERLIS,
2. ili2pg translation/options and DB constraints/FKs,
3. Cayenne DB Import mapping,
4. cgen configuration.

## ObjectContext rules

- short-lived per write use case / unit of work,
- no ObjectContext in HTTP session,
- no mutable global singleton context,
- commit domain write + journal event atomically,
- rollback on any domain/persistence failure.

## Query rules

Use typed Cayenne queries for ordinary object navigation/CRUD. For a genuinely complex quality/reporting query, explicit SQL is acceptable behind a query service if it is clearer and fully tested. Do not introduce jOOQ/JPA as a second persistence framework without explicit approval.
