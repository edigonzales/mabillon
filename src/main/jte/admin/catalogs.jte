@param String title
@param String active
@param org.springframework.security.web.csrf.CsrfToken csrfToken
@param java.util.List<guru.interlis.mabillon.catalog.CatalogGroupView> groups
@param java.util.List<guru.interlis.mabillon.catalog.CatalogEntryView> geschaeftsarten
@template.layout(
    title = title,
    active = active,
    content = @`
        <nav class="mabillon-breadcrumbs" aria-label="Brotkrümel"><a href="/admin">Administration</a> / Kataloge</nav>
        <div class="mabillon-page-header">
            <div>
                <p class="mabillon-eyebrow">Konfiguration</p>
                <h1>${title}</h1>
                <p class="mabillon-page-subtitle">Aktive Werte sind für neue Geschäftsdaten auswählbar. Historische Werte bleiben erhalten.</p>
            </div>
        </div>
        @for (var group : groups)
            <section class="mabillon-tile mabillon-admin-section">
                <div class="mabillon-section-heading">
                    <h2>${group.type().label()}</h2>
                    <span class="mabillon-count-badge">${group.entries().size()}</span>
                </div>
                <ul class="mabillon-record-list">
                    @for (var entry : group.entries())
                        <li class="mabillon-record-row">
                            <span>
                                <strong>${entry.name()}</strong>
                                <span><code>${entry.code()}</code> · ${entry.status()}</span>
                            </span>
                            <span class="mabillon-page-actions">
                                <a class="mabillon-button mabillon-button--secondary" href="/admin/kataloge/${group.type().name().toLowerCase()}/${entry.code()}/bearbeiten">Bearbeiten</a>
                                @if (entry.active())
                                    <form method="post" action="/admin/kataloge/${group.type().name().toLowerCase()}/${entry.code()}/deactivate">
                                        <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                                        <button class="mabillon-button mabillon-button--secondary" type="submit">Deaktivieren</button>
                                    </form>
                                @else
                                    <form method="post" action="/admin/kataloge/${group.type().name().toLowerCase()}/${entry.code()}/activate">
                                        <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                                        <button class="mabillon-button mabillon-button--secondary" type="submit">Aktivieren</button>
                                    </form>
                                @endif
                            </span>
                        </li>
                    @endfor
                </ul>
                <form class="mabillon-form" method="post" action="/admin/kataloge/${group.type().name().toLowerCase()}">
                    <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                    <h3>${group.type().label()} ergänzen</h3>
                    <label>Code <input name="code" required></label>
                    <label>Name <input name="name" required></label>
                    <label>Beschreibung <textarea name="description"></textarea></label>
                    @if (group.type().name().equals("GESCHAEFTSART"))
                        <label class="mabillon-checkbox"><input type="checkbox" name="resultatErforderlich"> Resultat erforderlich</label>
                    @endif
                    @if (group.type().name().equals("PROZESSSTATUS"))
                        <label>Geschäftsart
                            <select name="geschaeftsartCode" required>
                                @for (var art : geschaeftsarten)
                                    <option value="${art.code()}">${art.name()} (${art.code()})</option>
                                @endfor
                            </select>
                        </label>
                        <label>Sortierung <input type="number" name="sortierung"></label>
                        <label class="mabillon-checkbox"><input type="checkbox" name="initial"> Initialstatus</label>
                        <label class="mabillon-checkbox"><input type="checkbox" name="terminal"> Terminalstatus</label>
                    @endif
                    @if (group.type().name().equals("RESULTATSTATUS"))
                        <label>Geschäftsart
                            <select name="geschaeftsartCode" required>
                                @for (var art : geschaeftsarten)
                                    <option value="${art.code()}">${art.name()} (${art.code()})</option>
                                @endfor
                            </select>
                        </label>
                        <label>Sortierung <input type="number" name="sortierung"></label>
                        <label class="mabillon-checkbox"><input type="checkbox" name="terminal"> Terminalstatus</label>
                    @endif
                    <button class="mabillon-button" type="submit">Katalogwert anlegen</button>
                </form>
            </section>
        @endfor
    `
)
