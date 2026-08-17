---
name: mabillon-archive-sip
description: Work on Mabillon Aussonderung, ArchivAblieferung, SIP generation/validation, archive acceptance, or archive-related data-quality rules. Use primarily in Phase 9 and for earlier model changes needed to preserve archiveability. Treat SIP as a standards-driven archival package, not a ZIP export.
compatibility: Codex and OpenCode
metadata:
  project: mabillon
  category: archive
---

# Archive and SIP rules

## Scope and phase

Do not implement Phase-9 SIP behavior early. Earlier phases may only add explicitly specified model seams/invariants required so later archival delivery remains possible.

Before implementing SIP, verify the **current target archive profile/version** and its validation requirements. Record that decision in a versioned ADR/test fixture. Do not hardcode an assumed old profile from memory.

## Domain model

Keep separate concepts:

- Dossier archival state,
- ArchivAblieferung: groups a set of eligible dossiers into one delivery,
- SipPaket / generation attempt: one concrete generated/validated package attempt,
- archive acceptance/rejection and archive signature/reference.

A delivery may have more than one SIP generation attempt. Do not overwrite failed attempts in a way that destroys audit history.

## Eligibility

An archive-ready delivery must reject dossiers that violate mandatory closure/data-quality rules. Validation errors must prevent "ready for delivery" status.

## SIP generation

Generation is an application/backend concern independent of JTE/HTMX. It must be deterministic from a recorded selection/configuration where the target profile allows it, and must capture sufficient metadata to reproduce/audit what was delivered.

Never define "SIP" as merely zipping exported files.

## Validation and audit

Store/report:

- target profile/version,
- generation timestamp,
- selected dossiers/counts,
- validation result/errors/warnings,
- package checksum/size where applicable,
- delivery/acceptance state,
- archive reference/signature after acceptance.

Journal important state transitions.

## Tests

Use valid and intentionally invalid package fixtures. Test: eligibility rejection, generation, validation failure, corrected regeneration, successful handoff state, and preservation of prior attempts.
