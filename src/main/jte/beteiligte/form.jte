@param String title
@param String active
@param guru.interlis.mabillon.beteiligung.BeteiligterView beteiligter
@param org.springframework.security.web.csrf.CsrfToken csrfToken
@template.layout(
    title = title,
    active = active,
    content = @`
        <p class="breadcrumb"><a href="/beteiligte">Beteiligte</a> / ${beteiligter == null ? "Neu" : "Bearbeiten"}</p>
        <div class="page-header"><div><p class="eyebrow">Geschäftskontakt</p><h1>${title}</h1></div></div>
        <section class="workspace-card">
            <form class="admin-form" method="post" action="${beteiligter == null ? "/beteiligte" : "/beteiligte/" + beteiligter.tid()}">
                <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                <label>Typ
                    <select name="typ" required>
                        @if (beteiligter != null && "Person".equals(beteiligter.typ()))
                            <option value="Person" selected>Person</option>
                        @else
                            <option value="Person">Person</option>
                        @endif
                        @if (beteiligter != null && "Organisation".equals(beteiligter.typ()))
                            <option value="Organisation" selected>Organisation</option>
                        @else
                            <option value="Organisation">Organisation</option>
                        @endif
                        @if (beteiligter != null && "Interne_Organisationseinheit".equals(beteiligter.typ()))
                            <option value="Interne_Organisationseinheit" selected>Interne Organisationseinheit</option>
                        @else
                            <option value="Interne_Organisationseinheit">Interne Organisationseinheit</option>
                        @endif
                    </select>
                </label>
                <label>Name <input name="name" required value="${beteiligter == null ? "" : beteiligter.name()}"></label>
                <label>Vorname <input name="vorname" value="${beteiligter == null || beteiligter.vorname() == null ? "" : beteiligter.vorname()}"></label>
                <label>Organisation <input name="organisation" value="${beteiligter == null || beteiligter.organisation() == null ? "" : beteiligter.organisation()}"></label>
                <label>E-Mail <input type="email" name="email" value="${beteiligter == null || beteiligter.email() == null ? "" : beteiligter.email()}"></label>
                <label>Telefon <input name="telefon" value="${beteiligter == null || beteiligter.telefon() == null ? "" : beteiligter.telefon()}"></label>
                <label>Adresse <textarea name="adresse">${beteiligter == null || beteiligter.adresse() == null ? "" : beteiligter.adresse()}</textarea></label>
                <label>Externe Referenz <input name="externeReferenz" value="${beteiligter == null || beteiligter.externeReferenz() == null ? "" : beteiligter.externeReferenz()}"></label>
                <div class="page-actions">
                    <button class="button" type="submit">Speichern</button>
                    <a class="button button--quiet" href="${beteiligter == null ? "/beteiligte" : "/beteiligte/" + beteiligter.tid()}">Abbrechen</a>
                </div>
            </form>
        </section>
    `
)
