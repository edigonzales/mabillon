# ili2grails design reference for Mabillon

Pinned repository: `edigonzales/ili2grails`
Pinned commit: `3e133a976a0ed1c704f38e81a6493501e0568ec4`
Primary stylesheet: `target-grails/src/main/resources/grails/overlays/ui-assets/stylesheets/ili-modern.css`

Inspect these mockups before establishing foundational Mabillon components:

1. `mockups/01-application-shell-dashboard.png`
2. `mockups/02-domain-list-search-filter.png`
3. `mockups/03-object-detail-workspace.png`
4. `mockups/04-domain-edit-form.png`
5. `mockups/05-multi-domain-workspace.png`

Design characteristics to preserve, expressed without copying implementation:

- neutral text/emphasis/muted hierarchy, white surface, light canvas/header/hover surfaces,
- clear subtle borders and tiny card elevation,
- approximately 3px component radii,
- content grid capped around 1440px,
- app shell/top bar and stable local navigation,
- breadcrumbs + page header + action group,
- structured form sections, validation summary, field help and sticky actions,
- dedicated search/filter tool area and visible active filters,
- semantic responsive tables, compact row actions and pagination,
- restrained notification styling.

Mabillon may brand the primary accent through a token. Do not hardcode component colors independently.

The reference's Bootstrap integration is not a requirement for Mabillon. Recreate only the needed semantics in Vanilla CSS.
