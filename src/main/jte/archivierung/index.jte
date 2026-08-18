@param String title
@param String active
@param org.springframework.security.web.csrf.CsrfToken csrfToken
@param guru.interlis.mabillon.query.SearchPage<guru.interlis.mabillon.archivierung.AussonderungView> eligible
@template.layout(title = title, active = active, content = @`
    <nav class="mabillon-breadcrumbs" aria-label="Brotkrümel"><a href="/">Übersicht</a> / Archivierung</nav>
    <div class="mabillon-page-header">
        <div><p class="mabillon-eyebrow">Archivierung</p><h1>${title}</h1><p class="mabillon-page-subtitle">Geschlossene und qualitätsgeprüfte Dossiers für die Aussonderung.</p></div>
    </div>
    <section class="mabillon-tile">
        <div class="mabillon-section-heading"><h2>Neue Archivablieferung</h2></div>
        <form method="post" action="/archivierung/ablieferungen" class="mabillon-form-grid">
            <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
            <label>Organisation <input name="organisationCode" value="AGI" required></label>
            <label>Titel <input name="title" required></label>
            <label>Archivempfänger <input name="archivempfaenger" required></label>
            <label>Bemerkung <input name="bemerkung"></label>
            <button class="mabillon-button" type="submit">Ablieferung anlegen</button>
        </form>
    </section>
    <section class="mabillon-tile">
        <div class="mabillon-section-heading"><h2>Aussonderung</h2><span class="mabillon-count-badge">${eligible.totalElements()}</span></div>
        @if (eligible.items().isEmpty())
            <p class="mabillon-empty-state">Keine geeigneten Dossiers.</p>
        @else
            <div class="mabillon-record-list">
                @for (var dossier : eligible.items())
                    <div class="mabillon-record-row"><span><strong>${dossier.dossierNumber()} · ${dossier.title()}</strong><span>${dossier.status()} · geschlossen ${dossier.closedAt()} · ${dossier.documentCount()} Unterlagen · ${dossier.warningCount()} Warnungen</span></span></div>
                @endfor
            </div>
        @endif
    </section>
`)
