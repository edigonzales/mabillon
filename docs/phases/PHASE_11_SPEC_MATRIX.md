# Phase 11 – Specification Closure Matrix

**Status:** Working baseline  
**Branch:** `agent/phase-11-spec-closure-matrix`  
**Reference:** `MABILLON_IMPLEMENTATION_SPEC.md` v0.4  
**Purpose:** Evidence-based closure of the gap between specification, implementation, UI, security and automated verification.

## 1. Status model

A use case is only `PASS` when the specification is implemented end-to-end and the repository contains sufficient automated evidence for the relevant layers.

- `PASS` – implemented and sufficiently evidenced.
- `PARTIAL` – substantial implementation exists, but at least one required layer is incomplete.
- `FAIL` – a mandatory part is missing or contradicts the specification.
- `N/A` – explicitly out of scope by the specification.

The following dimensions are considered:

1. application/domain service,
2. business rules and persistence,
3. web/UI reachability where relevant,
4. authorization/security,
5. integration/browser/INTERLIS verification as required by the specification.

A phase report marked `SUCCESS` is not by itself evidence that a use case is complete.

## 2. Cross-cutting blockers

The following issues prevent a strict claim that the current application fully satisfies the specification:

| ID | Severity | Finding | Required closure |
|---|---|---|---|
| X-SEC-01 | P0 | `SecurityConfiguration` ends with `anyRequest().permitAll()`. Many read paths therefore do not enforce the specification's authenticated-user precondition / `VIEW_MABILLON`. | Default-deny/authenticated policy for all fachliche routes, with only explicit public exceptions. |
| X-SEC-02 | P0 | In-memory development users have default `{noop}` passwords; `sachbearbeiter` can remain at the default password in production-like compose usage. | Dev/test-only identity configuration; production startup must not silently use default credentials. |
| X-AUDIT-01 | P0 | Fachlicher journal user may fall back to `anna.mueller` when the authenticated principal cannot be mapped. | Remove silent person fallback; map authenticated identities deterministically or use an explicit technical actor. |
| X-STORAGE-01 | P0 | `UnterlageService` finalizes storage inside the Cayenne unit-of-work callback, while `CayenneUnitOfWork` commits the DB only afterwards. | Implement and failure-test the specified staging/DB/storage ordering and compensation behavior. |
| X-TEST-01 | P1 | Specification requires Playwright Java; no Playwright dependency/test is present although phase reports describe browser/Playwright verification as passed. | Add real automated Playwright golden-path tests. |
| X-TEST-02 | P1 | `Phase0CompatibilityTest` uses a large shared PostgreSQL fixture/state, conflicting with the rule that tests must not share persistent mutable state between test methods. | Split/restructure integration tests and make their persistent state independent/reproducible. |
| X-VAL-01 | P1 | Web layer primarily uses raw `@RequestParam` and exceptions; the specified structured validation/error model is not consistently implemented. | Introduce consistent form/command validation and user-facing field/domain errors. |
| X-DQ-01 | P1 | Mandatory rule DQ-007 (`geschlossenes Dossier mit offenem Geschäft`) is missing from `DataQualityService`. | Implement rule plus positive and negative PostgreSQL tests. |
| X-SEARCH-01 | P1 | Global search generates object links for Beteiligte/Unterlagen that are not backed by corresponding detail routes; matching logic is also overly positional/generic. | Make every result navigable and replace positional matching with explicit type-specific matching. |
| X-PERF-01 | P2 | Several search/query services load complete entity sets and filter/sort/page in Java. | Push search, ordering, counting and pagination into Cayenne/PostgreSQL before larger datasets. |

## 3. Use-case closure matrix

