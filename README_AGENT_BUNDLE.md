# Mabillon coding-agent bundle

**Mabillon – Einfache und transparente Geschäftsverwaltung**

Start here:

1. Read `AGENTS.md`.
2. Read `MABILLON_IMPLEMENTATION_SPEC.md` for the approved phase and affected use cases.
3. Use the project-local skills in `.agents/skills` as focused workflow guides.
4. Do not start a later phase without explicit user approval.

## Local prerequisites assumed by the specification

- Java 25
- Spring Boot 4.1.0 project to be created in Phase 0/1
- PostgreSQL / Testcontainers
- Apache Cayenne 5.0-M2 and Cayenne Modeler MCP
- ili2pg 5.5.2 at `/Users/stefan/apps/ili2pg-5.5.2/ili2pg-5.5.2.jar`
- ili2c 5.6.8 at `/Users/stefan/apps/ili2c-5.6.8/ili2c.jar`
- ilivalidator 1.15.0 at `/Users/stefan/apps/ilivalidator-1.15.0/ilivalidator-1.15.0.jar`

For schema generation use `--createTidCol --createBasketCol`; for normal XTF imports use `--importTid --importBid`.

The `model/` folder contains the current INTERLIS starting model and the Nomenklatur test fixtures. Phase 0 may change the model only as specified and must update these fixtures accordingly.

## Skills portability

The canonical skills live in `.agents/skills/<name>/SKILL.md`. Keep this one copy in source control. Both Codex and OpenCode are expected to discover that project-local path; `AGENTS.md` also lists the skills and their intended triggers.


## Naming

Gradle group: `guru.interlis`; Java base package: `guru.interlis.mabillon`; artifact/repository: `mabillon`. The bundled INTERLIS model remains `SO_AGI_GEVER_20260707` until an explicit model migration is approved.


## Local INTERLIS tools

The specification assumes these local defaults:

- ili2pg 5.5.2: `/Users/stefan/apps/ili2pg-5.5.2/ili2pg-5.5.2.jar`
- ili2c 5.6.8: `/Users/stefan/apps/ili2c-5.6.8/ili2c.jar`
- ilivalidator 1.15.0: `/Users/stefan/apps/ilivalidator-1.15.0/ilivalidator-1.15.0.jar`

All positive `.ili`/`.xtf` artifacts must pass the corresponding validator before a phase gate can be marked successful.
