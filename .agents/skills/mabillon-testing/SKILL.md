---
name: mabillon-testing
description: Plan, implement or review Mabillon tests across domain rules, PostgreSQL/Cayenne, MVC, INTERLIS, storage, archive and Playwright.
compatibility: Codex and OpenCode
metadata:
  project: mabillon
  category: testing
---

# Mabillon testing

Canonical source: `docs/development/testing.md`.

Use PostgreSQL/Testcontainers for persistence. Every meaningful business rule needs positive and relevant negative/boundary evidence. Every write use case touching persistence needs real PostgreSQL/Cayenne coverage. Web behavior requires MVC validation/authorization coverage; critical journeys use Playwright.

Tests must be deterministic and order-independent. Never delete/disable/weaken a failing test merely to make the build green.
