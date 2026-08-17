---
name: mabillon-domain-model
description: Apply the Mabillon GEVER domain model and business invariants. Use when changing Dossier, Geschaeft, Unterlage, Aufgabe, Beteiligung, Registraturplan, catalog/master data, numbering, journal/audit, closing rules, search semantics, or data-quality rules.
compatibility: Codex and OpenCode
metadata:
  project: mabillon
  category: domain
---

# Mabillon domain rules

Read the affected use cases and domain sections in `MABILLON_IMPLEMENTATION_SPEC.md` before changing model or application logic.

## Core semantics

- **Registraturplan** is the UI/business term for INTERLIS `Ordnungssystem` + `OrdnungssystemPosition`.
- **Dossier** is the Akte / records container and is classified at one Registraturplan position.
- **Geschaeft** is a business/process Vorgang and belongs to exactly one Dossier.
- **Unterlage** is an Aktenstück. It belongs to exactly one Dossier and may have zero or one Geschaeft as business context.
- If an Unterlage has a Geschaeft context, that Geschaeft **must belong to the same Dossier**.
- **Aufgabe** belongs to a Geschaeft and models work, not archival classification.
- **Beteiligter** answers "who?"; **Beteiligung** answers "who has which role in this Geschaeft?".
- Geschaeftsart, ProzessStatus, ResultatStatus, BeteiligungsRolle, UnterlagenTyp, AufgabenTyp are catalog/configuration data, not transaction data.
- Organisationseinheit, Benutzer and Registraturplan are master data.

## Status separation

Never collapse these concepts:

- lifecycle status: generic lifecycle,
- process status: specific to Geschaeftsart,
- result status: business outcome, optional/required according to Geschaeftsart,
- task status: state of an Aufgabe.

A ProzessStatus or ResultatStatus must be valid for the Geschaeft's Geschaeftsart.

## Identity

Keep three identity levels separate:

1. DB key (`t_id`) — internal relational key.
2. INTERLIS transfer identity (`t_ili_tid`) — stable object/transfer identity.
3. Human business number — e.g. `AGI-G-2026-000421`, `AGI-D-2026-000007`.

Never derive authorization or domain identity from `t_id`. Never reuse a human business number.

## Journal/audit

Every relevant write use case must atomically create the required journal event in the same unit of work. Normal users cannot edit/delete journal events.

## Closing

A Geschaeft or Dossier may close only if its specified closure rules are satisfied. Dossier closure must reject contained open Geschaefte. Do not silently auto-fix inconsistent data during closure.

## Golden path

Preserve and use the fixture:

- Gemeinde Musterwil,
- rename `Im alten Boden` -> `Bodenrain`,
- Dossier `AGI-D-2026-000007`,
- Geschaeft `AGI-G-2026-000421`,
- Geschaeftsart Nomenklaturmutation.

Use it in integration and E2E tests across phases.