| UC | Use case | Status | Evidence / remaining gap |
|---|---|---|---|
| UC-001 | Meine Arbeit anzeigen | PARTIAL | Dashboard/query exists, but the required authenticated-user precondition is not reliably enforced because read routes are generally permitted anonymously. |
| UC-002 | Geschäft suchen | PARTIAL | Search filters and pagination API/UI exist. Implementation currently materializes/filter/sorts in Java rather than querying/paging in the DB; granular spec verification is insufficient. |
| UC-003 | Dossier suchen | PARTIAL | Search filters and UI exist. Same Java-side materialization/pagination weakness as UC-002; strict verification incomplete. |
| UC-004 | Neues Geschäft eröffnen | PARTIAL | Service and UI exist; numbering, initial process status and lifecycle behavior are implemented. Spec-level independent integration/security evidence still needs closure. |
| UC-005 | Neues Dossier eröffnen | PARTIAL | Service and UI exist with numbering and registratur position checks. Spec-level independent integration/security evidence still needs closure. |
| UC-006 | Geschäft bestehendem Dossier zuordnen | PASS | Specification explicitly limits this to assignment during business creation; no post-creation move is required. |
| UC-007 | Dossier anzeigen | PARTIAL | Detail view exists with related information, journal and fachsystem references. Access is not protected by default and the specified tab/detail completeness needs formal verification. |
| UC-008 | Geschäft anzeigen | PARTIAL | Detail view includes process/result, tasks, participations, references and journal. Default read authorization remains a blocker; underlagen/detail integration should be verified explicitly. |
| UC-009 | Geschäft bearbeiten | PARTIAL | Explicit update service and POST flow exist. Dedicated edit-form UX/validation and complete spec tests remain incomplete. |
| UC-010 | Prozessstatus ändern | PARTIAL | Service and UI flow exist and validate target status/business type. Permission/integration coverage must be made explicit under the Phase-11 gate. |
| UC-011 | Geschäftsergebnis erfassen | PARTIAL | Service and UI flow exist. Permission/integration coverage and structured validation need closure. |
| UC-012 | Beteiligten erfassen | FAIL | Service exists, but the specification's duplicate-warning behavior is not implemented as a complete use case and there is no dedicated participant create/detail web flow. |
| UC-013 | Beteiligten einem Geschäft zuordnen | PARTIAL | Service and POST controller exist. `add()` does not enforce `gueltigBis >= gueltigVon` although `update()` does; update/end UI flows are missing. |
| UC-014 | Unterlage registrieren | FAIL | Upload/metadata/hash and service/UI exist, but DB/storage commit ordering contradicts the specified failure-safe staging sequence. |
| UC-015 | Unterlage einem Geschäft zuordnen | PARTIAL | Service validates same-dossier consistency. A complete web flow/detail page and strict verification are incomplete. |
| UC-016 | Eingegangene E-Mail registrieren | PARTIAL | Specialized service exists; no complete dedicated UI/verification of the incoming-email flow is evident. |
| UC-017 | Ausgangsschreiben registrieren | PARTIAL | Supported through generic/specialized registration and outgoing date fields, but dedicated end-to-end evidence is incomplete. |
| UC-018 | Unterlage anzeigen/herunterladen | PARTIAL | Download endpoint sets MIME/content-disposition correctly. Specified `UnterlageQueryService.get`/detail screen is not exposed as `/unterlagen/{tid}`. |
| UC-019 | Aufgabe erstellen | PARTIAL | Service and POST flow exist. Independent integration/security evidence needs closure. |
| UC-020 | Aufgabe bearbeiten | FAIL | `AufgabeService.update` exists but `AufgabeController` exposes no update flow. |
| UC-021 | Aufgabe erledigen | PARTIAL | Service and controller flow exist with status/journal behavior. Spec-level integration/security evidence needs closure. |
| UC-022 | Eigene Aufgaben verwalten | PARTIAL | Query support exists, but the controller is primarily action-oriented and does not expose the full specified task-management/list/detail flow. |
| UC-023 | Fachsystemreferenz erfassen | PARTIAL | Add-to-business and add-to-dossier flows exist. Removal/audit semantics and security verification need closure. |
| UC-024 | Journal eines Geschäfts anzeigen | PARTIAL | Journal is shown on business detail. Global read security and actor attribution prevent strict PASS. |
| UC-025 | Geschäft abschliessen | PARTIAL | Close service/UI and major domain rules exist. Edge cases and independent positive/negative rule tests must be explicitly proven; null/inconsistent imported process state also deserves a guard. |
| UC-026 | Dossier abschliessen | PARTIAL | Close service/UI exists with quality/business checks. Role enforcement exists at service level, but full web/security verification and missing DQ-007 prevent PASS. |
| UC-027 | Geschäftsart konfigurieren | PARTIAL | Catalog creation supports `resultatErforderlich`, but general update/edit UI/service behavior required by the spec is incomplete. |
| UC-028 | Prozessstatus konfigurieren | PARTIAL | Creation supports initial/terminal/sortierung, activation/deactivation exists; general editing/update flow is incomplete. |
| UC-029 | Kataloge pflegen | PARTIAL | List/create/activate/deactivate implemented. Specification describes CRUD-style maintenance including update; no general update flow is implemented. |
| UC-030 | Organisationseinheiten pflegen | PARTIAL | List/create/deactivate exists; specified update maintenance/UI is incomplete. |
| UC-031 | Benutzer pflegen | PARTIAL | List/create/deactivate exists; specified update maintenance/UI is incomplete. Authentication-to-fachlicher-user mapping is also unsafe due audit fallback. |
| UC-032 | Registraturplan pflegen | FAIL | Service contains create/activate/replace operations, but controller exposes mainly list/move/deactivate. Required plan creation/activation/replacement UI is missing. |
| UC-033 | Registraturplanposition pflegen | PARTIAL | Service supports create/update/move/deactivate, but web layer exposes only move/deactivate; create/update UI is missing. |
| UC-034 | Katalogdaten importieren/exportieren | PARTIAL | INTERLIS exchange implementation is substantial and validation-aware; a strict automated export→clean DB→import→semantic roundtrip is not yet demonstrated. |
| UC-035 | Stammdaten importieren/exportieren | PARTIAL | Same as UC-034: strong implementation, insufficient semantic roundtrip evidence. |
| UC-036 | Geschäftsdaten importieren/exportieren | PARTIAL | Same as UC-034/035; TID/BID/export evidence exists, but full semantic graph roundtrip remains a Phase-11 requirement. |
| UC-037 | Abgeschlossene Dossiers zur Aussonderung suchen | PARTIAL | Archive candidate/query functionality exists and phase 9 is comparatively strong. Strict independent acceptance mapping should be added to the matrix tests. |
| UC-038 | Archivablieferung zusammenstellen | PARTIAL | Create/add/remove and quality gates are implemented. Strict role/integration evidence should be made explicit. |
| UC-039 | SIP erzeugen | PARTIAL | Strong eCH-0160-based implementation with package/hash/profile handling and tests. Mark PASS only after independent test-state/security gate closure. |
| UC-040 | SIP validieren | PARTIAL | Persistent validation/report behavior exists and is one of the strongest areas. Overall test/security closure still prevents strict PASS. |
| UC-041 | SIP-Ablieferung dokumentieren | PARTIAL | Transfer recording/journal functionality exists. Needs explicit permission and integration acceptance evidence. |
| UC-042 | Dossier nach erfolgreicher Ablieferung kennzeichnen | PARTIAL | Acceptance/archive state handling exists. Needs explicit end-to-end acceptance verification. |
| UC-043 | Systemweite Suche | FAIL | Search exists, but result links for at least Beteiligte/Unterlagen are not consistently backed by valid detail routes; generic positional matching can apply filters to wrong semantic fields. |
| UC-044 | Geschäftskontrolle / Fristenübersicht | PARTIAL | Functionality exists, but should be included in the default-authenticated policy and receive explicit integration/performance acceptance coverage. |
| UC-045 | Datenqualität prüfen | FAIL | DQ framework and nearly all mandatory rules exist, but DQ-007 is missing. The specification requires all DQ-001…DQ-013. |
| UC-046 | Historie/Audit nachvollziehen | FAIL | Journaling is pervasive, but the authenticated principal may be attributed to an unrelated fachlicher fallback user. Audit correctness is therefore not reliable enough for PASS. |

