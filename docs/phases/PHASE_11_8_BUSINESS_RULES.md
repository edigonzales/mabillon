# Phase 11.8 – Business Rules / Data Quality

**Status:** SUCCESS  
**Date:** 2026-08-17  
**Scope:** DQ-007, participation date/lifecycle rules, participant duplicate warning

## Implemented

### DQ-007 – geschlossenes Dossier mit offenem Geschäft

`DataQualityService` now reports `DQ-007` with severity `ERROR` when a dossier is in a terminal state (`Geschlossen`, `Archiviert` or `Vernichtet`) while at least one assigned business is not terminal (`Abgeschlossen`, `Archiviert` or `Vernichtet`).

The invariant is surfaced by both dossier and business quality checks. The normal `DossierService.close` path already rejects open businesses before changing the dossier state; DQ-007 therefore also protects imported, externally changed or otherwise inconsistent persisted states.

### Participation dates and lifecycle

`BeteiligungService` keeps the existing date ordering rule (`gueltigBis >= gueltigVon`) as an explicit service invariant in addition to command validation.

Participation mutations now also respect the business lifecycle consistently:

- add requires an editable business;
- update requires an editable business;
- end requires an editable business;
- a participation of an `Abgeschlossen`, `Archiviert` or `Vernichtet` business cannot be changed afterwards.

### Participant duplicate warning

Participant creation now performs a lightweight duplicate check before persistence. It is intentionally a warning, not a global merge or hard uniqueness rule.

Potential duplicates are detected within the same participant type by:

- identical non-empty external reference, or
- identical non-empty e-mail address, or
- normalized identical name; for persons, first name must also match.

The creation flow shows the matching records and offers an explicit **„Trotzdem erfassen“** action. The underlying create service remains capable of creating the record after explicit confirmation; no automatic merge is introduced.

## Automated evidence

`Phase11BusinessRulesIntegrationTest` uses its own PostgreSQL/PostGIS Testcontainer and one mutating test method so it does not reintroduce shared mutable test state.

It verifies:

- no DQ-007 finding for an open dossier;
- DQ-007/ERROR after deliberately creating the otherwise forbidden inconsistent state through the persistence layer;
- DQ-007 is visible from dossier and business quality checks;
- invalid participation date ranges are rejected;
- valid ranges persist correctly;
- participation update/end are rejected after business completion;
- duplicate lookup detects an existing participant;
- the HTTP creation flow renders the duplicate warning;
- explicit `duplicateConfirmed=true` still creates the participant.

## CI gate

GitHub Actions **Run #113** on commit `4048fe7b52f5c3c1dc6ea3e95e2466ff958b5fb2` completed successfully with the full `./gradlew test --no-daemon` suite.

## Decision

Phase 11.8 is complete. No rules engine, merge subsystem or additional domain abstraction was introduced; the rules remain in the existing application services where they are used.

**Next:** Phase 11.9 – Search correctness.
