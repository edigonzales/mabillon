@param String title
@param String active
@param guru.interlis.mabillon.beteiligung.CreateBeteiligterCommand command
@param java.util.List<guru.interlis.mabillon.beteiligung.BeteiligterView> duplicates
@param org.springframework.security.web.csrf.CsrfToken csrfToken
@template.layout(
    title = title,
    active = active,
    content = @`
        <p class="breadcrumb"><a href="/beteiligte">Beteiligte</a> / Neu / Duplikatprüfung</p>
        <div class="page-header">
            <div>
                <p class="eyebrow">Duplikatprüfung</p>
                <h1>Mögliches Duplikat</h1>
                <p>Es existieren bereits ähnliche Beteiligte. Bitte prüfen Sie diese Einträge vor dem Erfassen.</p>
            </div>
        </div>
        <section class="workspace-card">
            <ul class="record-list">
                @for (var duplicate : duplicates)
                    <li>
                        <a href="/beteiligte/${duplicate.tid().toString()}">
                            <strong>${duplicate.name()}${duplicate.vorname() == null ? "" : ", " + duplicate.vorname()}</strong>
                            <span>${duplicate.typ()}${duplicate.organisation() == null ? "" : " · " + duplicate.organisation()}${duplicate.email() == null ? "" : " · " + duplicate.email()}</span>
                        </a>
                    </li>
                @endfor
            </ul>
            <form method="post" action="/beteiligte" class="inline-form">
                <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                <input type="hidden" name="duplicateConfirmed" value="true">
                <input type="hidden" name="typ" value="${command.typ()}">
                <input type="hidden" name="name" value="${command.name()}">
                <input type="hidden" name="vorname" value="${command.vorname() == null ? "" : command.vorname()}">
                <input type="hidden" name="organisation" value="${command.organisation() == null ? "" : command.organisation()}">
                <input type="hidden" name="email" value="${command.email() == null ? "" : command.email()}">
                <input type="hidden" name="telefon" value="${command.telefon() == null ? "" : command.telefon()}">
                <input type="hidden" name="adresse" value="${command.adresse() == null ? "" : command.adresse()}">
                <input type="hidden" name="externeReferenz" value="${command.externeReferenz() == null ? "" : command.externeReferenz()}">
                <button class="button" type="submit">Trotzdem erfassen</button>
                <a class="button button--quiet" href="/beteiligte/neu">Zurück</a>
            </form>
        </section>
    `
)
