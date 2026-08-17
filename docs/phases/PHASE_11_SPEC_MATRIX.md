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

The following dimensions are considered:

1. application/domain service,
2. business rules and persistence,
3. web/UI reachability where relevant,
4. authorization/security,
5. integration/browser/INTERLIS verification as required by the specification.

A phase report marked `SUCCESS` is not by itself evidence that a use case is complete. For Phase-11 findings, `IMPLEMENTED` means the repository contains the intended fix and automated test code; final closure still requires an executed green verification run.

## 2. Cross-cutting findings

| ID | Severity | Status | Finding / implementation | Remaining closure |
|---|---|---|---|---|
| X-SEC-01 | P0 | IMPLEMENTED | Fachliche routes are default-deny and require a Mabillon role; only health and explicit static assets remain public. Focused MVC security tests cover anonymous, roleless, Sachbearbeiter and Admin access. The tests execute successfully in GitHub Actions. | Keep the policy covered by the final green Phase-11 verification run. |
| X-SEC-02 | P0 | OPEN | In-memory development users still use configurable `{noop}` credentials and development defaults. | Dev/test-only identity configuration; production startup must not silently use default credentials. |
| X-AUDIT-01 | P0 | IMPLEMENTED | `JournalService` no longer falls back to `anna.mueller`. `SpringSecurityCurrentActor` maps the development login aliases deterministically to configured fachliche usernames; an unmapped principal is accepted only under its own username and a writing use case fails if no corresponding fachlicher `Benutzer` exists. Unit and PostgreSQL/Cayenne integration tests cover the mapping and negative case. | The PostgreSQL/Cayenne integration test executes successfully in GitHub Actions after the 11.5 Java-API fixture migration; keep it covered by the final Phase-11 gate. |
| X-CI-01 | P0 | IMPLEMENTED | `.github/workflows/ci.yml` now runs Java 25 and `./gradlew test` on pushes to `main`/`agent/**` and pull requests to `main`, using the GitHub-hosted Docker daemon for Testcontainers and uploading Gradle test reports on failure. A real runner reaches and executes the tests. | The gate is green without installing a second independent INTERLIS CLI/JAR toolchain; keep it green through final closure. |
| X-STORAGE-01 | P0 | IMPLEMENTED | `DocumentStorage.describe` now plans and verifies final metadata/URI without moving the staged file. `UnterlageService` commits Unterlage plus journal through `CayenneUnitOfWork` before calling the final storage move. If that move fails after DB commit, a compensation transaction deletes the newly created Unterlage and its journal event and storage cleanup removes staging/partial final state. The old `anna.mueller` fallback in `UnterlageService` was removed as part of the same fail-closed path. | Both the filesystem sequencing test and PostgreSQL/Cayenne compensation test execute successfully in GitHub Actions after the 11.5 fixture migration. |
| X-TEST-01 | P1 | OPEN | Specification requires Playwright Java; no Playwright dependency/test is present although phase reports describe browser/Playwright verification as passed. | Add real automated Playwright golden-path tests. |
| X-TEST-02 | P1 | IMPLEMENTED | `Phase0CompatibilityTest` keeps one PostgreSQL Testcontainer and one expensive Golden-Path import, but no longer shares mutations between test methods. A JUnit extension snapshots the pristine `mabillon` schema and restores it before every method, resets the technical number sequence and clears document/SIP filesystem state. `MabillonDatabaseBaselineTest` proves the PostgreSQL snapshot/restore mechanism independently in CI. | The complete `Phase0CompatibilityTest` now executes successfully in GitHub Actions under the per-method reset; keep order independence covered by the final gate. |
| X-VAL-01 | P1 | OPEN | Web layer primarily uses raw `@RequestParam` and exceptions; the specified structured validation/error model is not consistently implemented. | Introduce consistent form/command validation and user-facing field/domain errors. |
| X-DQ-01 | P1 | OPEN | Mandatory rule DQ-007 (`geschlossenes Dossier mit offenem Geschäft`) is missing from `DataQualityService`. | Implement rule plus positive and negative PostgreSQL tests. |
| X-SEARCH-01 | P1 | OPEN | Global search generates object links for Beteiligte/Unterlagen that are not backed by corresponding detail routes; matching logic is also overly positional/generic. | Make every result navigable and replace positional matching with explicit type-specific matching. |
| X-PERF-01 | P2 | OPEN | Several search/query services load complete entity sets and filter/sort/page in Java. | Push search, ordering, counting and pagination into Cayenne/PostgreSQL before larger datasets. |

