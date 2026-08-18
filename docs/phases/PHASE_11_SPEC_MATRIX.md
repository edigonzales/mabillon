# Phase 11 – Specification Closure Matrix

**Status:** 11.15 Final Candidate – use-case closure complete, final full-suite CI pending  
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

| ID | Severity | Status | Evidence |
|---|---|---|---|
| X-SEC-01 | P0 | PASS | Fachliche routes are default-deny; `SecurityConfigurationTest` rejects anonymous and non-Mabillon identities and protects `/admin/**`. |
| X-SEC-02 | P0 | PASS | Local users exist only in explicit `dev`/`test`; other profiles fail closed. `ProductionSecurityConfigurationTest` verifies production separation. 11.14 full-suite Run #205 (`32116846311`) is green. |
| X-AUDIT-01 | P0 | PASS | Login aliases map deterministically to fachliche users. 11.15 removed the remaining archive actor fallback; unmapped archive actors now fail closed and roll back. |
| X-CI-01 | P0 | PASS* | GitHub Actions runs Java 25, Testcontainers, the full Gradle test suite and real Playwright Chromium. `*` The final 11.15 head still requires its closing green run. |
| X-STORAGE-01 | P0 | PASS | Unterlage registration uses staging → DB commit → final storage move with compensation; PostgreSQL/filesystem tests cover failure paths. |
| X-TEST-01 | P1 | PASS | `PlaywrightGoldenPathE2ETest` executes the binding-spec Nomenklatur workflow in real Chromium. Run #160 is green. |
| X-TEST-02 | P1 | PASS | The old `Phase0CompatibilityTest` monolith is gone. Thematic integration tests share `MabillonIntegrationTestSupport`; `MabillonIntegrationDatabaseIsolationExtension` restores the PostgreSQL baseline and filesystem before every test. 11.14 Run #205 is green. |
| X-VAL-01 | P1 | PASS | Structured validation plus deterministic 400/404/409/403 handling for normal HTTP and HTMX are covered by focused integration tests. |
| X-DQ-01 | P1 | PASS | `DataQualityRulesIntegrationTest` covers DQ-001, DQ-003 and DQ-005…DQ-013. DQ-002 and DQ-004 are additionally proven impossible in the valid schema by `InterlisSchemaConstraintIntegrationTest` mandatory-FK tests. |
| X-SEARCH-01 | P1 | PASS | Global search uses type-specific semantics and DB-side counts/filtering/pagination; correctness and pagination integration tests are green in the established Phase-11 baseline. |
| X-PERM-01 | P1 | PASS | `AuthorizationPermissionMatrixTest` verifies every declared permission for Sachbearbeitung, GEVER, Archiv and Admin roles, complementing HTTP and focused negative permission tests. |
| X-INTERLIS-01 | P0 | PASS | Java-API INTERLIS import/export/validation and fresh-PostgreSQL semantic roundtrip preserve relevant BID/TID/REF identities. |

## 3. Use-case closure matrix

