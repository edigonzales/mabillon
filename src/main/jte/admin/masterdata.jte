@param String title
@param String active
@param java.util.List<guru.interlis.mabillon.stammdaten.OrganisationseinheitView> organisationseinheiten
@param java.util.List<guru.interlis.mabillon.stammdaten.BenutzerView> benutzer
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
                        </li>
                    @endfor
                </ul>
            </section>
            <section class="workspace-card admin-section">
                <div class="section-heading"><h2>Benutzer</h2><span class="count-badge">${benutzer.size()}</span></div>
                <ul class="record-list">
                    @for (var user : benutzer)
                        <li class="admin-record">
                            <span><strong>${user.name()}</strong><span><code>${user.username()}</code> · ${user.organisationseinheit()} · ${user.status()}</span></span>
                        </li>
                    @endfor
                </ul>
            </section>
        </div>
    `
)
