---
name: mabillon-interlis-ili2pg
description: Work on the Mabillon INTERLIS 2.4 model, ili2c validation, ilivalidator XTF validation, ili2pg-generated PostgreSQL schema, XTF import/export, baskets, TIDs/BIDs, or schema evolution. Use whenever persistent domain structure or INTERLIS exchange changes. Enforces the fixed local toolchain and model-first pipeline.
compatibility: Codex and OpenCode
metadata:
  project: mabillon
  category: interlis
---

# INTERLIS and ili2pg workflow

## Source of truth

The persistent domain schema starts in:

`model/SO_AGI_GEVER_20260707.ili`

Never add a fachliche DB column/table directly to Flyway or Cayenne first. Change INTERLIS, compile it, generate a fresh PostgreSQL reference schema, then update Cayenne DB-first mapping.

Technical application-only tables may live in `mabillon_app` only when the specification/ADR permits them.

## Fixed local tools

Defaults:

- ili2pg 5.5.2: `/Users/stefan/apps/ili2pg-5.5.2/ili2pg-5.5.2.jar`
- ili2c 5.6.8: `/Users/stefan/apps/ili2c-5.6.8/ili2c.jar`
- ilivalidator 1.15.0: `/Users/stefan/apps/ilivalidator-1.15.0/ilivalidator-1.15.0.jar`

Allow `ILI2PG_JAR`, `ILI2C_JAR` and `ILIVALIDATOR_JAR` as explicit overrides, but retain the paths above as defaults. Before Phase 1, prove that all three JARs exist and are callable with the expected versions.

Mandatory order for model changes: `.ili` → ili2c → ili2pg schemaimport → Cayenne DB Import/cgen.

Mandatory order for XTF input: XTF → ilivalidator → ili2pg import. Invalid XTF must never reach the database import.

Mandatory order for XTF output: ili2pg export → ilivalidator → expose/accept export.

## Mandatory schema options

Schema creation must include at least:

- `--schemaimport`
- `--dbschema mabillon`
- `--createFk`
- `--createFkIdx`
- `--createUnique`
- `--createMandatoryChecks`
- `--createNumChecks`
- `--createTextChecks`
- `--createDateTimeChecks`
- `--createMetaInfo`
- `--createTidCol`
- `--createBasketCol`

The correct spelling is `--createBasketCol`.

Centralize these options in `scripts/create-schema.sh`; CI calls the same script/task instead of duplicating the command.

## Mandatory XTF import options

Every normal Mabillon XTF import uses:

- `--importTid`
- `--importBid`

This applies to Kataloge, Stammdaten and Geschaeftsdaten. Do not omit these flags unless a separately approved use case explicitly requires identity remapping.

Import order:

1. Kataloge
2. Stammdaten
3. Geschaeftsdaten

Read `references/ili2pg-commands.md` before changing scripts.

## Required checks after a model change

1. INTERLIS compile/validation succeeds with ili2c 5.6.8.
2. Positive test XTFs validate with ilivalidator 1.15.0.
3. Fresh PostgreSQL schema creation succeeds.
4. Expected FK, unique and mandatory/check constraints exist.
5. `t_ili_tid` and `t_basket` are present where expected.
6. Test import with `--importTid --importBid` preserves known transfer and basket IDs.
7. Cayenne DB Import produces explainable relationship changes.
8. cgen compiles.
9. Invalid-XTF negative test proves validation blocks import.
10. Exported XTF validates with ilivalidator.
11. Integration tests pass.

## Schema evolution

For new/test DBs: always recreate from `.ili` using ili2pg.

For existing production DBs: derive/review an explicit migration from old generated schema to new generated schema. Flyway is a migration mechanism, not source of truth.
