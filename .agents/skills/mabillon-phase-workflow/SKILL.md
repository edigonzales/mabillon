---
name: mabillon-phase-workflow
description: Enforce the Mabillon phased implementation workflow. Use for every implementation, refactoring, migration, or continuation task in this repository. Determines the currently approved phase, keeps work inside scope, runs the phase gate, writes PHASE_N_REPORT.md, and stops before the next phase.
compatibility: Codex and OpenCode
metadata:
  project: mabillon
  category: workflow
---

# Mabillon phase workflow

## Mandatory first reads

1. Locate the repository root.
2. Read `AGENTS.md` completely.
3. Read the relevant sections of `MABILLON_IMPLEMENTATION_SPEC.md`:
   - current phase,
   - affected use cases,
   - affected feature/module,
   - global Definition of Done.
4. Inspect existing `docs/phases/PHASE_*_REPORT.md` files and the user's current instruction to determine the **one** phase that is approved.

If no phase is explicitly approved, do not infer permission to start a later phase.

## Hard gate

Never implement work from phase N+1 while phase N is active. Do not "prepare" later-phase production code, schema, UI, or APIs unless the current specification explicitly requires a compatibility seam.

At the end of the active phase:

1. Run all targeted tests.
2. Run the complete phase gate/build.
3. Run INTERLIS/ili2pg/Cayenne consistency checks if the phase touches the model or persistence.
4. Review the final diff for accidental scope expansion and generated-code edits.
5. Write `docs/phases/PHASE_N_REPORT.md` using `references/phase-report-template.md`.
6. Mark `SUCCESS` only if every mandatory criterion is green.
7. Stop. Do not begin the next phase until the user explicitly authorizes it.

## Failure behavior

A failed mandatory test, unavailable required tool, unexplained Cayenne mapping diff, invalid INTERLIS model, or incomplete acceptance criterion makes the phase `FAILED` or `BLOCKED`, not "mostly successful".

Do not delete tests, weaken assertions, add `@Disabled`, substitute H2, or bypass model constraints to obtain a green build.

## Scope discipline

Before changing code, state internally:

- active phase,
- use case IDs,
- business invariant being implemented,
- files/classes expected to change,
- tests that will prove it.

Prefer the smallest coherent vertical change. Avoid unrelated cleanup/refactoring.
