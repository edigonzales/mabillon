# Phase 11.11 – INTERLIS Semantic Roundtrip

**Status:** SUCCESS  
**Date:** 2026-08-17  
**Scope:** Java-API DB → XTF → fresh PostgreSQL → XTF semantic roundtrip

## Goal

The Phase-11 hard gate requires more than proving that ili2pg can independently import and export files. Data exported from Mabillon must survive a complete transfer into a fresh database without losing INTERLIS identities, baskets, references or fachliche attribute values.

The comparison must be semantic. PostgreSQL/ili2pg technical IDs such as `t_id` are implementation details and are allowed to differ in the fresh database; XML header metadata and document ordering are not a fachlicher identity contract either.

## Implemented test

`InterlisSemanticRoundtripIntegrationTest` starts two independent `sogis/postgis:16-3.5` Testcontainers databases.

### Source database

1. create the Mabillon schema through `JavaApiIli2pgRunner`;
2. import the existing Golden-Path catalog, master-data and business-data XTFs;
3. export all three topics through the ili2pg Java API;
4. validate every exported XTF through the in-process ilivalidator Java API.

### Fresh target database

1. create only the schema through the same Java API adapter;
2. import the source exports in dependency order: Kataloge → Stammdaten → Geschäftsdaten;
3. use `importTid=true` and `importBid=true` for every import;
4. execute ili2pg database validation after every topic import;
5. re-export every topic through the same Java API;
6. validate every re-export through ilivalidator.

No ProcessBuilder, external INTERLIS installation or second toolchain is involved.

## Semantic graph comparison

The test reads only the XTF `DATASECTION` and builds a deterministic semantic graph. It compares:

- topic/basket identity including **BID**;
- object class and **TID**;
- all fachliche attribute values;
- association/reference attributes including **REF**;
- nested INTERLIS elements.

XML namespace declaration noise and insignificant formatting whitespace are ignored. Basket/object ordering is removed by keying the graph by semantic identity. The full source graph must equal the graph produced by the fresh target database.

The Golden-Path business topic also has explicit non-vacuous assertions for:

- business-data BID `ada09d02-2110-5e46-afa6-ea7426d960bc`;
- dossier TID `dd811d7e-1890-5254-9cd9-0a93bb5635a8`;
- business TID `8e2db417-33f6-5818-b052-2b0c91c48f49`;
- a REF to participant TID `016af2e9-9dc3-5a2d-b032-81fb7353eb0d`.

These assertions ensure the equality check cannot accidentally pass while ignoring the exact identity/reference semantics that Phase 11.11 is intended to protect.

## CI evidence

- Run #152 on commit `f90dc2113ef5af7020f92d6687cc308c00d209e0` completed successfully with the complete two-database semantic roundtrip.
- The follow-up commit `884985c36a5dfd41b9f1ad7051bf2eea85098e3c` adds explicit BID/TID/REF assertions on top of the already-green graph comparison.

## Decision

Phase 11.11 is complete once the final branch-head CI gate remains green. The semantic roundtrip is now a permanent automated regression gate for all three Mabillon INTERLIS topics and runs entirely through the reviewed Java-API toolchain introduced in Phase 11.5.

This closes the semantic-roundtrip gap for UC-034, UC-035 and UC-036.

**Next:** Phase 11.12 – real Playwright Java golden path.
