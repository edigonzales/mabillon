# ADR 0003: Reproduzierbare Cayenne-Quellen

## Status

Accepted for Phase 10.

## Decision

Cayenne `cgen` writes its MCP-generated entity pairs to
`src/generated/java`. Gradle compiles that directory as a generated source
set. The files are generated artifacts and must never be edited manually;
mapping changes still follow the mandatory sequence INTERLIS → ili2pg →
PostgreSQL → Cayenne DB Import → MCP cgen.

## Rationale

The earlier `build/generated/sources/cayenne` location made a clean checkout
depend on a prior local Modeler run. Keeping the MCP output in the project
makes `./gradlew clean check` reproducible in CI and in the deployment build,
while preserving the DB-first source-of-truth rule.