## 3. Use-case closure matrix

| UC | Use case | Status | Evidence / remaining gap |
|---|---|---|---|
| UC-001 | Meine Arbeit anzeigen | PARTIAL | Dashboard/query exists. Fachliche routes now require a Mabillon role, and development login aliases resolve to fachliche usernames so personal work is queried under the fachliche identity. Full spec-level acceptance evidence remains incomplete. |
| UC-002 | Geschäft suchen | PARTIAL | Search filters and pagination API/UI exist. Implementation currently materializes/filter/sorts in Java rather than querying/paging in the DB; granular spec verification is insufficient. |
| UC-003 | Dossier suchen | PARTIAL | Search filters and UI exist. Same Java-side materialization/pagination weakness as UC-002; strict verification incomplete. |
| UC-004 | Neues Geschäft eröffnen | PARTIAL | Service and UI exist; numbering, initial process status and lifecycle behavior are implemented. Spec-level independent integration/security evidence still needs closure. |
| UC-005 | Neues Dossier eröffnen | PARTIAL | Service and UI exist with numbering and registratur position checks. Spec-level independent integration/security evidence still needs closure. |
| UC-006 | Geschäft bestehendem Dossier zuordnen | PASS | Specification explicitly limits this to assignment during business creation; no post-creation move is required. |
| UC-007 | Dossier anzeigen | PARTIAL | Detail view exists with related information, journal and fachsystem references. Default read protection is now implemented; specified tab/detail completeness still needs formal verification. |
| UC-008 | Geschäft anzeigen | PARTIAL | Detail view includes process/result, tasks, participations, references and journal. Default read protection is now implemented; underlagen/detail integration should still be verified explicitly. |
| UC-009 | Geschäft bearbeiten | PARTIAL | Explicit update service and POST flow exist. Dedicated edit-form UX/validation and complete spec tests remain incomplete. |
| UC-010 | Prozessstatus ändern | PARTIAL | Service and UI flow exist and validate target status/business type. Permission/integration coverage must be made explicit under the Phase-11 gate. |
| UC-011 | Geschäftsergebnis erfassen | PARTIAL | Service and UI flow exist. Permission/integration coverage and structured validation need closure. |
| UC-012 | Beteiligten erfassen | FAIL | Service exists, but the specification's duplicate-warning behavior is not implemented as a complete use case and there is no dedicated participant create/detail web flow. |
| UC-013 | Beteiligten einem Geschäft zuordnen | PARTIAL | Service and POST controller exist. `add()` does not enforce `gueltigBis >= gueltigVon` although `update()` does; update/end UI flows are missing. |
| UC-014 | Unterlage registrieren | PARTIAL | Upload, metadata/hash, service/UI and the specified staging → DB commit → final storage ordering are implemented. A final-storage failure triggers DB compensation and cleanup. Both the focused filesystem sequencing test and PostgreSQL/Cayenne compensation test execute successfully in CI. A complete Unterlage detail/lifecycle web flow is still missing. |
| UC-015 | Unterlage einem Geschäft zuordnen | PARTIAL | Service validates same-dossier consistency. A complete web flow/detail page and strict verification are incomplete. |
| UC-016 | Eingegangene E-Mail registrieren | PARTIAL | Specialized service exists; no complete dedicated UI/verification of the incoming-email flow is evident. |
| UC-017 | Ausgangsschreiben registrieren | PARTIAL | Supported through generic/specialized registration and outgoing date fields, but dedicated end-to-end evidence is incomplete. |
| UC-018 | Unterlage anzeigen/herunterladen | PARTIAL | Download endpoint sets MIME/content-disposition correctly. Specified `UnterlageQueryService.get`/detail screen is not exposed as `/unterlagen/{tid}`. |
| UC-019 | Aufgabe erstellen | PARTIAL | Service and POST flow exist. Independent integration/security evidence needs closure. |
| UC-020 | Aufgabe bearbeiten | FAIL | `AufgabeService.update` exists but `AufgabeController` exposes no update flow. |
| UC-021 | Aufgabe erledigen | PARTIAL | Service and controller flow exist with status/journal behavior. Spec-level integration/security evidence needs closure. |
| UC-022 | Eigene Aufgaben verwalten | PARTIAL | Query support exists, but the controller is primarily action-oriented and does not expose the full specified task-management/list/detail flow. |
| UC-023 | Fachsystemreferenz erfassen | PARTIAL | Add-to-business and add-to-dossier flows exist. Removal/audit semantics and security verification need closure. |
| UC-024 | Journal eines Geschäfts anzeigen | PARTIAL | Journal is shown on business detail. Read protection and deterministic actor attribution are now implemented; complete journal-event acceptance coverage remains to be verified. |
| UC-025 | Geschäft abschliessen | PARTIAL | Close service/UI and major domain rules exist. Edge cases and independent positive/negative rule tests must be explicitly proven; null/inconsistent imported process state also deserves a guard. |
| UC-026 | Dossier abschliessen | PARTIAL | Close service/UI exists with quality/business checks. Role enforcement exists at service level, but full web/security verification and missing DQ-007 prevent PASS. |
| UC-027 | Geschäftsart konfigurieren | PARTIAL | Catalog creation supports `resultatErforderlich`, but general update/edit UI/service behavior required by the spec is incomplete. |
| UC-028 | Prozessstatus konfigurieren | PARTIAL | Creation supports initial/terminal/sortierung, activation/deactivation exists; general editing/update flow is incomplete. |
| UC-029 | Kataloge pflegen | PARTIAL | List/create/activate/deactivate implemented. Specification describes CRUD-style maintenance including update; no general update flow is implemented. |
| UC-030 | Organisationseinheiten pflegen | PARTIAL | List/create/deactivate exists; specified update maintenance/UI is incomplete. |
| UC-031 | Benutzer pflegen | PARTIAL | List/create/deactivate exists and login-to-fachlicher-user mapping is now explicit. Specified general update maintenance/UI remains incomplete. |
| UC-032 | Registraturplan pflegen | FAIL | Service contains create/activate/replace operations, but controller exposes mainly list/move/deactivate. Required plan creation/activation/replacement UI is missing. |
| UC-033 | Registraturplanposition pflegen | PARTIAL | Service supports create/update/move/deactivate, but web layer exposes only move/deactivate; create/update UI is missing. |
| UC-034 | Katalogdaten importieren/exportieren | PARTIAL | INTERLIS exchange implementation is substantial and validation-aware; the Java-API import/export path and identity preservation are covered, but a strict automated export→clean DB→import→semantic roundtrip is not yet demonstrated. |
| UC-035 | Stammdaten importieren/exportieren | PARTIAL | Same as UC-034: Java-API import/export is verified, but semantic roundtrip evidence remains incomplete. |
| UC-036 | Geschäftsdaten importieren/exportieren | PARTIAL | Same as UC-034/035; TID/BID/export evidence exists through the Java APIs, but full semantic graph roundtrip remains a Phase-11 requirement. |
| UC-037 | Abgeschlossene Dossiers zur Aussonderung suchen | PARTIAL | Archive candidate/query functionality exists and phase 9 is comparatively strong. Strict independent acceptance mapping should be added to the matrix tests. |
| UC-038 | Archivablieferung zusammenstellen | PARTIAL | Create/add/remove and quality gates are implemented. Strict role/integration evidence should be made explicit. |
| UC-039 | SIP erzeugen | PARTIAL | Strong eCH-0160-based implementation with package/hash/profile handling and tests. Mark PASS only after independent test-state/security gate closure. |
| UC-040 | SIP validieren | PARTIAL | Persistent validation/report behavior exists and is one of the strongest areas. Overall test/security closure still prevents strict PASS. |
| UC-041 | SIP-Ablieferung dokumentieren | PARTIAL | Transfer recording/journal functionality exists. Needs explicit permission and integration acceptance evidence. |
| UC-042 | Dossier nach erfolgreicher Ablieferung kennzeichnen | PARTIAL | Acceptance/archive state handling exists. Needs explicit end-to-end acceptance verification. |
| UC-043 | Systemweite Suche | FAIL | Search exists, but result links for at least Beteiligte/Unterlagen are not consistently backed by valid detail routes; generic positional matching can apply filters to wrong semantic fields. |
| UC-044 | Geschäftskontrolle / Fristenübersicht | PARTIAL | Functionality exists and the page is now covered by default fachliche route protection. Explicit integration/performance acceptance coverage remains incomplete. |
| UC-045 | Datenqualität prüfen | FAIL | DQ framework and nearly all mandatory rules exist, but DQ-007 is missing. The specification requires all DQ-001…DQ-013. |
| UC-046 | Historie/Audit nachvollziehen | PARTIAL | Journaling is pervasive. Silent fallback attribution has been removed; login aliases map deterministically to fachliche usernames, and an unknown actor makes the write fail. Unit and PostgreSQL/Cayenne integration tests execute successfully in CI. Complete journal-event acceptance coverage is still outstanding. |

