@param String title
@param String active
@param java.util.List<guru.interlis.mabillon.stammdaten.OrganisationseinheitView> organisationseinheiten
@param java.util.List<guru.interlis.mabillon.stammdaten.BenutzerView> benutzer
@param org.springframework.security.web.csrf.CsrfToken csrfToken
@template.layout(
    title = title,
    active = active,
    content = @`
        <p class="breadcrumb"><a href="/admin">Administration</a> / Stammdaten</p>
        <div class="page-header">
            <div>
                <p class="eyebrow">Organisation</p>
                <h1>${title}</h1>
                <p class="lead">Verantwortlichkeiten und fachliche Benutzerstammdaten.</p>
            </div>
        </div>
        <div class="admin-grid">
            <section class="workspace-card admin-section">
                <div class="section-heading"><h2>Organisationseinheiten</h2><span class="count-badge">${organisationseinheiten.size()}</span></div>
                <ul class="record-list">
                    @for (var organisationseinheit : organisationseinheiten)
                        <li class="admin-record">
                            <span><strong>${organisationseinheit.name()}</strong><span><code>${organisationseinheit.kuerzel()}</code> · ${organisationseinheit.status()}</span></span>
                            <details>
                                <summary>Bearbeiten</summary>
                                <form class="admin-form" method="post" action="/admin/stammdaten/organisationseinheiten/${organisationseinheit.kuerzel()}">
                                    <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                                    <label>Name <input name="name" required value="${organisationseinheit.name()}"></label>
                                    <label>Beschreibung <textarea name="beschreibung">${organisationseinheit.beschreibung() == null ? "" : organisationseinheit.beschreibung()}</textarea></label>
                                    <label>Übergeordnete Einheit <input name="uebergeordneteEinheit" value="${organisationseinheit.uebergeordneteEinheit() == null ? "" : organisationseinheit.uebergeordneteEinheit()}"></label>
                                    <button class="button button--quiet" type="submit">Speichern</button>
                                </form>
                            </details>
                            @if (organisationseinheit.active())
                                <form method="post" action="/admin/stammdaten/organisationseinheiten/${organisationseinheit.kuerzel()}/deactivate">
                                    <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                                    <button class="button button--quiet" type="submit">Deaktivieren</button>
                                </form>
                            @endif
                        </li>
                    @endfor
                </ul>
                <form class="admin-form" method="post" action="/admin/stammdaten/organisationseinheiten">
                    <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                    <h3>Organisationseinheit ergänzen</h3>
                    <label>Kürzel <input name="kuerzel" required></label>
                    <label>Name <input name="name" required></label>
                    <label>Beschreibung <textarea name="beschreibung"></textarea></label>
                    <label>Übergeordnete Einheit <input name="uebergeordneteEinheit"></label>
                    <button class="button" type="submit">Anlegen</button>
                </form>
            </section>
            <section class="workspace-card admin-section">
                <div class="section-heading"><h2>Benutzer</h2><span class="count-badge">${benutzer.size()}</span></div>
                <ul class="record-list">
                    @for (var user : benutzer)
                        <li class="admin-record">
                            <span><strong>${user.name()}</strong><span><code>${user.username()}</code> · ${user.organisationseinheit()} · ${user.status()}</span></span>
                            <details>
                                <summary>Bearbeiten</summary>
                                <form class="admin-form" method="post" action="/admin/stammdaten/benutzer/${user.username()}">
                                    <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                                    <label>Name <input name="name" required value="${user.name()}"></label>
                                    <label>E-Mail <input type="email" name="email" value="${user.email() == null ? "" : user.email()}"></label>
                                    <label>Organisationseinheit <input name="organisationseinheit" required value="${user.organisationseinheit()}"></label>
                                    <button class="button button--quiet" type="submit">Speichern</button>
                                </form>
                            </details>
                            @if (user.active())
                                <form method="post" action="/admin/stammdaten/benutzer/${user.username()}/deactivate">
                                    <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                                    <button class="button button--quiet" type="submit">Deaktivieren</button>
                                </form>
                            @endif
                        </li>
                    @endfor
                </ul>
                <form class="admin-form" method="post" action="/admin/stammdaten/benutzer">
                    <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                    <h3>Benutzer ergänzen</h3>
                    <label>Username <input name="username" required></label>
                    <label>Name <input name="name" required></label>
                    <label>E-Mail <input type="email" name="email"></label>
                    <label>Organisationseinheit <input name="organisationseinheit" required></label>
                    <button class="button" type="submit">Anlegen</button>
                </form>
            </section>
        </div>
    `
)