## 4. Summary

Strict status from the current repository baseline:

- **PASS:** 1 use case (`UC-006`)
- **PARTIAL:** 36 use cases
- **FAIL:** 9 use cases

This deliberately conservative classification reflects the specification's own Definition of Done. A `PARTIAL` use case often already has most of its business implementation; it is not equivalent to "half implemented". The most common reasons for `PARTIAL` are missing web reachability, missing default authorization, insufficient independent automated evidence, or incomplete validation/error semantics.

## 5. Phase-11 work order

The matrix should be closed in the following order:

1. **11.1 Security default-deny** – close X-SEC-01.
2. **11.2 Identity & audit** – close X-SEC-02 and X-AUDIT-01.
3. **11.3 DB/storage consistency** – close X-STORAGE-01 and make UC-014 safe.
4. **11.4 Test isolation** – close X-TEST-02 before adding more integration tests.
5. **11.5 Missing UI use cases** – especially UC-012, UC-020, UC-027…UC-033 and Unterlage detail/lifecycle flows.
6. **11.6 Unterlage lifecycle** – implement/update/finalize/register/unassign behavior from section 14.7.
7. **11.7 Business rules / DQ** – add DQ-007 and participation create-date validation.
8. **11.8 Search correctness** – valid result routes and explicit type-specific filtering.
9. **11.9 Validation/error model** – structured form/domain errors instead of generic failures.
10. **11.10 INTERLIS semantic roundtrip** – export → fresh DB → import → semantic comparison.
11. **11.11 Real Playwright golden path** – automated browser E2E, not manual browser smoke.
12. **11.12 DB-side search/pagination** – remove Java-side full scans where relevant.
13. **11.13 Dev/prod security separation** – production-safe identity configuration.
14. **11.14 Final spec verification** – rerun this matrix and permit no unresolved MUST/UC requirement.

## 6. Hard final gate

Phase 11 may be reported `SUCCESS` only when:

- every mandatory UC is `PASS`, or an explicit specification change marks it out of scope;
- every writing use case has a PostgreSQL/Cayenne integration test;
- every business rule has positive and negative automated evidence;
- every security-relevant use case has a permission test;
- test methods do not rely on shared mutable persistent state;
- a real Playwright Java golden path passes;
- DQ-001 through DQ-013 are implemented and tested;
- INTERLIS export/import has a semantic roundtrip test preserving relevant identities and references;
- no fachlicher page/download is accidentally anonymous;
- audit attribution cannot silently impersonate another fachlicher user;
- document storage failure tests demonstrate no accepted silent inconsistency.

Only after this gate should the implementation be described as **Mabillon MVP 1.0 / Pilot Candidate** rather than merely a functionally broad technical MVP.
