---
name: mabillon-domain-model
description: Apply Mabillon GEVER concepts, lifecycles, business invariants, numbering, journal and data-quality rules.
compatibility: Codex and OpenCode
metadata:
  project: mabillon
  category: domain
---

# Mabillon domain rules

Canonical sources:

- `docs/domain/concepts.md`
- `docs/domain/lifecycle-and-rules.md`
- `docs/domain/documents.md`
- `docs/domain/data-quality.md`
- `docs/product/use-cases.md`

Read the affected sections before changing domain behavior.

Hard invariants include Dossier/Geschäft/Unterlage consistency, status-to-Geschäftsart consistency, explicit closure rules, three separate identity levels, and atomic journal attribution.

Do not silently repair inconsistent domain state in a normal write use case unless the documented use case explicitly defines that behavior.
