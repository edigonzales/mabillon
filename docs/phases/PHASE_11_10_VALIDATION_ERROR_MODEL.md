# Phase 11.10 – Validation / Error Model

**Status:** SUCCESS  
**Date:** 2026-08-17  
**Scope:** structured validation/domain errors and deterministic HTTP error semantics

## Problem

The web layer previously relied on a mixture of raw `IllegalArgumentException`, `ResponseStatusException` and generic error rendering. Command validation often exposed Java field names or only one unstructured message. HTMX and normal requests did not have a single deterministic mapping from validation/domain failures to HTTP status and presentation.

## Implemented

A small common error model now exists around `MabillonException`:

- `ValidationException` with `FieldError(field, code, message)`
- `NotFoundException`
- `ConflictException`
- `AuthorizationException`
- dedicated technical exception types for storage/SIP boundaries

`DomainRuleViolationException` remains the specific domain-rule type and is a `ConflictException`, so existing service semantics stay intact without a big-bang rewrite.

The central `WebExceptionHandler` maps failures deterministically:

- **400 Bad Request** – structured validation, missing request parameters, type conversion and remaining legacy invalid arguments
- **404 Not Found** – missing Mabillon resources and unknown web resources
- **409 Conflict** – validly formed requests that violate a domain/business rule
- **403 Forbidden** – application-level authorization failures

Unknown technical failures are deliberately not swallowed by a catch-all handler and therefore remain visible as real server errors.

Normal HTTP requests render the full error page. HTMX requests render the small `error/_notice.jte` fragment with `role="alert"` and the same structured field errors.

The central create/update commands for dossiers, businesses, participants, tasks and documents now expose field-specific errors with stable codes such as `required`, `range` and `dateOrder`. Existing public command contracts that were already asserted by older compatibility tests were preserved where changing the exception class would have been gratuitous.

During the audit a remaining SIP actor fallback was also found and removed, keeping the Phase-11.2 fail-closed audit rule intact.

## Automated evidence

`Phase11ValidationErrorIntegrationTest` verifies against the real PostgreSQL/Spring MVC stack:

- field name, stable code and human-readable message for structured command validation;
- **400** for invalid form input;
- **404** for missing resources;
- **409** for domain-rule conflicts;
- HTMX error rendering through the alert fragment rather than a full page.

Existing Phase-0/11 tests continue to cover legacy command contracts and authorization behavior.

## CI gate

- Run #148 completed successfully with the new structured validation/error model and focused regression test.
- Final Phase-11.10 head Run #151 on commit `b926e4713e702002898ed008e30eae271e11b59c` completed successfully after the task-validation and SIP actor cleanups.

## Decision

Phase 11.10 is complete. Validation failures are structured where forms need field-level feedback, domain conflicts have a stable 409 meaning, missing resources have a stable 404 meaning, and HTMX/non-HTMX requests share the same error semantics.

**Next:** Phase 11.11 – INTERLIS semantic roundtrip.