| UC | Use case | Status | Evidence |
|---|---|---|---|
| UC-001 | Meine Arbeit anzeigen | PASS | `GeschaeftIntegrationTest` creates active work/open overdue tasks and verifies populated `MyWorkQueryService` lists; dashboard is authenticated and Playwright-tested. |
| UC-002 | Geschäft suchen | PASS | DB-side filters/count/order/pagination plus MVC/search integration evidence. |
| UC-003 | Dossier suchen | PASS | DB-side filters/count/order/pagination plus MVC/search integration evidence. |
| UC-004 | Neues Geschäft eröffnen | PASS | PostgreSQL service/UI/numbering plus real Playwright creation. |
| UC-005 | Neues Dossier eröffnen | PASS | PostgreSQL service/UI/numbering plus real Playwright creation. |
| UC-006 | Geschäft bestehendem Dossier zuordnen | PASS | Specified assignment-at-creation scope is exercised by the real Golden Path. |
| UC-007 | Dossier anzeigen | PASS | Detail UI and relationships render in MVC/Playwright lifecycle flows. |
| UC-008 | Geschäft anzeigen | PASS | Detail UI renders tasks, participants, documents, references and journal in MVC/Playwright flows. |
| UC-009 | Geschäft bearbeiten | PASS | `GeschaeftAuditIntegrationTest` verifies HTTP update, immutable number/type, persisted editable fields, audit attribution and negative `EDIT_GESCHAEFT` permission. |
| UC-010 | Prozessstatus ändern | PASS | Service validates status/business type, normal HTTP and HTMX are integration-tested, and the real Golden Path changes status; role/permission matrix covers `EDIT_GESCHAEFT`. |
| UC-011 | Geschäftsergebnis erfassen | PASS | Service/UI validation and real-browser result capture before closure. |
| UC-012 | Beteiligten erfassen | PASS | Participant create/search/detail/edit routes exist; duplicate warning and participant creation/validation are PostgreSQL/MVC-tested. |
| UC-013 | Beteiligten einem Geschäft zuordnen | PASS | Add/update/end rules and journal behavior are integration-tested; Playwright assigns the participant. |
| UC-014 | Unterlage registrieren | PASS | Upload/storage/compensation/lifecycle plus real-browser upload and registration. |
| UC-015 | Unterlage einem Geschäft zuordnen | PASS | Assign/unassign and same-dossier consistency are integration-tested and exercised in Playwright. |
| UC-016 | Eingegangene E-Mail registrieren | PASS | `EmailRegistrationIntegrationTest` verifies `EMAIL_EINGANG`, dates, stored EML bytes, business attachment and negative permission. The MVP spec explicitly requires no mailbox integration. |
| UC-017 | Ausgangsschreiben registrieren | PASS | `EmailRegistrationIntegrationTest` verifies `EMAIL_AUSGANG`, outgoing/document dates, stored EML bytes, business attachment and permission. |
| UC-018 | Unterlage anzeigen/herunterladen | PASS | Detail/download behavior is MVC-tested; real browser renders registered documents. |
| UC-019 | Aufgabe erstellen | PASS | Service/create validation and Playwright form flow persist the task. |
| UC-020 | Aufgabe bearbeiten | PASS | `GeschaeftIntegrationTest` updates and delegates tasks, verifies lifecycle/journal rules and rejection after completion; web edit/delegate flows are reachable. |
| UC-021 | Aufgabe erledigen | PASS | Service/controller integration and Playwright completion. |
| UC-022 | Eigene Aufgaben verwalten | PASS | Populated own/open/overdue work lists are integration-tested; task list/edit/delegate UI exists and queries are DB-side. |
| UC-023 | Fachsystemreferenz erfassen | PASS | `SearchIntegrationTest` adds/removes dossier/business references and verifies journal/search behavior. |
| UC-024 | Journal eines Geschäfts anzeigen | PASS | Journal query/rendering plus Playwright verification of creation/status/result/closure events and mapped actor. |
| UC-025 | Geschäft abschliessen | PASS | Positive/negative close rules plus complete Playwright closure. |
| UC-026 | Dossier abschliessen | PASS | Close rules and DQ-007 are integration-tested; Playwright closes only after business completion. |
| UC-027 | Geschäftsart konfigurieren | PASS | `AdministrationIntegrationTest` HTTP-creates/updates/deactivates a business type and verifies `resultatErforderlich` and persisted fields. |
| UC-028 | Prozessstatus konfigurieren | PASS | `AdministrationIntegrationTest` verifies business-type link, ordering, initial/terminal flags and the sole-initial-status invariant. |
| UC-029 | Kataloge pflegen | PASS | Create/update/activate/deactivate behavior, historical readability and specialized catalog rules are integration-tested. |
| UC-030 | Organisationseinheiten pflegen | PASS | `AdministrationIntegrationTest` creates hierarchy, updates the child and verifies persisted parent semantics. |
| UC-031 | Benutzer pflegen | PASS | `AdministrationIntegrationTest` creates, updates and deactivates a fachlicher user and verifies organisation mapping. |
| UC-032 | Registraturplan pflegen | PASS | `AdministrationIntegrationTest` creates and replaces a plan and verifies validity/status. |
| UC-033 | Registraturplanposition pflegen | PASS | `AdministrationIntegrationTest` creates root/child positions, updates and moves them; cycle/inactive historical behavior has separate integration evidence. |
| UC-034 | Katalogdaten importieren/exportieren | PASS | Java-API import/export, ilivalidator and fresh-DB semantic roundtrip. |
| UC-035 | Stammdaten importieren/exportieren | PASS | Java-API import/export, ilivalidator and fresh-DB semantic roundtrip. |
| UC-036 | Geschäftsdaten importieren/exportieren | PASS | Fresh-DB roundtrip explicitly compares semantic graph and BID/TID/REF identities. |
| UC-037 | Abgeschlossene Dossiers zur Aussonderung suchen | PASS | `AussonderungQueryService` performs final DQ-aware eligibility; archive permission has a focused negative test. |
| UC-038 | Archivablieferung zusammenstellen | PASS | `ArchiveDeliveryWorkflowIntegrationTest` verifies create/add/remove through UI/service, journal events and archive permission. |
| UC-039 | SIP erzeugen | PASS | `ArchivSipIntegrationTest` generates structured eCH-0160 SIP content and validates storage/hash/package structure. |
| UC-040 | SIP validieren | PASS | Invalid package → correction required → regeneration → valid package is integration-tested with persistent validation result. |
| UC-041 | SIP-Ablieferung dokumentieren | PASS | Transfer is service/integration-tested; 11.15 exposes transfer/accept/reject actions in the archive detail UI. |
| UC-042 | Dossier nach erfolgreicher Ablieferung kennzeichnen | PASS | Existing archive integration test records transfer/acceptance, archive signature and verifies dossier `Archiviert`; UI now exposes the complete final flow. |
| UC-043 | Systemweite Suche | PASS | Type-specific correctness, navigable targets and DB-side per-type counts/pagination are integration-tested. |
| UC-044 | Geschäftskontrolle / Fristenübersicht | PASS | Open/overdue metrics and DB-side control queries are integration-tested. |
| UC-045 | Datenqualität prüfen | PASS | DQ-001…DQ-013 are now explicitly covered by `DataQualityRulesIntegrationTest` plus mandatory-schema tests for DQ-002/DQ-004. |
| UC-046 | Historie/Audit nachvollziehen | PASS | Journal rendering and lifecycle events are covered throughout; `GeschaeftAuditIntegrationTest` proves mapped actor attribution and fail-closed archive attribution with rollback. |

