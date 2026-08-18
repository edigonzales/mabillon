@param String title
@param String active
@param org.springframework.security.web.csrf.CsrfToken csrfToken
@param guru.interlis.mabillon.archivierung.ArchivAblieferungView delivery
@template.layout(title = title, active = active, content = @`
    <p class="breadcrumb"><a href="/archivierung">Archivierung</a> / ${delivery.deliveryNumber()}</p>
    <div class="page-header"><div><p class="eyebrow">Archivablieferung</p><h1>${delivery.title()}</h1><p class="lead">${delivery.deliveryNumber()} · ${delivery.status()} · ${delivery.archiveRecipient()}</p></div></div>
    <section class="workspace-card">
        <div class="section-heading"><h2>Dossiers</h2><span class="count-badge">${delivery.dossiers().size()}</span></div>
        @if (delivery.status().equals("Entwurf"))
            <form method="post" action="/archivierung/${delivery.deliveryNumber()}/dossiers" class="inline-form">
                <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                <label>Dossiernummer <input name="dossierNumber" placeholder="AGI-D-2026-000007" required></label>
                <button class="button" type="submit">Hinzufügen</button>
            </form>
        @endif
        <div class="record-list">
            @for (var dossier : delivery.dossiers())
                <div class="admin-record">
                    <span><strong>${dossier.dossierNumber()} · ${dossier.title()}</strong><span>Dossier: ${dossier.dossierStatus()} · Archivierung: ${dossier.archiveStatus()}</span></span>
                    @if (delivery.status().equals("Entwurf"))
                        <form method="post" action="/archivierung/${delivery.deliveryNumber()}/dossiers/${dossier.dossierNumber()}/entfernen">
                            <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                            <button class="button button-secondary" type="submit">Entfernen</button>
                        </form>
                    @endif
                </div>
            @endfor
        </div>
    </section>
    <section class="workspace-card">
        <div class="section-heading"><h2>Ablauf</h2></div>
        <div class="inline-actions">
            @if (delivery.status().equals("Entwurf") || delivery.status().equals("Abgelehnt"))
                <form method="post" action="/archivierung/${delivery.deliveryNumber()}/bereit">
                    <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                    <button class="button" type="submit">Bereit markieren</button>
                </form>
            @endif
            @if (delivery.status().equals("Bereit") || delivery.status().equals("Korrektur_erforderlich"))
                <form method="post" action="/archivierung/${delivery.deliveryNumber()}/sip-erzeugen">
                    <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                    <button class="button" type="submit">SIP erzeugen</button>
                </form>
            @endif
            @if (delivery.status().equals("SIP_Erstellt"))
                <form method="post" action="/archivierung/${delivery.deliveryNumber()}/sip-validieren">
                    <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                    <button class="button" type="submit">SIP validieren</button>
                </form>
            @endif
        </div>
        <div class="record-list">
            @for (var sip : delivery.sipPackages())
                <div class="admin-record"><span><strong>SIP Versuch ${sip.attempt()} · ${sip.validationStatus()}</strong><span>${sip.status()} · ${sip.size()} Bytes · ${sip.sha256()}</span></span></div>
            @endfor
        </div>
    </section>
    @if (delivery.status().equals("Validiert"))
        <section class="workspace-card">
            <div class="section-heading"><h2>Übergabe</h2></div>
            <form method="post" action="/archivierung/${delivery.deliveryNumber()}/uebergeben" class="inline-form">
                <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                <label>Bemerkung <input name="bemerkung" placeholder="Übergabe an das Archiv"></label>
                <button class="button" type="submit">Übergabe dokumentieren</button>
            </form>
        </section>
    @endif
    @if (delivery.status().equals("Uebergeben"))
        <section class="workspace-card">
            <div class="section-heading"><h2>Entscheid des Archivs</h2></div>
            <form method="post" action="/archivierung/${delivery.deliveryNumber()}/uebernehmen" class="inline-form">
                <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                <label>Archivsignatur <input name="archivsignatur" required></label>
                <label>Bemerkung <input name="bemerkung"></label>
                <button class="button" type="submit">Übernahme dokumentieren</button>
            </form>
            <form method="post" action="/archivierung/${delivery.deliveryNumber()}/ablehnen" class="inline-form">
                <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                <label>Bemerkung <input name="bemerkung"></label>
                <button class="button button-secondary" type="submit">Ablehnung dokumentieren</button>
            </form>
        </section>
    @endif
`)
