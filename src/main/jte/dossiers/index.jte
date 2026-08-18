@param String title
@param guru.interlis.mabillon.query.SearchPage<guru.interlis.mabillon.dossier.DossierView> searchPage
@param guru.interlis.mabillon.dossier.DossierSearchCriteria criteria
@param java.util.List<guru.interlis.mabillon.registraturplan.RegistraturplanPositionView> positions
@param java.util.List<guru.interlis.mabillon.stammdaten.OrganisationseinheitView> organisationseinheiten
@param String[] statuses
@param org.springframework.security.web.csrf.CsrfToken csrfToken
@template.layout(
    title = title,
    active = "dossiers",
    content = @`
        <div class="mabillon-page-header">
            <div><nav class="mabillon-breadcrumbs" aria-label="Brotkrümel">Arbeitsbereich</nav><h1>Dossiers</h1><p class="mabillon-page-subtitle">Aktenmässige Einheiten suchen und öffnen.</p></div>
            <a class="mabillon-button" href="/dossiers/neu">Neues Dossier</a>
        </div>
        <section class="mabillon-tile mabillon-list-tools">
            <form method="get" action="/dossiers" class="mabillon-filter-grid">
                <label>Nummer<input name="number" value="${criteria.number() == null ? "" : criteria.number()}"></label>
                <label>Titel<input name="title" value="${criteria.title() == null ? "" : criteria.title()}"></label>
                <label>Status<select name="status"><option value="">Alle</option>@for (var value : statuses)<option value="${value}">${value}</option>@endfor</select></label>
                <label>Federführung<select name="federation"><option value="">Alle</option>@for (var value : organisationseinheiten)<option value="${value.kuerzel()}">${value.kuerzel()} · ${value.name()}</option>@endfor</select></label>
                <label>Position<select name="position"><option value="">Alle</option>@for (var value : positions)<option value="${value.code()}">${value.code()} · ${value.titel()}</option>@endfor</select></label>
                <label>Eröffnet ab<input type="date" name="openedFrom" value="${criteria.openedFrom() == null ? "" : criteria.openedFrom().toString()}"></label>
                <label>Eröffnet bis<input type="date" name="openedTo" value="${criteria.openedTo() == null ? "" : criteria.openedTo().toString()}"></label>
                <button class="mabillon-button" type="submit">Suchen</button>
            </form>
        </section>
        @template.dossiers._list(searchPage = searchPage)
    `
)
