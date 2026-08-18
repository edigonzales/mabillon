---
name: mabillon-interlis-ili2pg
description: Change or verify the Mabillon INTERLIS model, ili2c/ilivalidator/ili2pg Java-API integration, XTF import/export, schema generation, baskets and transfer identities.
compatibility: Codex and OpenCode
metadata:
  project: mabillon
  category: interlis
---

# INTERLIS workflow

Read:

- `docs/interfaces/interlis.md`
- `docs/development/interlis-model-workflow.md`
- ADR 0001 under `docs/architecture/`

The persistent fachliche schema starts in `model/SO_AGI_GEVER_20260707.ili`. Never introduce a fachliche table/column in Flyway or Cayenne first.

Use the Gradle-resolved in-process Java APIs. Do not add local JAR paths, `java -jar` subprocesses or a second set of tool versions.

Normal XTF import validates first and preserves TID/BID. Export validates before successful delivery. Model changes require fresh schema verification plus Cayenne DB Import/cgen and integration tests.
