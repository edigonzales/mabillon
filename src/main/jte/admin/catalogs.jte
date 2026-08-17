@param String title
@param String active
@param org.springframework.security.web.csrf.CsrfToken csrfToken
@param java.util.List<guru.interlis.mabillon.catalog.CatalogGroupView> groups
@template.layout(
    title = title,
    active = active,
    content = @`
        <p class="breadcrumb"><a href="/admin">Administration</a> / Kataloge</p>
        <div class="page-header">
            <div>
                <p class="eyebrow">Konfiguration</p>
                <h1>${title}</h1>
                <p class="lead">Aktive Werte sind für neue Geschäftsdaten auswählbar. Historische Werte bleiben erhalten.</p>
            </div>
        </div>
        @for (var group : groups)
            <section class="workspace-card admin-section">
                <div class="section-heading">
                    <h2>${group.type().label()}</h2>
                    <span class="count-badge">${group.entries().size()}</span>
                </div>
                <ul class="record-list">
                    @for (var entry : group.entries())
                        <li class="admin-record">
                            <span>
                                <strong>${entry.name()}</strong>
                                <span><code>${entry.code()}</code> · ${entry.status()}</span>
                            </span>
                            @if (entry.active())
                                <form method="post" action="/admin/kataloge/${group.type().name().toLowerCase()}/${entry.code()}/deactivate">
                                    <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                                    <button class="button button--quiet" type="submit">Deaktivieren</button>
                                </form>
                            @else
                                <form method="post" action="/admin/kataloge/${group.type().name().toLowerCase()}/${entry.code()}/activate">
                                    <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                                    <button class="button button--quiet" type="submit">Aktivieren</button>
                                </form>
                            @endif
                        </li>
                    @endfor
                </ul>
                @if (group.type().name().equals("GESCHAEFTSART"))
                    <form class="admin-form" method="post" action="/admin/kataloge/geschaeftsart">
                        <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                        <h3>Geschäftsart ergänzen</h3>
                        <label>Code <input name="code" required></label>
                        <label>Name <input name="name" required></label>
                        <label>Beschreibung <textarea name="description"></textarea></label>
                        <label class="checkbox"><input type="checkbox" name="resultatErforderlich"> Resultat erforderlich</label>
                        <button class="button" type="submit">Katalogwert anlegen</button>
                    </form>
                @endif
            </section>
        @endfor
    `
)
