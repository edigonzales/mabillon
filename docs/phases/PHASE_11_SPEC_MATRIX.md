# Phase 11 – Specification Closure Matrix

**Status:** Working baseline  
**Branch:** `agent/phase-11-spec-closure-matrix`  
**Reference:** `MABILLON_IMPLEMENTATION_SPEC.md` v0.5  
**Purpose:** Evidence-based closure of the gap between specification, implementation, UI, security and automated verification.

## 1. Status model

A use case is only `PASS` when the specification is implemented end-to-end and the repository contains sufficient automated evidence for the relevant layers.

- `PASS` – implemented and sufficiently evidenced.
- `PARTIAL` – substantial implementation exists, but at least one required layer is incomplete.
- `FAIL` – a mandatory part is missing or contradicts the specification.
- `N/A` – explicitly out of scope by the specification.

The relevant dimensions are application/domain service, business rules and persistence, web/UI reachability, authorization/security and automated integration/browser/INTERLIS verification.

## 2. Cross-cutting findings

| ID | Severity | Status | Finding / implementation | Remaining closure |
|---|---|---|---|---|
| X-SEC-01 | P0 | IMPLEMENTED | Fachliche routes are default-deny and require a Mabillon role; focused MVC security tests execute successfully in GitHub Actions. | Keep covered by final Phase-11 gate. |
| X-SEC-02 | P0 | OPEN | In-memory development users still use configurable `{noop}` credentials and development defaults. | 11.14 dev/prod security separation. |
| X-AUDIT-01 | P0 | IMPLEMENTED | No silent `anna.mueller` fallback remains; login aliases map deterministically to fachliche users and unknown actors fail closed. | Keep covered by final gate. |
| X-CI-01 | P0 | IMPLEMENTED | GitHub Actions runs Java 25 and `./gradlew test`, with Testcontainers/Docker and failure reports. | Keep green through final closure. |
| X-STORAGE-01 | P0 | IMPLEMENTED | Unterlage registration uses staging → DB commit → final storage move, with explicit compensation after post-commit storage failure. | Keep focused filesystem/PostgreSQL tests green. |
| X-TEST-01 | P1 | OPEN | Specification requires a real Playwright Java golden path. | 11.12. |
| X-TEST-02 | P1 | IMPLEMENTED | `Phase0CompatibilityTest` restores a pristine PostgreSQL baseline and filesystem state before every method. | Keep order independence covered. |
| X-VAL-01 | P1 | OPEN | Web layer still primarily uses raw request parameters/exceptions rather than the specified structured validation/error model. | 11.10. |
| X-DQ-01 | P1 | OPEN | Mandatory DQ-007 (`geschlossenes Dossier mit offenem Geschäft`) is missing. | 11.8. |
| X-SEARCH-01 | P1 | OPEN | Result-route gaps have been reduced by participant/detail UI; full type-specific search correctness still needs closure. | 11.9. |
| X-PERF-01 | P2 | OPEN | Several searches materialize/filter/page in Java. | 11.13. |

## 3. Use-case closure matrix

