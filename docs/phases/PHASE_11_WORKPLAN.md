# Phase 11 – Specification Closure & Hardening Work Plan

**Status:** Approved working plan  
**Branch:** `agent/phase-11-spec-closure-matrix`  
**Related:** `docs/phases/PHASE_11_SPEC_MATRIX.md`

This work plan supersedes the work-order list in section 5 of the initial Phase-11 matrix where the two documents differ.

## Architecture change: INTERLIS tools are in-process Java libraries

The Phase-11 target architecture changes the current Spring Boot runtime integration of ili2pg, ilivalidator and ili2c.

The current implementation launches external JVMs through `ProcessBuilder` and hard-coded/overridable tool JAR paths. This is no longer the target architecture.

The approved target is:

```text
Spring Boot
    |
    +-- InterlisExchangeService
    |      |
    |      +-- Ili2pgRunner ----------------> ili2pg Java API
    |      +-- XtfValidator ----------------> ilivalidator Java API
    |
    +-- InterlisModelValidator -------------> ili2c Java API

All libraries are resolved by Gradle from https://jars.interlis.ch.
```

The existing application-facing interfaces stay in place unless a concrete API limitation makes a small adjustment necessary. The ProcessBuilder implementations are replaced behind those interfaces.

### Version baseline

Use **ili2pg 5.5.1 instead of 5.5.2**. The reason is dependency compatibility: ili2pg, ilivalidator and ili2c share several INTERLIS libraries, and the selected versions must resolve to a mutually compatible set.

The Gradle build must use the INTERLIS Maven repository:

```groovy
repositories {
    mavenCentral()
    maven { url = uri('https://jars.interlis.ch') }
}
```

The exact final dependency set is accepted only after Gradle dependency convergence has been inspected. Do not solve conflicts by allowing arbitrary newest-version wins.

Shared libraries such as `ili2c-core`, `iox-ili`, `iox-api`, `ili2db`, `ehibasics` and related transitive dependencies must resolve consistently with the selected ili2pg 5.5.1 / ili2c / ilivalidator combination.

### Runtime implementation

Replace:

- `ProcessBuilderIli2pgRunner`
- `ProcessBuilderXtfValidator`
- `ProcessBuilderInterlisModelValidator`
- `ProcessBuilderSupport`

with in-process adapters, for example:

- `JavaApiIli2pgRunner`
- `JavaApiXtfValidator`
- `JavaApiInterlisModelValidator`

The adapters must call the supported Java APIs directly. No `java -jar`, shell command construction, temporary subprocess JVM, or parsing of console output as the primary result channel remains in the Spring Boot runtime.

`InterlisExchangeService` should keep its current orchestration semantics:

```text
Import:
XTF -> ilivalidator Java API -> topic check -> ili2pg Java API import
    -> post-import validation

Export:
ili2pg Java API export -> ilivalidator Java API -> expose result only when valid
```

Normal imports must continue to preserve TIDs and BIDs. Export must continue to preserve the required transfer identities.

### Configuration cleanup

Runtime configuration must no longer require:

- `ILI2PG_JAR`
- `ILI2C_JAR`
- `ILIVALIDATOR_JAR`
- `/Users/stefan/apps/...` defaults
- tool JARs mounted/copied into the application container

Keep model/model-repository configuration (`MABILLON_MODEL`, `MABILLON_MODEL_DIR` or a cleaner Spring equivalent) where it remains useful.

The Docker image must be self-contained through normal Gradle application dependencies; no separate INTERLIS CLI installation is required for application import/export/validation.

### Build and verification tooling

Prefer one dependency/version source for runtime and verification. Gradle tasks may call the same Java APIs for model/XTF/schema verification. Existing shell scripts may remain as thin convenience wrappers, but they must not introduce a second independent set of INTERLIS tool versions.

The binding specification currently prescribes ProcessBuilder implementations and fixed external JAR paths. Phase 11 must update those sections so the specification reflects the newly approved Java-API architecture before final spec closure.

### Mandatory-constraint review

Before carrying the current `--createMandatoryChecks` behavior into the Java-API implementation, verify whether Mabillon actually needs these additional mandatory CHECK constraints.

The default ili2db schema mapping already creates SQL `NOT NULL` constraints for directly mappable INTERLIS `MANDATORY` attributes unless `--sqlEnableNull` is enabled. `--createMandatoryChecks` is only justified where mandatory semantics cannot be represented by a simple column-level `NOT NULL`, for example because of inheritance, smart mapping, references or roles that are represented by several nullable columns.

The review must therefore:

1. generate a fresh Mabillon schema **without** `createMandatoryChecks` and without `sqlEnableNull`;
2. enumerate every INTERLIS `MANDATORY` attribute/reference/role in the Mabillon model;
3. verify for each one whether the generated PostgreSQL schema already enforces the required mandatory semantics through `NOT NULL`, FK structure or another generated constraint;
4. identify any concrete Mabillon model element whose mandatory semantics would be lost without `createMandatoryChecks`;
5. add focused positive/negative database tests for every such exceptional case;
6. compare the result with a schema generated with `createMandatoryChecks` and document every additional CHECK constraint it creates;
7. specifically verify that optional references and `{0..1}` roles remain nullable;
8. repeat the check with the selected ili2pg 5.5.1 Java-API baseline, because the current workaround was observed with ili2pg 5.5.2.

