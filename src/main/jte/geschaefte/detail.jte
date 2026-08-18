@param guru.interlis.mabillon.geschaeft.GeschaeftView geschaeft
@param java.util.List<guru.interlis.mabillon.catalog.CatalogEntryView> processStatuses
@param java.util.List<guru.interlis.mabillon.catalog.CatalogEntryView> resultStatuses
@param java.util.List<guru.interlis.mabillon.catalog.CatalogEntryView> aufgabentypen
@param java.util.List<guru.interlis.mabillon.beteiligung.BeteiligungView> beteiligungen
@param java.util.List<guru.interlis.mabillon.beteiligung.BeteiligterView> beteiligte
@param java.util.List<guru.interlis.mabillon.aufgabe.AufgabeView> aufgaben
@param java.util.List<guru.interlis.mabillon.fachsystem.FachsystemReferenzView> fachsystemReferenzen
@param java.util.List<guru.interlis.mabillon.journal.JournalEntryView> journalEntries
@param org.springframework.security.web.csrf.CsrfToken csrfToken
@template.layout(
    title = geschaeft.title(),
    active = "geschaefte",
    content = @`
        <nav class="mabillon-breadcrumbs" aria-label="Brotkrümel"><a href="/">Übersicht</a> / Geschäfte</nav>
        @template.geschaefte._detail(geschaeft = geschaeft, processStatuses = processStatuses, resultStatuses = resultStatuses, aufgabentypen = aufgabentypen, beteiligungen = beteiligungen, beteiligte = beteiligte, aufgaben = aufgaben, fachsystemReferenzen = fachsystemReferenzen, journalEntries = journalEntries, csrfToken = csrfToken)
    `
)
