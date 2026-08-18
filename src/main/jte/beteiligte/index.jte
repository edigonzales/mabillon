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
        <nav class="mabillon-breadcrumbs" aria-label="Brotkrümel">Beteiligte</nav>
        <div class="mabillon-page-header">
            <div>
                <p class="mabillon-eyebrow">Geschäftskontakte</p>
                <h1>${title}</h1>
                <p class="mabillon-page-subtitle">Personen und Organisationen erfassen und für Geschäftsbeteiligungen verwenden.</p>
            </div>
            <a class="mabillon-button" href="/beteiligte/neu">Beteiligten erfassen</a>
        </div>
        <section class="mabillon-tile">
            <form class="mabillon-filter-bar" method="get" action="/beteiligte">
                <label>Name <input name="name" value="${name == null ? "" : name}"></label>
                <label>Typ
                    <select name="typ">
                        <option value="">Alle</option>
                        @if ("Person".equals(typ))
                            <option value="Person" selected>Person</option>
                        @else
                            <option value="Person">Person</option>
                        @endif
                        @if ("Organisation".equals(typ))
                            <option value="Organisation" selected>Organisation</option>
                        @else
                            <option value="Organisation">Organisation</option>
                        @endif
                        @if ("Interne_Organisationseinheit".equals(typ))
                            <option value="Interne_Organisationseinheit" selected>Interne Organisationseinheit</option>
                        @else
                            <option value="Interne_Organisationseinheit">Interne Organisationseinheit</option>
                        @endif
                    </select>
                </label>
                <label>Externe Referenz <input name="externeReferenz" value="${externeReferenz == null ? "" : externeReferenz}"></label>
                <button class="mabillon-button mabillon-button--secondary" type="submit">Filtern</button>
            </form>
            <div class="mabillon-section-heading"><h2>Treffer</h2><span class="mabillon-count-badge">${result.totalElements()}</span></div>
            <ul class="mabillon-record-list">
                @for (var value : result.items())
                    <li>
                        <a href="/beteiligte/${value.tid()}">
                            <strong>${value.name()}</strong>
                            <span>${value.typ()}@if (value.organisation() != null) · ${value.organisation()}@endif</span>
                        </a>
                    </li>
                @endfor
            </ul>
            <div class="mabillon-pagination-bar">
                @if (result.page() > 0)
                    <a class="mabillon-button mabillon-button--secondary" href="/beteiligte?page=${result.page() - 1}&name=${name == null ? "" : name}&typ=${typ == null ? "" : typ}&externeReferenz=${externeReferenz == null ? "" : externeReferenz}">Zurück</a>
                @endif
                <span>Seite ${result.page() + 1}</span>
                @if (result.hasNext())
                    <a class="mabillon-button mabillon-button--secondary" href="/beteiligte?page=${result.page() + 1}&name=${name == null ? "" : name}&typ=${typ == null ? "" : typ}&externeReferenz=${externeReferenz == null ? "" : externeReferenz}">Weiter</a>
                @endif
            </div>
        </section>
    `
)
