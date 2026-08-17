# ADR 0004: Mabillon visual language

## Status

Accepted for Phase 0.

## Context

The product needs a calm, dense, and recognizable public-sector interface. The
normative visual reference is `edigonzales/ili2grails` at commit
`3e133a976a0ed1c704f38e81a6493501e0568ec4`, especially its `ili-modern.css`
and five mockups. Mabillon may reproduce the design language without taking a
Bootstrap dependency.

## Decision

Use semantic vanilla CSS tokens and components with:

- quiet neutral canvas and panel surfaces, clear 1px borders, and subtle
  shadows;
- small radii (reference: 3px), restrained spacing, and readable typography;
- an app shell with topbar, navigation, breadcrumbs, page header, and content;
- consistent form sections, validation messages, sticky form actions, filters,
  tables, row actions, pagination, and status/hint components.

The primary brand color is a Mabillon token, while density, structure,
neutral palette, and interaction patterns follow the pinned reference.

## Reference

- [Pinned `ili-modern.css`](https://raw.githubusercontent.com/edigonzales/ili2grails/3e133a976a0ed1c704f38e81a6493501e0568ec4/target-grails/src/main/resources/grails/overlays/ui-assets/stylesheets/ili-modern.css)
- [Pinned mockups directory](https://github.com/edigonzales/ili2grails/tree/3e133a976a0ed1c704f38e81a6493501e0568ec4/mockups)

## Consequences

- UI work is reviewed against the pinned reference and this ADR.
- Components remain application-owned and can be rendered by JTE without a
  client-side component framework.
- Any later visual departure requires a new ADR or an explicit specification
  update.
