# Phase 11.12 – Real Playwright Golden Path

**Status:** SUCCESS  
**Date:** 2026-08-17  
**Scope:** complete Nomenklatur Golden Path through a real Chromium browser

## Goal

The binding specification requires a real Playwright Java end-to-end test for the complete Nomenklatur Golden Path. A MockMvc test or a browser smoke test is not sufficient: the test must exercise the running Spring Boot application, real HTML/JTE forms, Spring Security/CSRF, PostgreSQL persistence and document storage as one user-visible workflow.

## Test architecture

`PlaywrightGoldenPathE2ETest` uses:

- Playwright Java 1.61.0;
- headless Chromium;
- `@SpringBootTest(webEnvironment = RANDOM_PORT)` and the real embedded Tomcat server;
- a dedicated `sogis/postgis:16-3.5` Testcontainers database;
- the normal Cayenne persistence layer;
- the real filesystem `DocumentStorage` under `build/playwright/storage`;
- the existing INTERLIS Golden-Path catalog/master-data/business-data fixture as initial reference data;
- HTTP Basic authentication with the application user `admin`, which maps deterministically to fachlicher Benutzer `anna.mueller`;
- normal browser form submission including the application's CSRF fields/cookie handling.

The fachliche workflow contains no controller/service shortcuts. After fixture setup, every business mutation is initiated by Chromium through the application's real HTML UI.

The test is intentionally one JUnit method. The Golden Path is one ordered business transaction and therefore cannot accidentally depend on ordering between separate test methods.

## Executed Golden Path

The browser performs the complete binding-spec workflow:

1. authenticates and opens the Mabillon dashboard;
2. creates a new dossier at registratur position `4.3.2`, federated by `AGI-NOM`, responsible user `anna.mueller`;
3. creates a `NOMENKLATURMUTATION` business inside that dossier;
4. assigns existing participant **Gemeinde Musterwil** as `ANTRAGSTELLERIN`;
5. uploads and registers a real PDF-like file as document type `ANTRAG`, then assigns it to the business;
6. moves the process to `FACHLICHE_PRUEFUNG`;
7. creates an `AUFGABE_FACHLICHE_PRUEFUNG` task assigned to `anna.mueller` and completes it through the UI;
8. uploads/registers the `KOMMISSIONSBESCHLUSS` and assigns it to the business;
9. records result `GENEHMIGT`;
10. moves the process to terminal status `ABGESCHLOSSEN` and closes the business;
11. closes the dossier;
12. reloads the business/dossier detail pages and verifies the expected lifecycle and journal/audit entries, including attribution to `anna.mueller`.

The explicit move to process status `ABGESCHLOSSEN` before business closure is required by the implemented business rule: a business may only be closed when its process state is terminal. This preserves the binding-spec intent while exercising the actual invariant rather than bypassing it.

## Assertions

The browser test verifies, among other things:

- the authenticated dashboard renders real application content;
- generated dossier/business numbers are returned by the real redirects;
- the new dossier/business detail pages render correctly;
- Gemeinde Musterwil is visibly assigned as Antragstellerin;
- both uploaded documents reach `Registriert` and are visibly linked to the new business;
- process status and result updates are visible after the real form submissions;
- the created task becomes `Erledigt`;
- business lifecycle becomes `Abgeschlossen`;
- dossier lifecycle becomes `Geschlossen`;
- business journal contains `Erstellt`, `Status_geaendert`, `Entscheid_erfasst` and `Geschaeft_abgeschlossen`;
- dossier journal contains `Dossier_abgeschlossen`;
- audit output identifies `anna.mueller`.

On browser failure a full-page screenshot is written to `build/playwright/golden-path-failure.png`; the CI failure-artifact configuration includes `build/playwright/` together with the Gradle reports.

## CI integration

The normal GitHub Actions `test` job now:

1. sets up Java 25 and Gradle;
2. executes `./gradlew playwrightInstall --no-daemon` to install Chromium and its Linux dependencies;
3. executes the unchanged main verification command `./gradlew test --no-daemon`;
4. uploads browser screenshots together with Gradle reports on failure.

Only Chromium is part of the acceptance gate. During the test process CI sets `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1`, because the preceding explicit install step already provides the required browser and downloading unused Firefox/WebKit binaries would add time without increasing Mabillon's acceptance coverage.

For a fresh local workstation the equivalent preparation is:

```text
./gradlew playwrightInstall
./gradlew test --tests guru.interlis.mabillon.PlaywrightGoldenPathE2ETest
```

## CI evidence

- Run #158 proved the explicit Playwright/Chromium installation step and the existing test suite work on the GitHub-hosted Ubuntu runner.
- Run #159 was the first real browser execution and failed only because the initial test asserted the non-visible `aria-label` text `Meine Arbeit`; the failure screenshot/report proved Chromium and the authenticated running application were already functioning.
- Run #160 on commit `4b3d6b21998d17c9b9399b708cf5423bc2bba4ea` completed successfully with the complete real-browser Golden Path and the full Gradle test suite.
- Commit `2b7694ad8a96b5b36e8d54085baeed03b68314dc` only prevents Playwright from downloading unused Firefox/WebKit binaries during the CI test process after Chromium has already been installed explicitly.

## Decision

Phase 11.12 is complete. The hard requirement for a **real Playwright Java Golden Path** is now a permanent part of the normal CI suite and exercises the complete Nomenklatur workflow against PostgreSQL, the running Spring Boot application and real document storage.

**Next:** Phase 11.13 – DB-side search and pagination.
