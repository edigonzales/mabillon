---
name: mabillon-ui-design
description: Design or implement Mabillon UI/CSS/JTE components. Use for any visual/layout work. Applies the pinned ili2grails design language: calm neutral public-sector UI, semantic components, small radii, subtle shadows, forms/tables/filter patterns, and no Bootstrap/Tailwind dependency unless explicitly approved.
compatibility: Codex and OpenCode
metadata:
  project: mabillon
  category: ui
---

# Mabillon UI design language

## Normative reference

Use `edigonzales/ili2grails` at commit:

`3e133a976a0ed1c704f38e81a6493501e0568ec4`

Primary reference file:

`target-grails/src/main/resources/grails/overlays/ui-assets/stylesheets/ili-modern.css`

Visual references:

- `mockups/01-application-shell-dashboard.png`
- `mockups/02-domain-list-search-filter.png`
- `mockups/03-object-detail-workspace.png`
- `mockups/04-domain-edit-form.png`
- `mockups/05-multi-domain-workspace.png`

Read `references/design-reference.md` before adding foundational CSS/components.

## Adopt the design language, not its framework coupling

ili2grails currently expresses parts of the design via Bootstrap variables/components. Mabillon should reproduce the useful visual/component vocabulary in Vanilla CSS unless Bootstrap is explicitly approved.

No Tailwind/utility-class soup. No external CSS/font CDN.

## Visual principles

- calm, neutral, information-dense public-administration UI,
- white/small-contrast surfaces with clear borders,
- very subtle shadows only,
- small radii (reference is approximately 3px),
- clear typographic hierarchy, not oversized dashboard marketing UI,
- consistent spacing and alignment,
- restrained single accent color as a token,
- visible focus states,
- avoid decorative rounded pills except compact statuses/tags where useful.

## Component vocabulary

Build/reuse semantic components for:

- topbar/app shell/sidebar/navigation,
- breadcrumbs and page header/actions,
- form sections/field errors/validation summary/sticky form actions,
- list tools/search/quick filters/active filters,
- tables/sort/row actions/pagination,
- notifications/notices,
- dossier/case summary,
- status history, task list and journal/timeline.

Prefer `ili-*` for generic design-system primitives and `mabillon-*` for domain-specific patterns.

## Typography

The ili2grails reference uses Fira Sans. Use it only if the project intentionally provides a licensed local font asset. Otherwise use the agreed system sans stack. Never fetch fonts from an external CDN.

## Review checklist

Compare new foundational UI against the pinned mockups. Verify desktop and narrow viewport, keyboard focus, empty/error/loading states, long German labels, table overflow, and full-page vs HTMX fragment consistency.
