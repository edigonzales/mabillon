---
name: mabillon-archive-sip
description: Work on Mabillon Aussonderung, ArchivAblieferung, SIP generation/validation, archive acceptance and archive-related data-quality rules.
compatibility: Codex and OpenCode
metadata:
  project: mabillon
  category: archive
---

# Archive and SIP rules

Read:

- `docs/domain/archival-delivery.md`
- `docs/interfaces/archive/ech-0160-1.3.0/PROFILE.md`
- `docs/domain/data-quality.md`

Keep Dossier archival state, ArchivAblieferung and individual SIP generation attempts distinct. A failed/corrected package must not erase prior audit history.

SIP is a standards-driven structured package, not a generic ZIP export. Eligibility, files, hashes and profile validation must succeed before a delivery is treated as valid. Journal important state transitions and fail closed on missing fachlicher actor attribution.
