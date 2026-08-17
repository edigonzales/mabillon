# Phase 11.9 – Search Correctness

**Status:** SUCCESS  
**Date:** 2026-08-17  
**Scope:** explicit type-specific filtering and valid global-search result targets

## Problem

`GlobalSearchService` previously routed every object type through one positional `matchesCommon(...)` method. Several parameters therefore had different meanings depending on the caller. Examples:

- a dossier number was passed as the generic `businessNumber` for dossier hits;
- a Fachsystem reference `systemCode` was passed in the process-status position;
- a Fachsystem object ID was passed in the generic title position.

This made structured filters capable of matching unrelated fields even though the result URLs themselves had already been introduced in Phase 11.6/11.7.

## Implemented

`GlobalSearchService` now has explicit matching per searchable object type:

- `matchesDossier`
- `matchesBusiness`
- `matchesParty`
- `matchesDocument`
- `matchesReference`

Every structured criterion is mapped only to fields that have that declared meaning. Related-object matching is explicit rather than positional: for example a dossier may match a business-number criterion through an actually related business, and a document may match its assigned business/dossier numbers.

The Fachsystem-reference result target is chosen explicitly: business when present, otherwise dossier. References without either context are not exposed as navigable global-search hits.

`GlobalSearchCriteria` now trims string values and converts blank browser fields to `null`, so a submitted HTML form with empty optional inputs has the same semantics as a direct service call with absent criteria.

The implementation deliberately keeps the existing in-memory loading/filtering/pagination architecture. Moving these operations to Cayenne/PostgreSQL is owned by Phase 11.13 and is not mixed into this correctness change.

## Automated evidence

`GlobalSearchCorrectnessIntegrationTest` runs against the real PostgreSQL/PostGIS Golden-Path fixture and verifies:

- a dossier number supplied as `geschaeftsnummer` no longer matches a dossier accidentally;
- a real business number explicitly matches the business and its related dossier/documents/references;
- Fachsystem `systemCode=NOMENKLATUR` is not treated as a process status;
- Fachsystem object ID is not treated as a generic title;
- `fachsystemId` finds the expected Fachsystem reference;
- participant and dossier-context filters traverse only explicit relationships;
- blank HTTP form inputs do not suppress a valid free-text query;
- every distinct URL returned for the Golden-Path search resolves through MockMvc to a renderable page;
- whitespace/empty criteria normalization is deterministic.

The target-route test exposed a real latent UI defect in `beteiligte/detail.jte`: the participant UUID was rendered directly and JTE could not compile that expression. The detail view now renders the UUID via `toString()`.

## CI gate

- Run #117 proved the type-specific production search implementation remained compatible with the existing full suite.
- Run #120 on commit `e6618087d816b5152d29ff70b1ffa109a3dde033` completed successfully with the new `GlobalSearchCorrectnessIntegrationTest`, including all returned-target route checks.

## Decision

Phase 11.9 is complete. Search correctness is separated from search performance: semantic matching and navigability are closed here; DB-side filtering/pagination remains Phase 11.13.

**Next:** Phase 11.10 – Validation / error model.