| UC | Use case | Status | Evidence / remaining gap |
|---|---|---|---|
| UC-001 | Meine Arbeit anzeigen | PARTIAL | Dashboard/query and identity mapping exist; final acceptance evidence remains incomplete. |
| UC-002 | Geschäft suchen | PARTIAL | Search/UI exist; DB-side filtering/paging remains 11.13. |
| UC-003 | Dossier suchen | PARTIAL | Search/UI exist; DB-side filtering/paging remains 11.13. |
| UC-004 | Neues Geschäft eröffnen | PARTIAL | Service/UI/numbering exist; final independent acceptance mapping remains. |
| UC-005 | Neues Dossier eröffnen | PARTIAL | Service/UI/numbering exist; final independent acceptance mapping remains. |
| UC-006 | Geschäft bestehendem Dossier zuordnen | PASS | Assignment during business creation is the specified scope. |
| UC-007 | Dossier anzeigen | PARTIAL | Detail view and related information exist; final acceptance verification remains. |
| UC-008 | Geschäft anzeigen | PARTIAL | Detail view, tasks, participations, references and journal exist; final acceptance verification remains. |
| UC-009 | Geschäft bearbeiten | PARTIAL | Update service/UI exist; structured validation remains 11.10. |
| UC-010 | Prozessstatus ändern | PARTIAL | Service/UI and catalog validation exist; final permission/integration mapping remains. |
| UC-011 | Geschäftsergebnis erfassen | PARTIAL | Service/UI exist; structured validation/final evidence remain. |
| UC-012 | Beteiligten erfassen | PARTIAL | 11.6 adds participant list/create/detail/edit routes and forms. Duplicate-warning behavior remains 11.8. |
| UC-013 | Beteiligten einem Geschäft zuordnen | PARTIAL | Add/update/end actions are reachable in 11.6; add-date validation remains 11.8. |
| UC-014 | Unterlage registrieren | PARTIAL | Upload/storage/compensation are implemented and tested; 11.7 completes remaining lifecycle/detail flows. |
| UC-015 | Unterlage einem Geschäft zuordnen | PARTIAL | Service enforces same-dossier consistency; 11.7 completes assign/unassign lifecycle UI. |
| UC-016 | Eingegangene E-Mail registrieren | PARTIAL | Specialized service exists; dedicated UI/evidence remain incomplete. |
| UC-017 | Ausgangsschreiben registrieren | PARTIAL | Supported by registration services; dedicated end-to-end evidence remains incomplete. |
| UC-018 | Unterlage anzeigen/herunterladen | PARTIAL | Download endpoint is tested; 11.7 adds the missing `/unterlagen/{tid}` detail screen. |
| UC-019 | Aufgabe erstellen | PARTIAL | Service and POST flow exist; final acceptance mapping remains. |
| UC-020 | Aufgabe bearbeiten | PARTIAL | 11.6 adds list/detail/edit/update/delegate web flows on top of the existing service. |
| UC-021 | Aufgabe erledigen | PARTIAL | Service/controller flow exists; final acceptance mapping remains. |
| UC-022 | Eigene Aufgaben verwalten | PARTIAL | 11.6 exposes own-task list/detail/edit/delegate flows; final acceptance evidence remains. |
| UC-023 | Fachsystemreferenz erfassen | PARTIAL | Dossier/business add flows exist; final removal/audit acceptance remains. |
| UC-024 | Journal eines Geschäfts anzeigen | PARTIAL | Detail view shows journal with deterministic actor attribution; final event coverage remains. |
| UC-025 | Geschäft abschliessen | PARTIAL | Close service/UI and major rules exist; remaining rule evidence is handled later in Phase 11. |
| UC-026 | Dossier abschliessen | PARTIAL | Close service/UI exist; DQ-007 blocks strict closure. |
| UC-027 | Geschäftsart konfigurieren | PARTIAL | 11.6 adds catalog update UI/service; structured validation/final evidence remain. |
| UC-028 | Prozessstatus konfigurieren | PARTIAL | 11.6 adds update UI/service alongside activation/deactivation. |
| UC-029 | Kataloge pflegen | PARTIAL | 11.6 completes create/update/activate/deactivate reachability; final acceptance remains. |
| UC-030 | Organisationseinheiten pflegen | PARTIAL | 11.6 adds update maintenance UI/service; final acceptance remains. |
| UC-031 | Benutzer pflegen | PARTIAL | 11.6 adds update maintenance UI/service; final acceptance remains. |
| UC-032 | Registraturplan pflegen | PARTIAL | 11.6 exposes plan create/activate/replace flows; no longer a missing-use-case FAIL. |
| UC-033 | Registraturplanposition pflegen | PARTIAL | 11.6 exposes create/update/move/deactivate flows; final acceptance remains. |
| UC-034 | Katalogdaten importieren/exportieren | PARTIAL | Java-API import/export works; semantic roundtrip remains 11.11. |
| UC-035 | Stammdaten importieren/exportieren | PARTIAL | Java-API import/export works; semantic roundtrip remains 11.11. |
| UC-036 | Geschäftsdaten importieren/exportieren | PARTIAL | TID/BID-aware Java-API flow works; semantic roundtrip remains 11.11. |
| UC-037 | Abgeschlossene Dossiers zur Aussonderung suchen | PARTIAL | Archive candidate/query functionality exists; final acceptance mapping remains. |
| UC-038 | Archivablieferung zusammenstellen | PARTIAL | Create/add/remove and quality gates exist; final role/integration mapping remains. |
| UC-039 | SIP erzeugen | PARTIAL | Strong implementation/tests exist; final browser/security closure remains. |
| UC-040 | SIP validieren | PARTIAL | Persistent validation/report behavior exists; final closure remains. |
| UC-041 | SIP-Ablieferung dokumentieren | PARTIAL | Transfer recording/journal exists; final permission evidence remains. |
| UC-042 | Dossier nach erfolgreicher Ablieferung kennzeichnen | PARTIAL | Acceptance/archive state handling exists; final end-to-end evidence remains. |
| UC-043 | Systemweite Suche | FAIL | Search matching remains overly positional/generic and not yet fully type-specific. 11.9 owns closure. |
| UC-044 | Geschäftskontrolle / Fristenübersicht | PARTIAL | Functionality and route protection exist; final integration/performance evidence remains. |
| UC-045 | Datenqualität prüfen | FAIL | DQ-007 is still missing. 11.8 owns closure. |
| UC-046 | Historie/Audit nachvollziehen | PARTIAL | Journaling and fail-closed actor attribution are implemented; final event acceptance coverage remains. |

