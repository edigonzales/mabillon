---
name: mabillon-spring-jte-htmx
description: Implement or review Mabillon controllers, forms, JTE templates and HTMX interactions while preserving HTML-first behavior.
compatibility: Codex and OpenCode
metadata:
  project: mabillon
  category: web
---

# Spring MVC + JTE + HTMX

Read `docs/development/web-ui.md` and ADR 0004.

Implement business behavior once in Application Services. Controllers parse/validate HTTP input, invoke services, build view models and choose full-page/redirect or HTMX fragment responses.

Keep CSRF enabled, server-side business validation authoritative, and normal HTTP fallbacks functional. Do not expose Cayenne objects to templates. Reuse existing Mabillon UI tokens/components instead of adding a new frontend framework.