## 4. Summary

Strict status after the implemented 11.1–11.5 changes:

- **PASS:** 1 use case (`UC-006`)
- **PARTIAL:** 38 use cases
- **FAIL:** 7 use cases

This deliberately conservative classification reflects the specification's own Definition of Done. A `PARTIAL` use case often already has most of its business implementation; it is not equivalent to "half implemented". The most common remaining reasons for `PARTIAL` are missing web reachability, insufficient independent automated evidence, incomplete validation/error semantics, or the still-outstanding semantic INTERLIS roundtrip.

## 5. Phase-11 execution status

The detailed and current order is maintained in `PHASE_11_WORKPLAN.md`, which supersedes the original work-order list.

- **11.1 Security default-deny:** implementation complete; focused security tests execute successfully in GitHub Actions.
- **11.2 Identity & audit:** deterministic login-to-fachlicher-user mapping and fail-closed journal attribution implemented; unit and PostgreSQL/Cayenne integration tests execute successfully in GitHub Actions.
- **11.CI Continuous integration baseline:** implemented and exercised by real GitHub Actions runs. Java 25 setup, Gradle build, Docker/Testcontainers access and failure-report upload work. The complete test suite is green without a second independent INTERLIS installation.
- **11.3 DB/storage consistency:** implementation complete. Final storage mutation happens only after a successful Cayenne DB commit; post-commit storage failure has explicit DB compensation and storage cleanup. Filesystem sequencing and PostgreSQL/Cayenne compensation tests execute successfully in CI.
- **11.4 Test isolation:** implementation complete. The pristine Golden-Path database is snapshotted once and restored before every `Phase0CompatibilityTest`; technical numbering, document storage and SIP filesystem state are reset as well. The complete compatibility suite executes successfully in CI under the per-method reset.
- **11.5 INTERLIS Java-API integration:** implementation complete. Runtime and test fixtures use the ili2pg, ili2c and ilivalidator Java APIs; ili2pg/ili2db are pinned to 5.5.1; Gradle resolves the reviewed dependency set from `jars.interlis.ch`; `createMandatoryChecks` is deliberately not used; `SchemaConstraintRepair` is removed; TID/BID and validated export behavior are covered; and the binding specification is aligned as v0.5. The full Gradle test suite has passed in GitHub Actions on this architecture.
- **Next:** 11.6 Missing UI use cases.
- **X-SEC-02:** remains intentionally open and is handled by the later dev/prod security-separation work package.

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
- document storage failure tests demonstrate no accepted silent inconsistency;
- the GitHub Actions CI gate is green without installing a second independent INTERLIS toolchain.

Only after this gate should the implementation be described as **Mabillon MVP 1.0 / Pilot Candidate** rather than merely a functionally broad technical MVP.