## 4. Summary

Final 11.15 use-case status:

- **PASS:** 46 use cases
- **PARTIAL:** 0 use cases
- **FAIL:** 0 use cases

There is no remaining mandatory use-case gap in the closure matrix. The only remaining Phase-11 gate is a green full GitHub Actions run for the final 11.15 head.

## 5. Phase-11 execution status

- **11.1 Security default-deny:** complete.
- **11.2 Identity & audit:** complete.
- **11.CI Continuous integration baseline:** complete and continuously exercised.
- **11.3 DB/storage consistency:** complete.
- **11.4 Test isolation:** complete; the monolithic compatibility test has been replaced by thematic tests and generic DB/filesystem isolation.
- **11.5 INTERLIS Java-API integration:** complete.
- **11.6 Missing UI use cases:** complete.
- **11.7 Unterlage lifecycle:** complete.
- **11.8 Business rules / data quality:** complete.
- **11.9 Search correctness:** complete.
- **11.10 Validation / error model:** complete.
- **11.11 INTERLIS semantic roundtrip:** complete; Run #153 green.
- **11.12 Real Playwright golden path:** complete; Run #160 green.
- **11.13 DB-side search/pagination:** complete; Run #175 green.
- **11.14 Dev/prod security separation:** complete. Full suite Run #205 (`32116846311`) on `10eccbd2b538ecbe4adb528175f0287ffb9e92a9` is green.
- **11.15 Final specification verification:** use-case closure complete. Final full-suite CI for the current 11.15 head is the remaining release gate.

See `PHASE_11_15_FINAL_SPEC_VERIFICATION.md` for the detailed final-verification findings and the implementation gaps discovered during 11.15.

## 6. Hard final gate

| Gate | Status |
|---|---|
| Every mandatory UC is PASS or explicitly out of scope | PASS |
| Every writing use case has PostgreSQL/Cayenne integration evidence | PASS |
| Business rules have positive/negative automated evidence | PASS |
| Security-relevant use cases have permission coverage | PASS |
| Tests do not rely on shared mutable persistent state | PASS |
| Real Playwright Java golden path passes | PASS |
| DQ-001 through DQ-013 implemented and tested | PASS |
| INTERLIS semantic roundtrip preserves relevant identities/references | PASS |
| No fachlicher page/download accidentally anonymous | PASS |
| Audit attribution cannot silently impersonate another fachlicher user | PASS |
| Storage failure tests demonstrate no accepted silent inconsistency | PASS |
| Final 11.15 GitHub Actions full suite green | **PENDING** |

Phase 11 may be reported `SUCCESS` and the implementation described as **Mabillon MVP 1.0 / Pilot Candidate** only after the final pending CI gate is green.
