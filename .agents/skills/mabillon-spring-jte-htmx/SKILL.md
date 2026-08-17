---
name: mabillon-spring-jte-htmx
description: Implement or review Mabillon web features with Spring Boot 4.1.0, Spring MVC, JTE and HTMX. Use for controllers, forms, view models, templates, fragments, navigation, security integration, uploads, and progressive enhancement. Keeps the application HTML-first and usable through normal HTTP flows.
compatibility: Codex and OpenCode
metadata:
  project: mabillon
  category: web
---

# Spring MVC + JTE + HTMX conventions

## Fixed baseline

- Java 25
- Spring Boot 4.1.0
- Servlet/Spring MVC
- JTE
- HTMX 2.x as specified by the project
- no SPA framework

Do not upgrade framework versions during feature work unless the user explicitly approves it.

## HTML-first rule

Implement business behavior once in Application Services. A controller may return either:

- a full page / redirect for normal HTTP, or
- a JTE fragment for an HTMX request.

HTMX is progressive enhancement, not a second business path.

## Controller boundaries

Controllers:

- parse/validate HTTP input,
- invoke application use cases/query services,
- build form/view models,
- choose full-page or fragment response.

Controllers do not own business rules and never use ObjectContext directly.

Templates receive purpose-built view models, not Cayenne objects.

## Form/write behavior

- Prefer standard forms and POST/Redirect/GET for non-HTMX fallback.
- Keep CSRF protection enabled and compatible with HTMX.
- On validation errors, return field + summary errors with preserved user input.
- Use real labels and semantic HTML controls.
- Avoid client-side-only validation for business rules.
- Stable human business numbers may be URL keys as specified.

## HTMX

Use HTMX when it reduces navigation noise: status blocks, task completion, filtering, pagination, side details, inline forms. Avoid turning the page into an implicit client-side state machine.

Centralize `HX-Request` detection. Fragments must have predictable IDs/targets and reuse the same JTE components as full pages.

## JavaScript

Do not add custom JavaScript if native HTML/HTMX is sufficient. Any JS module must have a narrowly documented reason and tests where behavior is critical.

For styling, always load the `mabillon-ui-design` skill too.
