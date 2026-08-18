# Phase 11.4 – Test Isolation

**Status:** Implemented; full `Phase0CompatibilityTest` execution awaits 11.5 INTERLIS Java-API fixtures.  
**Branch:** `agent/phase-11-spec-closure-matrix`

## Problem

`Phase0CompatibilityTest` imported the Golden-Path fixtures once in `@BeforeAll` and then allowed all test methods to read and mutate the same PostgreSQL database and filesystem state.

That made the suite order-dependent. In particular, tests could change global catalog/registratur values, advance technical number sequences, create dossiers/businesses/tasks/documents and leave SIP/storage artifacts for later tests.

The binding specification requires test methods to be independent and not share mutable persistent state.

## Solution

The Golden-Path INTERLIS fixture import remains a one-time class setup because repeatedly running schema import and three XTF imports for every method would be unnecessarily expensive.

After the one-time import, `Phase0DatabaseIsolationExtension` creates a PostgreSQL dump of the pristine `mabillon` schema. Before every test method it:

1. restores the `mabillon` schema from that baseline dump;
2. truncates `mabillon_app.number_sequence` when the technical sequence table already exists;
3. clears the document-storage test root;
4. clears the test SIP root.

This keeps one Testcontainers PostgreSQL instance but removes shared mutations between test methods.

The `mabillon_app` schema is deliberately not dropped. `PostgresNumberSequenceStore` caches its initialization state in the Spring application instance; dropping the schema/table behind that instance would create an artificial test-only failure. Truncating the sequence table restores the intended empty per-test numbering state while keeping the runtime object's initialization contract valid.

## Verification

`MabillonDatabaseBaselineTest` is independent of the INTERLIS fixture tooling and runs against a real PostgreSQL/PostGIS Testcontainer. It proves that:

- a mutation in the `mabillon` schema disappears after restore;
- the original baseline row is restored;
- technical number-sequence rows are removed.

GitHub Actions run `32029576164` executed 21 tests. The new baseline test passed. The only four failing tests remained the already known tests whose class/fixture setup invokes the obsolete external INTERLIS tool JARs:

- `InterlisExchangeServiceTest.localToolAdaptersValidateModelAndPositiveXtf`
- `Phase0CompatibilityTest` initialization
- `JournalIdentityIntegrationTest` initialization
- `UnterlageStorageConsistencyIntegrationTest` initialization

Therefore the isolation mechanism itself is verified. After 11.5 replaces the external fixture/tool path with the approved Java APIs, the complete `Phase0CompatibilityTest` must execute under this per-method reset and the full CI gate must become green.

## Deliberate non-goal

11.4 does not split `Phase0CompatibilityTest` merely to reduce file size. The specification problem is shared mutable persistent state, not the number of test classes. Feature-oriented splitting can be done later when it improves maintainability, but it is not required to obtain deterministic isolation and would add substantial churn without improving the isolation guarantee.
