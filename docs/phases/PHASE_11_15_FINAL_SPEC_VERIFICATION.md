# Phase 11.15 – Final specification verification

**Status:** SUCCESS – final specification verification complete.  
**Reference:** `MABILLON_IMPLEMENTATION_SPEC.md` v0.5  
**Branch:** `agent/phase-11-spec-closure-matrix`

## Goal

Phase 11.15 reruns the binding specification against the implementation and accepts no unresolved mandatory use case or cross-cutting hard gate. The verification is evidence-driven: implementation alone is not enough where the specification requires integration, permission, business-rule, browser, storage, data-quality or INTERLIS evidence.

## Result

The use-case closure matrix resolves to:

- **PASS:** 46
- **PARTIAL:** 0
- **FAIL:** 0

The final GitHub Actions full-suite gate is green for the final 11.15 implementation head `782b59bf4ea8307b46082af48afd93ac4977d160`. The normal `./gradlew test` workflow therefore closes the last outstanding release gate.

**Phase 11 is complete. Mabillon is a MVP 1.0 / Pilot Candidate.**

## Findings discovered during 11.15

The final verification found genuine implementation and verification gaps rather than merely stale matrix entries.

### Audit attribution in archive delivery

`ArchivAblieferungService` still contained a configurable fallback fachlicher username with default `anna.mueller`. An authenticated actor that could not be resolved to a domain `Benutzer` could therefore create an archive delivery attributed to somebody else.

The fallback was removed. Archive delivery creation now resolves the mapped `CurrentActor` and fails closed when the fachlicher user does not exist. `GeschaeftAuditIntegrationTest` proves both correct business journal attribution and rollback/failure for an unmapped archive actor.

### Archive workflow UI reachability

The archive domain service already supported removing dossiers, recording transfer, recording acceptance and recording rejection, but the detail UI did not expose all of those actions and the controller had no remove-dossier endpoint.

The controller/template now expose the complete specified workflow:

- add/remove dossier while the delivery is in `Entwurf`;
- mark ready;
- generate and validate SIP;
- record transfer after successful validation;
- record archive acceptance including mandatory archive signature;
- record archive rejection.

`ArchiveDeliveryWorkflowIntegrationTest` verifies composition, removal, journal events and archive permission enforcement. The existing `ArchivSipIntegrationTest` continues to exercise generation, invalid/correction/regeneration, validation, transfer and final acceptance with dossier archival.

### Registraturplan root positions

The final administration tests exposed a persistence bug for root positions: a root `Ordnungssystemposition` was assigned itself as parent. Cayenne could not persist the resulting cyclic dependency before the primary key existed.

Root positions now persist with `parent = null`; moving a position to the root also sets a null parent. The query/view model already represents null as the root semantic.

## Acceptance evidence added in 11.15

### UC-009 / UC-046 – business update and audit

`GeschaeftAuditIntegrationTest` verifies that editing a business:

- preserves the business number and business type;
- persists editable fields;
- records the journal event in the same operation;
- attributes the event to the mapped fachlicher actor;
- denies the operation without `EDIT_GESCHAEFT`.

It also proves that archive creation does not fall back to another fachlicher user when actor mapping fails.

### UC-016 / UC-017 – incoming/outgoing e-mail registration

`EmailRegistrationIntegrationTest` verifies the specified MVP behavior through `EmailRegistrationService`:

- incoming mail is registered as `EMAIL_EINGANG` with incoming/document dates;
- outgoing mail is registered as `EMAIL_AUSGANG` with outgoing/document dates;
- the original EML bytes are stored and can be opened again;
- both records are attached to the business;
- registration requires `EDIT_UNTERLAGE`.

The binding specification explicitly does not require mailbox integration for the MVP.

### UC-027…UC-033 – administration

`AdministrationIntegrationTest` adds database-backed HTTP evidence for:

- business-type configuration including `resultatErforderlich`;
- process-status configuration including business type, ordering, initial and terminal flags;
- the invariant that the sole active initial status cannot be deactivated;
- organisation-unit hierarchy creation/update;
- domain-user create/update/deactivate;
- registraturplan create/replace;
- position create/update/move and tree semantics.

Existing security tests keep `/admin/**` admin-only.

### UC-037…UC-042 – archive delivery

The archive acceptance evidence combines:

- `ArchiveDeliveryWorkflowIntegrationTest` for candidate permission, composition/removal, UI and journal behavior;
- `ArchivSipIntegrationTest` for ready → SIP generation → invalid validation → correction → regenerated valid SIP → transfer → acceptance → dossier archived;
- `DataQualityRulesIntegrationTest` for DQ-013.

### UC-045 – DQ-001 through DQ-013

`DataQualityRulesIntegrationTest` explicitly exercises DQ-001, DQ-003 and DQ-005 through DQ-013 with deliberately inconsistent states. The status-mismatch scenarios create their own foreign business type, process status and result status so the verification is independent of incidental Golden-Path catalog contents.

DQ-002 (business without dossier) and DQ-004 (document without dossier) are additionally prevented structurally by the INTERLIS-derived PostgreSQL schema: `geschaeft.geschaeftsdossier` and `unterlage.ablagedossier` are mandatory. `InterlisSchemaConstraintIntegrationTest` proves both columns reject `NULL`. The corresponding service checks remain defensive for malformed/external states.

### Role/permission matrix

`AuthorizationPermissionMatrixTest` verifies the complete declared role-to-permission mapping for:

- `SACHBEARBEITER`;
- `GEVER_VERANTWORTLICHER`;
- `ARCHIVVERANTWORTLICHER`;
- `ADMIN`.

This complements `SecurityConfigurationTest`, `ProductionSecurityConfigurationTest` and focused negative use-case tests.

## Previously closed hard gates retained

The final result retains the evidence from earlier Phase-11 steps:

- default-deny fachliche HTTP routes;
- deterministic login-to-domain-actor mapping;
- dev/test local users separated from fail-closed production security;
- storage staging/DB/final-move consistency and compensation;
- independent PostgreSQL/filesystem integration tests;
- structured validation/error semantics;
- DB-side search/count/pagination;
- real Chromium Playwright golden path;
- Java-API INTERLIS toolchain without external JAR execution;
- semantic fresh-database INTERLIS roundtrip preserving BID/TID/REF identities.

The 11.14 full-suite gate was green in GitHub Actions Run #205 (`32116846311`) on commit `10eccbd2b538ecbe4adb528175f0287ffb9e92a9`.

## Final gate

The final 11.15 head `782b59bf4ea8307b46082af48afd93ac4977d160` passes the normal GitHub Actions full-suite workflow including Testcontainers, INTERLIS tests and the real Playwright golden path.

There are no unresolved mandatory use cases, no `PARTIAL` or `FAIL` entries in the closure matrix, and no open Phase-11 hard gate.

**Phase 11 status: SUCCESS.**
