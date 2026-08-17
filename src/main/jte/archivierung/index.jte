@param String title
@param String active
@param org.springframework.security.web.csrf.CsrfToken csrfToken
@param guru.interlis.mabillon.query.SearchPage<guru.interlis.mabillon.archivierung.AussonderungView> eligible
@template.layout(title = title, active = active, content = @`
    <p class="breadcrumb"><a href="/">Übersicht</a> / Archivierung</p>
    <div class="page-header">
        <div><p class="eyebrow">Archivierung</p><h1>${title}</h1><p class="lead">Geschlossene und qualitätsgeprüfte Dossiers für die Aussonderung.</p></div>
    </div>
    <section class="workspace-card">
        <div class="section-heading"><h2>Neue Archivablieferung</h2></div>
        <form method="post" action="/archivierung/ablieferungen" class="form-grid">
            <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
            <label>Organisation <input name="organisationCode" value="AGI" required></label>
            <label>Titel <input name="title" required></label>
            <label>Archivempfänger <input name="archivempfaenger" required></label>
            <label>Bemerkung <input name="bemerkung"></label>
            <button class="button" type="submit">Ablieferung anlegen</button>
        </form>
    </section>
    <section class="workspace-card">
        <div class="section-heading"><h2>Aussonderung</h2><span class="count-badge">${eligible.totalElements()}</span></div>
        @if (eligible.items().isEmpty())
            <p class="empty-state">Keine geeigneten Dossiers.</p>
        @else
            <div class="record-list">
                @for (var dossier : eligible.items())
                    <div class="admin-record"><span><strong>${dossier.dossierNumber()} · ${dossier.title()}</strong><span>${dossier.status()} · geschlossen ${dossier.closedAt()} · ${dossier.documentCount()} Unterlagen · ${dossier.warningCount()} Warnungen</span></span></div>
                @endfor
            </div>
        @endif
    </section>
`)
