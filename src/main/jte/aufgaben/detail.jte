@param String title
@param String active
@param guru.interlis.mabillon.aufgabe.AufgabeView aufgabe
@param java.util.List<guru.interlis.mabillon.stammdaten.BenutzerView> benutzer
@param java.util.List<guru.interlis.mabillon.stammdaten.OrganisationseinheitView> organisationseinheiten
@param org.springframework.security.web.csrf.CsrfToken csrfToken
@template.layout(
    title = title,
    active = active,
    content = @`
        <nav class="mabillon-breadcrumbs" aria-label="Brotkrümel"><a href="/aufgaben">Meine Aufgaben</a> / ${aufgabe.title()}</nav>
        <div class="mabillon-page-header">
            <div>
                <p class="mabillon-eyebrow">${aufgabe.typName() == null ? "Aufgabe" : aufgabe.typName()}</p>
                <h1>${aufgabe.title()}</h1>
                <p class="mabillon-page-subtitle"><a href="/geschaefte/${aufgabe.geschaeftsnummer()}">${aufgabe.geschaeftsnummer()}</a> · ${aufgabe.status()}</p>
            </div>
        </div>
        <div class="mabillon-admin-grid">
            <section class="mabillon-tile mabillon-admin-section">
                <div class="mabillon-section-heading"><h2>Aufgabe bearbeiten</h2></div>
                @if (aufgabe.open())
                    <form class="mabillon-form" method="post" action="/aufgaben/${aufgabe.tid()}">
                        <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                        <label>Titel <input name="title" required value="${aufgabe.title()}"></label>
                        <label>Beschreibung <textarea name="description">${aufgabe.description() == null ? "" : aufgabe.description()}</textarea></label>
                        <label>Fällig am <input type="date" name="dueDate" value="${aufgabe.dueDate() == null ? "" : aufgabe.dueDate().toString()}"></label>
                        <label>Priorität <input type="number" min="0" max="5" name="priority" value="${aufgabe.priority() == null ? "0" : aufgabe.priority().toString()}"></label>
                        <button class="mabillon-button" type="submit">Speichern</button>
                    </form>
                @else
                    <p class="mabillon-muted">Abgeschlossene Aufgaben können nicht mehr bearbeitet werden.</p>
                @endif
            </section>
            <section class="mabillon-tile mabillon-admin-section">
                <div class="mabillon-section-heading"><h2>Zuweisung</h2></div>
                <p>Benutzer: <strong>${aufgabe.assignedUsername() == null ? "–" : aufgabe.assignedUsername()}</strong><br>Organisationseinheit: <strong>${aufgabe.assignedOrganisationseinheit() == null ? "–" : aufgabe.assignedOrganisationseinheit()}</strong></p>
                @if (aufgabe.open())
                    <form class="mabillon-form" method="post" action="/aufgaben/${aufgabe.tid()}/delegate">
                        <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                        <label>An Benutzer
                            <select name="username" required>
                                <option value="">Auswählen</option>
                                @for (var user : benutzer)
                                    <option value="${user.username()}">${user.name()} (${user.username()})</option>
                                @endfor
                            </select>
                        </label>
                        <button class="mabillon-button mabillon-button--secondary" type="submit">An Benutzer delegieren</button>
                    </form>
                    <form class="mabillon-form" method="post" action="/aufgaben/${aufgabe.tid()}/delegate">
                        <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                        <label>An Organisationseinheit
                            <select name="organisationseinheit" required>
                                <option value="">Auswählen</option>
                                @for (var organisationseinheit : organisationseinheiten)
                                    <option value="${organisationseinheit.kuerzel()}">${organisationseinheit.name()} (${organisationseinheit.kuerzel()})</option>
                                @endfor
                            </select>
                        </label>
                        <button class="mabillon-button mabillon-button--secondary" type="submit">An Organisationseinheit delegieren</button>
                    </form>
                @endif
            </section>
        </div>
    `
)
