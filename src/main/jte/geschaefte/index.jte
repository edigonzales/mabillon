@param String title
@param guru.interlis.mabillon.query.SearchPage<guru.interlis.mabillon.geschaeft.GeschaeftView> searchPage
@param guru.interlis.mabillon.geschaeft.GeschaeftSearchCriteria criteria
@param java.util.List<guru.interlis.mabillon.catalog.CatalogEntryView> types
@param java.util.List<guru.interlis.mabillon.catalog.CatalogEntryView> processStatuses
@param java.util.List<guru.interlis.mabillon.stammdaten.OrganisationseinheitView> organisationseinheiten
@param java.util.List<guru.interlis.mabillon.stammdaten.BenutzerView> benutzer
@param String[] lifecycles
@param org.springframework.security.web.csrf.CsrfToken csrfToken
@template.layout(
    title = title,
    active = "geschaefte",
    content = @`
        <div class="page-header"><div><p class="breadcrumb">Arbeitsbereich</p><h1>Geschäfte</h1><p class="lead">Bearbeitete Vorgänge suchen und eröffnen.</p></div><a class="button" href="/geschaefte/neu">Neues Geschäft</a></div>
        <section class="workspace-card filter-card"><form method="get" action="/geschaefte" class="filter-grid">
            <label>Nummer<input name="number" value="${criteria.number() == null ? "" : criteria.number()}"></label>
            <label>Titel<input name="title" value="${criteria.title() == null ? "" : criteria.title()}"></label>
            <label>Geschäftsart<select name="type"><option value="">Alle</option>@for (var value : types)<option value="${value.code()}">${value.name()}</option>@endfor</select></label>
            <label>Prozessstatus<select name="processStatus"><option value="">Alle</option>@for (var value : processStatuses)<option value="${value.code()}">${value.name()}</option>@endfor</select></label>
            <label>Lifecycle<select name="lifecycle"><option value="">Alle</option>@for (var value : lifecycles)<option value="${value}">${value}</option>@endfor</select></label>
            <label>Verantwortlicher<select name="responsible"><option value="">Alle</option>@for (var value : benutzer)<option value="${value.username()}">${value.username()} · ${value.name()}</option>@endfor</select></label>
            <label>Fälligkeit ab<input type="date" name="dueFrom" value="${criteria.dueFrom() == null ? "" : criteria.dueFrom().toString()}"></label>
            <label>Fälligkeit bis<input type="date" name="dueTo" value="${criteria.dueTo() == null ? "" : criteria.dueTo().toString()}"></label>
            <button class="button" type="submit">Suchen</button>
        </form></section>
        @template.geschaefte._list(searchPage = searchPage)
    `
)
