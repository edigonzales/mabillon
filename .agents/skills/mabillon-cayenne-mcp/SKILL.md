---
name: mabillon-cayenne-mcp
description: Work with Apache Cayenne DB-first persistence, Modeler MCP, DataMap, cgen, ObjectContext and PostgreSQL integration.
compatibility: Codex and OpenCode
metadata:
  project: mabillon
  category: persistence
---

# Cayenne workflow

Read `docs/development/cayenne.md`, ADR 0002 and ADR 0003 before changing mapping/generated code.

Cayenne is DB-first. After a model/schema change: fresh PostgreSQL reference schema -> Modeler DB Import -> inspect DataMap diff -> cgen -> inspect generated diff -> compile -> PostgreSQL integration tests.

Never manually edit generated base classes. No Cayenne objects in JTE or HTTP sessions. Write use cases use short-lived `ObjectContext`s and commit domain change plus journal atomically.
