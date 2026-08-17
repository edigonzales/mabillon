# Phase 11.7 – Unterlage lifecycle

**Status:** COMPLETE  
**Branch:** `agent/phase-11-spec-closure-matrix`

## Scope

Phase 11.7 closes the missing application/web lifecycle around `Unterlage` without adding document versioning or a second storage abstraction.

Implemented operations:

- metadata update,
- assign to a business in the same dossier,
- unassign from business,
- finalize,
- register as aktenrelevant,
- cancel instead of physical deletion,
- `/unterlagen/{tid}` detail page with the valid lifecycle actions,
- journal entries for every mutating operation.

## State transitions

The INTERLIS `UnterlagenStatus` remains the source of truth:

```text
In_Arbeit -> Final -> Registriert
     |          |          |
     +----------+----------+-> Storniert
```

Rules implemented by `UnterlageService`:

- a newly registered aktenrelevant Unterlage is immediately `Registriert`;
- a newly registered non-aktenrelevant Unterlage starts as `In_Arbeit`;
- only `In_Arbeit` can be finalized;
- only `Final` can be registered as aktenrelevant;
- aktenrelevant registration sets `aktenrelevant=true` and status `Registriert`;
- `Storniert` is terminal for normal mutations;
- metadata/assignment mutations still require an editable dossier and, where present, an editable business;
- assigning to a business retains the same-dossier invariant;
- cancellation remains audit-visible and performs no physical deletion.

## Web reachability

`UnterlageController` now exposes:

- `GET /unterlagen/{tid}`
- `POST /unterlagen/{tid}` – metadata update
- `POST /unterlagen/{tid}/geschaeft` – assign
- `POST /unterlagen/{tid}/geschaeft/entfernen` – unassign
- `POST /unterlagen/{tid}/finalisieren`
- `POST /unterlagen/{tid}/aktenrelevant-registrieren`
- `POST /unterlagen/{tid}/stornieren`
- existing `GET /unterlagen/{tid}/download`

The detail template only renders lifecycle actions valid for the current status and hides mutation forms once the Unterlage is `Storniert`.

## Automated evidence

`UnterlageLifecycleIntegrationTest` runs against PostgreSQL/Testcontainers and the real Cayenne/Spring application. It verifies:

- `In_Arbeit -> Final -> Registriert -> Storniert`,
- rejection of invalid repeated transitions,
- metadata persistence,
- assign/unassign persistence,
- terminal cancellation behavior,
- Unterlage journal events,
- real MVC/JTE detail rendering,
- lifecycle POST actions and redirects.

GitHub Actions Run #102 (`test: verify unterlage lifecycle end to end`) completed successfully, including the full Gradle test suite.

## Deliberately deferred

- duplicate participant warnings/date rules and DQ-007: 11.8,
- type-specific global-search correctness: 11.9,
- structured field/domain validation rendering: 11.10,
- document versioning: outside the MVP specification.
