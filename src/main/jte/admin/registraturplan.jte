@param String title
@param String active
@param java.util.List<guru.interlis.mabillon.registraturplan.RegistraturplanView> plans
@param guru.interlis.mabillon.registraturplan.RegistraturplanTreeView tree
@param org.springframework.security.web.csrf.CsrfToken csrfToken
@template.layout(
    title = title,
    active = active,
    content = @`
        <nav class="mabillon-breadcrumbs" aria-label="Brotkrümel"><a href="/admin">Administration</a> / Registraturplan</nav>
        <div class="mabillon-page-header">
            <div>
                <p class="mabillon-eyebrow">Ordnungssystem</p>
                <h1>${title}</h1>
                <p class="mabillon-page-subtitle">Nur aktive Blattpositionen können für neue Dossiers verwendet werden.</p>
            </div>
        </div>
        <section class="mabillon-tile mabillon-admin-section">
            <div class="mabillon-section-heading"><h2>Pläne</h2><span class="mabillon-count-badge">${plans.size()}</span></div>
            <ul class="mabillon-record-list">
                @for (var plan : plans)
                    <li><a href="/admin/registraturplan?plan=${plan.code()}"><strong>${plan.name()}</strong><span><code>${plan.code()}</code> · ${plan.status()}</span></a></li>
                @endfor
            </ul>
            <form class="mabillon-form" method="post" action="/admin/registraturplan/plaene">
                <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                <h3>Registraturplan anlegen</h3>
                <label>Code <input name="code" required></label>
                <label>Name <input name="name" required></label>
                <label>Gültig von <input type="date" name="gueltigVon" required></label>
                <label>Organisationseinheit <input name="organisationseinheit" required></label>
                <button class="mabillon-button" type="submit">Plan anlegen</button>
            </form>
        </section>
        @if (tree != null)
            <section class="mabillon-tile mabillon-admin-section">
                <div class="mabillon-section-heading"><h2>${tree.plan().name()}</h2><span class="mabillon-status-badge">${tree.plan().status()}</span></div>
                <p class="mabillon-muted"><code>${tree.plan().code()}</code> · gültig ab ${tree.plan().gueltigVon().toString()}</p>
                <div class="mabillon-page-actions">
                    <form method="post" action="/admin/registraturplan/plaene/${tree.plan().code()}/activate">
                        <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                        <button class="mabillon-button mabillon-button--secondary" type="submit">Aktivieren</button>
                    </form>
                    <form method="post" action="/admin/registraturplan/plaene/${tree.plan().code()}/replace">
                        <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                        <label>Gültig bis <input type="date" name="gueltigBis" required></label>
                        <button class="mabillon-button mabillon-button--secondary" type="submit">Ersetzen</button>
                    </form>
                </div>
                <ul class="mabillon-tree">
                    @for (var position : tree.roots())
                        @template.admin._position(position = position, csrfToken = csrfToken)
                    @endfor
                </ul>
                <form class="mabillon-form" method="post" action="/admin/registraturplan/positionen">
                    <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                    <input type="hidden" name="planCode" value="${tree.plan().code()}">
                    <h3>Position anlegen</h3>
                    <label>Code <input name="code" required></label>
                    <label>Titel <input name="titel" required></label>
                    <label>Beschreibung <textarea name="beschreibung"></textarea></label>
                    <label>Oberposition <input name="oberposition"></label>
                    <label>Federführende Einheit <input name="federfuehrendeEinheit" required></label>
                    <button class="mabillon-button" type="submit">Position anlegen</button>
                </form>
            </section>
        @endif
    `
)
