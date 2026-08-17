# Phase 11.13 – DB-side search and pagination

**Status:** complete  
**Verification:** GitHub Actions Run #175 (`c5e745bb1d986483cc351429836661681f0521f5`)  
**Scope:** move relevant search/filter/sort/page work from Java collection processing into Cayenne/PostgreSQL without changing the domain model.

## Implemented query paths

The following query paths now execute their relevant filters, ordering, counts and limits in PostgreSQL through Cayenne:

- dossier search (`DossierQueryService`),
- business search plus dashboard business lists (`GeschaeftQueryService`),
- participant search and duplicate-candidate prefiltering (`BeteiligterService`),
- own/open task dashboard queries (`AufgabeQueryService`),
- business-control/open/overdue/inactivity queries and process-status aggregation (`GeschaeftskontrolleQueryService`),
- global search across participants, dossiers, business-system references, businesses and documents (`GlobalSearchService`).

List searches use `selectCount()` for totals and `offset()/limit()` for the requested page. Dashboard/control queries use database predicates and bounded result sets instead of materialising entire tables.

## Global search pagination

Global search keeps the type order defined by the existing result contract. Each object-type query is counted independently in PostgreSQL. The global page offset is then applied across these type blocks, and only the rows needed for the requested page are loaded.

Related-object filters use Cayenne subqueries on technical IDs rather than Java-side relationship traversal. This preserves the explicit type-specific semantics introduced in 11.9 while removing full-table materialisation.

## INTERLIS `LangerText` boundary

INTERLIS `LangerText` maps to effectively unbounded text attributes. Cayenne 5.0-M2 generates a `VARCHAR(2147483647)` cast for case-insensitive `contains` expressions on these attributes; PostgreSQL rejects `varchar(n)` lengths above 10,485,760.

Therefore the generic free-text search deliberately uses bounded identifying/search fields only. The following unbounded narrative fields are not part of the generic free-text predicate:

- dossier description,
- business short description,
- participant address,
- document remarks,
- business-system-reference description.

Structured fields, identifiers, titles, names, organisation, catalog codes/names, document titles, filenames/MIME types and business-system IDs remain database-searchable. No PostgreSQL-specific native SQL workaround was introduced merely to search five narrative fields.

## Regression evidence

`Phase11DatabaseSearchPaginationIntegrationTest` creates isolated PostgreSQL data and verifies:

- stable `totalElements`,
- page 0/page 1 boundaries for dossier, business and participant searches,
- global pagination across multiple object-type blocks.

The existing `GlobalSearchCorrectnessIntegrationTest` continues to verify the explicit 11.9 field semantics and navigable result URLs. Existing Phase-0 search/control coverage and the real Playwright Golden Path remain green.

## Verification history

Intermediate runs exposed two Cayenne-specific issues rather than application-domain defects:

1. deeply nested relationship expressions could bind incorrect types;
2. case-insensitive matching on unbounded `LangerText` produced an invalid PostgreSQL `VARCHAR(2147483647)` cast.

The final implementation uses Cayenne ID subqueries for related search fields and excludes unbounded narrative fields from generic free text.

**Run #175 is fully green**, including Java 25 setup, Playwright Chromium installation, the complete Gradle test suite, the new database pagination test and the existing browser E2E gate.

## Result

`X-PERF-01` is closed for the relevant search, dashboard and business-control paths. Archive-candidate selection is intentionally not forced through the same generic pagination pattern because its final candidate semantics depend on data-quality checks including storage/hash validation; pre-paging before those checks would change the business result.

Next Phase-11 block: **11.14 Dev/prod security separation**.