Decision rule:

- **If no Mabillon mandatory rule depends on `createMandatoryChecks`: remove it.** Delete `SchemaConstraintRepair.java` and the corresponding post-schema-import repair step. This is the preferred solution because it removes both the unnecessary option and the workaround.
- **If Mabillon genuinely needs `createMandatoryChecks`: keep it only with a minimal reproducer and automated tests proving both the required mandatory constraints and the absence of false mandatory checks on optional references.** If ili2pg 5.5.1 still creates false checks, retain the repair only as a narrowly documented compatibility workaround and prepare an upstream reproducer/bug report.

Do **not** use `sqlEnableNull` as the workaround: it intentionally suppresses ordinary SQL `NOT NULL` constraints and changes the database validation strategy globally.

### Dependency convergence gate

Before the Java-API migration is accepted:

1. run `./gradlew dependencies` for the relevant runtime/test configurations;
2. run `dependencyInsight` for the shared INTERLIS libraries;
3. verify there is exactly one intentionally selected version of every shared INTERLIS component on the runtime classpath;
4. add explicit constraints/resolution rules only where required and document why;
5. fail tests/build on accidental incompatible version drift where practical;
6. record the resolved INTERLIS dependency set in the Phase-11 report.

Do not use broad `force` rules without documenting the dependency that requires them.

### Java-API regression tests

The Java-API migration must prove at least:

- ili2c model compilation/validation works in-process;
- valid XTF is accepted by ilivalidator in-process;
- invalid XTF is rejected before ili2pg import;
- catalog, master-data and business-data imports work through the Java API;
- `importTid=true` and `importBid=true` semantics remain preserved;
- export produces an XTF that ilivalidator accepts;
- post-import ili2pg validation still works;
- diagnostics are captured as structured Mabillon results/exceptions without relying on subprocess stdout;
- Spring Boot import/export works in the production-like container without external tool JAR paths.

The full semantic export -> fresh DB -> import roundtrip remains a separate hard gate and must run through these Java-API adapters.

## Revised Phase-11 work order

1. **11.1 Security default-deny** – protect every fachliche route by default.
2. **11.2 Identity & audit** – deterministic authenticated-principal to fachlicher Benutzer mapping; remove silent person fallback.
3. **11.3 DB/storage consistency** – implement the specified staging/DB/storage ordering and tested compensation.
4. **11.4 Test isolation** – remove shared mutable persistent state between test methods.
5. **11.5 INTERLIS Java-API integration** – replace ProcessBuilder runtime adapters, move dependencies to `jars.interlis.ch`, use ili2pg 5.5.1, prove dependency convergence, review whether `createMandatoryChecks` is required, remove `SchemaConstraintRepair` if it is not, and update the binding specification.
6. **11.6 Missing UI use cases** – complete the reachable web flows, especially participants, tasks, catalog/master-data and registraturplan administration.
7. **11.7 Unterlage lifecycle** – metadata update, assign/unassign, finalize, register as aktenrelevant, cancel and transition rules.
8. **11.8 Business rules / data quality** – DQ-007, participation date validation, duplicate warning and remaining rule gaps.
9. **11.9 Search correctness** – valid result routes and explicit type-specific filtering.
10. **11.10 Validation/error model** – structured field/domain errors rather than generic failures.
11. **11.11 INTERLIS semantic roundtrip** – Java API export -> validation -> fresh PostgreSQL -> Java API import -> semantic graph comparison.
12. **11.12 Real Playwright golden path** – actual Playwright Java E2E test against PostgreSQL and the real application.
13. **11.13 DB-side search/pagination** – move relevant full scans/filter/sort/page operations into Cayenne/PostgreSQL.
14. **11.14 Dev/prod security separation** – no production startup with development credentials.
15. **11.15 Final specification verification** – rerun the closure matrix and allow no unresolved mandatory requirement.

## Phase-11 final gate additions

In addition to the gates in `PHASE_11_SPEC_MATRIX.md`, Phase 11 is not complete until:

- Spring Boot uses ili2pg, ilivalidator and ili2c via Java APIs, not `ProcessBuilder`;
- ili2pg is aligned to 5.5.1 as the selected compatibility baseline;
- Gradle resolves a reviewed, coherent set of shared INTERLIS libraries;
- the necessity of `createMandatoryChecks` has been demonstrated from the actual Mabillon model rather than assumed;
- if `createMandatoryChecks` is unnecessary, `SchemaConstraintRepair.java` and its repair step have been removed;
- if `createMandatoryChecks` remains necessary, automated tests prove required mandatory semantics and correct nullability of optional references/roles;
- the application runtime/container requires no external INTERLIS tool JAR installation;
- import/export/model-validation regression tests execute through the Java APIs;
- the semantic INTERLIS roundtrip executes through the Java APIs;
- `MABILLON_IMPLEMENTATION_SPEC.md` no longer mandates the superseded ProcessBuilder/external-JAR design.
