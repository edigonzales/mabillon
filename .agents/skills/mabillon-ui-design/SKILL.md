---
name: mabillon-ui-design
description: Design or implement Mabillon UI/CSS/JTE components using the application's own calm, dense, accessible Vanilla-CSS design language.
compatibility: Codex and OpenCode
metadata:
  project: mabillon
  category: ui
---

# Mabillon UI design

Canonical reference: `docs/architecture/0005-ui-design-language.md`, the existing application templates/CSS and the pinned ili2grails commit `3e133a976a0ed1c704f38e81a6493501e0568ec4`.

Use the exact `balanced` palette (`#4299E1` primary, `#ECF5FC` active, `#3F4B55` ink, `#27333D` emphasis, `#5E6D79` muted, `#D3DDE5` border, `#FFFFFF` surface, `#F5F7F9` canvas, `#EDF2F5` header and `#E8F1F7` hover), local Fira Sans, 3px radii, the reference shadows and the 0.5/0.75/1/1.5-rem spacing rhythm. Preserve the 4rem topbar, 17rem desktop sidebar and 1440px content limit.

Use the single `mabillon-*` namespace for all application-owned generic and domain components, custom properties, technical states and DOM hooks. Keep standardized third-party contracts such as `hx-*`, `htmx-*` and `aria-*` unchanged. Do not introduce utility-class soup, Bootstrap runtime styles, alternative palettes, external font/icon/CSS CDNs or parallel legacy component variants without an explicit architecture decision.

Review desktop/narrow viewport, long German labels, keyboard focus, no-JS navigation, empty/error states, horizontal overflow and full-page/HTMX consistency.
