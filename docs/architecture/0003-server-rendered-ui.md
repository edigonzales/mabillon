# ADR 0003: Server-rendered HTML with progressive enhancement

## Status

Accepted for Phase 0.

## Context

Mabillon is an internal public-sector application where transparent HTTP flows,
accessibility, and reliable fallback behavior are more important than a large
client-side runtime. The application also needs forms, tables, filters, and
detail workspaces that remain understandable without JavaScript.

## Decision

Use Spring MVC with JTE templates and vanilla CSS. HTMX 2.x is an optional
progressive-enhancement layer for partial updates; normal HTTP requests remain
the fallback and use the same controller/service business path. Controllers
receive and return form/view models and contain no domain logic.

No React, Vue, Angular, Bootstrap, Tailwind, or external CSS/font CDN is a
baseline dependency.

## Consequences

- Every mutation has a normal HTTP path before its HTMX enhancement.
- View models protect templates from persistence implementation details.
- MVC tests cover ordinary requests and HTMX fragment behavior together.
