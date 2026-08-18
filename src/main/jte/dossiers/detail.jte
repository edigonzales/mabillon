@param guru.interlis.mabillon.dossier.DossierView dossier
@param java.util.List<guru.interlis.mabillon.catalog.CatalogEntryView> unterlagentypen
@param java.util.List<guru.interlis.mabillon.fachsystem.FachsystemReferenzView> fachsystemReferenzen
@param java.util.List<guru.interlis.mabillon.journal.JournalEntryView> journalEntries
@param org.springframework.security.web.csrf.CsrfToken csrfToken
@template.layout(
    title = dossier.title(),
    active = "dossiers",
    content = @`
        <nav class="mabillon-breadcrumbs" aria-label="Brotkrümel"><a href="/">Übersicht</a> / Dossiers</nav>
        @template.dossiers._detail(dossier = dossier, unterlagentypen = unterlagentypen, fachsystemReferenzen = fachsystemReferenzen, journalEntries = journalEntries, csrfToken = csrfToken)
    `
)