## 4. Summary

Strict status after completed 11.6:

- **PASS:** 1 use case (`UC-006`)
- **PARTIAL:** 43 use cases
- **FAIL:** 2 use cases (`UC-043`, `UC-045`)

`PARTIAL` is deliberately conservative: many use cases are functionally complete but remain PARTIAL until their final business-rule, validation, permission or E2E evidence is closed by the later Phase-11 packages.

## 5. Phase-11 execution status

- **11.1 Security default-deny:** complete; focused tests green in GitHub Actions.
- **11.2 Identity & audit:** complete; unit/PostgreSQL tests green.
- **11.CI Continuous integration baseline:** complete and continuously exercised.
- **11.3 DB/storage consistency:** complete; sequencing/compensation tests green.
- **11.4 Test isolation:** complete; PostgreSQL/filesystem reset is deterministic.
- **11.5 INTERLIS Java-API integration:** complete; ili2pg/ili2db 5.5.1, coherent dependency set, no external JAR runtime, no `createMandatoryChecks`, binding spec v0.5 aligned.
- **11.6 Missing UI use cases:** complete. Participant, task, catalog/master-data and registraturplan maintenance routes/forms are reachable; `Phase11UiReachabilityTest` and the full suite are green in GitHub Actions (Run #97 after the final JTE UUID fix).
- **11.7 Unterlage lifecycle:** IN PROGRESS. Metadata update, assign/unassign, finalize, aktenrelevant registration, cancellation, detail page and transition tests are being added.
- **Next after 11.7:** 11.8 Business rules / data quality.
- **X-SEC-02:** intentionally deferred to 11.14.

## 6. Hard final gate

Phase 11 may be reported `SUCCESS` only when:

- every mandatory UC is `PASS`, or explicitly changed out of scope;
- every writing use case has PostgreSQL/Cayenne integration evidence;
- every business rule has positive and negative automated evidence;
- every security-relevant use case has permission coverage;
- tests do not rely on shared mutable persistent state;
- a real Playwright Java golden path passes;
- DQ-001 through DQ-013 are implemented and tested;
- INTERLIS export/import has a semantic roundtrip preserving relevant identities and references;
- no fachlicher page/download is accidentally anonymous;
- audit attribution cannot silently impersonate another fachlicher user;
- document storage failure tests demonstrate no accepted silent inconsistency;
- GitHub Actions remains green without a second independent INTERLIS toolchain.

Only after this gate should the implementation be described as **Mabillon MVP 1.0 / Pilot Candidate**.
