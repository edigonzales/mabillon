---
name: mabillon-development-workflow
description: Implement, fix or refactor Mabillon while preserving current product, domain, architecture, security and test contracts.
compatibility: Codex and OpenCode
metadata:
  project: mabillon
  category: workflow
---

# Mabillon development workflow

## Start

1. Read `AGENTS.md`.
2. Read the relevant canonical documents under `docs/`.
3. Inspect the existing implementation and tests for the affected behavior.
4. Identify domain invariants, permissions, persistence and external-interface impact.

## Implement

Prefer the smallest coherent vertical change. Do not introduce speculative frameworks or abstractions. Reuse existing feature packages, services, error semantics and UI components.

For persistent domain changes, follow `docs/development/interlis-model-workflow.md`. For Cayenne work, also read `docs/development/cayenne.md`.

## Verify

Run targeted tests first, then the relevant broader suite. Do not weaken/delete tests to obtain green. Review generated diffs and documentation impact.

## Documentation

Update the canonical current-state document when behavior, architecture, interface or operation changes. Do not create implementation-progress reports; Git/PR history already records how the change was produced.
