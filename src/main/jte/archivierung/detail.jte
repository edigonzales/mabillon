@param String title
@param String active
@param org.springframework.security.web.csrf.CsrfToken csrfToken
@param guru.interlis.mabillon.archivierung.ArchivAblieferungView delivery
@template.layout(title = title, active = active, content = @`
    <p class="breadcrumb"><a href="/archivierung">Archivierung</a> / ${delivery.deliveryNumber()}</p>
    <div class="page-header"><div><p class="eyebrow">Archivablieferung</p><h1>${delivery.title()}</h1><p class="lead">${delivery.deliveryNumber()} · ${delivery.status()} · ${delivery.archiveRecipient()}</p></div></div>
    <section class="workspace-card">
        <div class="section-heading"><h2>Dossiers</h2><span class="count-badge">${delivery.dossiers().size()}</span></div>
        <form method="post" action="/archivierung/${delivery.deliveryNumber()}/dossiers" class="inline-form">
            <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
            <label>Dossiernummer <input name="dossierNumber" placeholder="AGI-D-2026-000007" required></label>
            <button class="button" type="submit">Hinzufügen</button>
        </form>
        <div class="record-list">
            @for (var dossier : delivery.dossiers())
                <div class="admin-record"><span><strong>${dossier.dossierNumber()} · ${dossier.title()}</strong><span>Dossier: ${dossier.dossierStatus()} · Archivierung: ${dossier.archiveStatus()}</span></span></div>
            @endfor
        </div>
    </section>
    <section class="workspace-card">
        <div class="section-heading"><h2>Ablauf</h2></div>
        <div class="inline-actions">
            <form method="post" action="/archivierung/${delivery.deliveryNumber()}/bereit"><input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}"><button class="button" type="submit">Bereit markieren</button></form>
            <form method="post" action="/archivierung/${delivery.deliveryNumber()}/sip-erzeugen"><input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}"><button class="button" type="submit">SIP erzeugen</button></form>
            <form method="post" action="/archivierung/${delivery.deliveryNumber()}/sip-validieren"><input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}"><button class="button" type="submit">SIP validieren</button></form>
        </div>
        <div class="record-list">
            @for (var sip : delivery.sipPackages())
                <div class="admin-record"><span><strong>SIP Versuch ${sip.attempt()} · ${sip.validationStatus()}</strong><span>${sip.status()} · ${sip.size()} Bytes · ${sip.sha256()}</span></span></div>
            @endfor
        </div>
    </section>
`)
