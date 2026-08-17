@param String title
@param guru.interlis.mabillon.search.GlobalSearchCriteria criteria
@param guru.interlis.mabillon.search.GlobalSearchResult result
@template.layout(
    title = title,
    active = "search",
    content = @`
        <div class="page-header"><div><p class="breadcrumb">Arbeitsbereich</p><h1>Systemweite Suche</h1><p class="lead">Geschäfte, Dossiers, Beteiligte, Unterlagen und Fachsystemreferenzen strukturiert durchsuchen.</p></div></div>
        <section class="workspace-card filter-card">
            <form method="get" action="/suche" class="filter-grid">
                <label>Freitext<input name="q" value="${criteria.text() == null ? "" : criteria.text()}"></label>
                <label>Geschäftsnummer<input name="geschaeftsnummer" value="${criteria.geschaeftsnummer() == null ? "" : criteria.geschaeftsnummer()}"></label>
                <label>Dossiernummer<input name="dossiernummer" value="${criteria.dossiernummer() == null ? "" : criteria.dossiernummer()}"></label>
                <label>Titel<input name="titel" value="${criteria.titel() == null ? "" : criteria.titel()}"></label>
                <label>Beteiligter<input name="beteiligterName" value="${criteria.beteiligterName() == null ? "" : criteria.beteiligterName()}"></label>
                <label>Organisation<input name="organisation" value="${criteria.organisation() == null ? "" : criteria.organisation()}"></label>
                <label>Geschäftsart<input name="geschaeftsartCode" value="${criteria.geschaeftsartCode() == null ? "" : criteria.geschaeftsartCode()}"></label>
                <label>Prozessstatus<input name="processStatusCode" value="${criteria.processStatusCode() == null ? "" : criteria.processStatusCode()}"></label>
                <label>Unterlagentitel<input name="unterlagentitel" value="${criteria.unterlagentitel() == null ? "" : criteria.unterlagentitel()}"></label>
                <label>Fachsystem-ID<input name="fachsystemId" value="${criteria.fachsystemId() == null ? "" : criteria.fachsystemId()}"></label>
                <button class="button" type="submit">Suchen</button>
            </form>
        </section>
        @template.suche._results(result = result)
    `
)
