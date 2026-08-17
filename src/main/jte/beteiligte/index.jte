@param String title
@param String active
@param String name
@param String typ
@param String externeReferenz
@param guru.interlis.mabillon.query.SearchPage<guru.interlis.mabillon.beteiligung.BeteiligterView> result
@template.layout(
    title = title,
    active = active,
    content = @`
        <p class="breadcrumb">Beteiligte</p>
        <div class="page-header">
            <div>
                <p class="eyebrow">Geschäftskontakte</p>
                <h1>${title}</h1>
                <p class="lead">Personen und Organisationen erfassen und für Geschäftsbeteiligungen verwenden.</p>
            </div>
            <a class="button" href="/beteiligte/neu">Beteiligten erfassen</a>
        </div>
        <section class="workspace-card">
            <form class="filter-bar" method="get" action="/beteiligte">
                <label>Name <input name="name" value="${name == null ? "" : name}"></label>
                <label>Typ
                    <select name="typ">
                        <option value="">Alle</option>
                        <option value="Person" selected="${"Person".equals(typ)}">Person</option>
                        <option value="Organisation" selected="${"Organisation".equals(typ)}">Organisation</option>
                        <option value="Interne_Organisationseinheit" selected="${"Interne_Organisationseinheit".equals(typ)}">Interne Organisationseinheit</option>
                    </select>
                </label>
                <label>Externe Referenz <input name="externeReferenz" value="${externeReferenz == null ? "" : externeReferenz}"></label>
                <button class="button button--quiet" type="submit">Filtern</button>
            </form>
            <div class="section-heading"><h2>Treffer</h2><span class="count-badge">${result.totalElements()}</span></div>
            <ul class="record-list">
                @for (var value : result.items())
                    <li>
                        <a href="/beteiligte/${value.tid()}">
                            <strong>${value.name()}</strong>
                            <span>${value.typ()}@if (value.organisation() != null) · ${value.organisation()}@endif</span>
                        </a>
                    </li>
                @endfor
            </ul>
            <div class="pagination-bar">
                @if (result.page() > 0)
                    <a class="button button--quiet" href="/beteiligte?page=${result.page() - 1}&name=${name == null ? "" : name}&typ=${typ == null ? "" : typ}&externeReferenz=${externeReferenz == null ? "" : externeReferenz}">Zurück</a>
                @endif
                <span>Seite ${result.page() + 1}</span>
                @if (result.hasNext())
                    <a class="button button--quiet" href="/beteiligte?page=${result.page() + 1}&name=${name == null ? "" : name}&typ=${typ == null ? "" : typ}&externeReferenz=${externeReferenz == null ? "" : externeReferenz}">Weiter</a>
                @endif
            </div>
        </section>
    `
)
