---
name: mabillon-testing
description: Plan, implement, or review tests for Mabillon. Use for every behavior change and every phase gate. Requires unit tests for business rules, real PostgreSQL/Testcontainers+Cayenne integration tests for persistence, MVC/JTE tests for web behavior, and targeted Playwright tests for critical user flows. H2 is forbidden.
compatibility: Codex and OpenCode
metadata:
  project: mabillon
  category: testing
---

# Mabillon testing rules

Testing is part of implementation, not a cleanup step.

## Database

Use PostgreSQL via Testcontainers for persistence integration. Do not use H2 or another compatibility database.

Every write use case that touches Cayenne/persistence needs at least one real PostgreSQL+Cayenne integration test.

## Business rules

For every meaningful rule, add:

- a positive case,
- each materially different negative/boundary case.

Examples that must stay covered:

- Unterlage -> Geschaeft only when both belong to same Dossier,
- process/result status belongs to Geschaeftsart,
- Dossier cannot close with open Geschaeft,
- required result before Geschaeft closure,
- journal entry commits atomically with the domain change,
- TID/BID preservation on XTF import,
- archive delivery excludes invalid/open dossiers.

## Web tests

Use Spring MVC tests for:

- status codes/redirects,
- authorization,
- full-page behavior,
- HTMX fragment behavior,
- validation rendering.

Use Playwright only for critical end-to-end flows and phase gates, not to replace lower-level tests.

## Golden path

Maintain one stable Nomenklatur E2E fixture from application intake through Dossier/Geschaeft, tasks/documents, decision, closure and eventually archive/SIP.

## Test quality

Tests must be deterministic, independent of execution order, and explicit about fixtures. No shared persistent state across test methods.

Forbidden shortcuts:

- deleting a failing test,
- `@Disabled` to get green,
- weakening assertions,
- mocking persistence when the mapping/SQL itself is under test,
- swallowing exceptions merely to continue a phase.

Read `references/test-matrix.md` when deciding the minimum test set for a change.
