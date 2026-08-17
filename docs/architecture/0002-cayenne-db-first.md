# ADR 0002: Cayenne DB-first persistence mapping

## Status

Accepted for Phase 0.

## Context

The PostgreSQL schema is generated from INTERLIS. Cayenne must represent the
actual generated tables, foreign keys, basket metadata, and transfer IDs
without introducing a second hand-authored domain mapping.

## Decision

Apache Cayenne 5.0-M2 is used DB-first. For a model change, the project first
creates a fresh PostgreSQL schema with ili2pg, then runs Cayenne Modeler DB
Import, reviews the mapping diff, runs cgen, and reviews generated-code
changes. Generated Cayenne base classes are never edited manually. Application
behavior belongs in feature packages and explicit service/use-case classes.

The HTTP layer does not expose Cayenne objects to templates and never stores an
`ObjectContext` in the HTTP session. Write use cases use a unit of work so the
business change and its journal entry commit atomically.

## Consequences

- Schema or mapping surprises are treated as diagnostics, not patched around.
- `t_id`, `t_ili_tid`, and `t_basket` remain technical identities and are not
  used as business numbers.
- The Phase 0 MCP smoke test is a mandatory gate; unavailable Modeler MCP is a
  blocker rather than an invitation to replace the prescribed verification
  silently.
