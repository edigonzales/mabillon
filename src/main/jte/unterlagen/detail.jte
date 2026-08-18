@param String title
@param String active
@param guru.interlis.mabillon.unterlage.UnterlageView unterlage
@param org.springframework.security.web.csrf.CsrfToken csrfToken
@template.layout(
    title = title,
    active = active,
    content = @`
        <p class="breadcrumb"><a href="/dossiers/${unterlage.dossierNumber()}">${unterlage.dossierNumber()}</a> / Unterlage</p>
        <div class="page-header">
            <div>
                <p class="eyebrow">${unterlage.typName() == null ? unterlage.typCode() : unterlage.typName()}</p>
                <h1>${unterlage.title()}</h1>
                <p class="muted">Status: ${unterlage.status()}${unterlage.aktenrelevant() ? " · aktenrelevant" : ""}</p>
            </div>
            @if (unterlage.storageUri() != null && unterlage.storageUri().startsWith("mabillon:objects/"))
                <a class="button" href="/unterlagen/${unterlage.tid().toString()}/download">Herunterladen</a>
            @endif
        </div>

        <section class="workspace-card">
            <dl class="detail-grid">
                <div><dt>Dossier</dt><dd><a href="/dossiers/${unterlage.dossierNumber()}">${unterlage.dossierNumber()}</a></dd></div>
                <div><dt>Geschäft</dt><dd>${unterlage.geschaeftsnummer() == null ? "–" : unterlage.geschaeftsnummer()}</dd></div>
                <div><dt>Unterlagendatum</dt><dd>${unterlage.documentDate() == null ? "–" : unterlage.documentDate().toString()}</dd></div>
                <div><dt>Eingangsdatum</dt><dd>${unterlage.incomingDate() == null ? "–" : unterlage.incomingDate().toString()}</dd></div>
                <div><dt>Ausgangsdatum</dt><dd>${unterlage.outgoingDate() == null ? "–" : unterlage.outgoingDate().toString()}</dd></div>
                <div><dt>Registriert am</dt><dd>${unterlage.registeredAt() == null ? "–" : unterlage.registeredAt().toString()}</dd></div>
                <div><dt>Datei</dt><dd>${unterlage.filename() == null ? "–" : unterlage.filename()}</dd></div>
                <div><dt>MIME-Type</dt><dd>${unterlage.mimeType() == null ? "–" : unterlage.mimeType()}</dd></div>
                <div><dt>Dateiformat</dt><dd>${unterlage.dateiformat() == null ? "–" : unterlage.dateiformat()}</dd></div>
                <div><dt>SHA-256</dt><dd>${unterlage.sha256() == null ? "–" : unterlage.sha256()}</dd></div>
            </dl>
            @if (unterlage.bemerkungen() != null && !unterlage.bemerkungen().isBlank())
                <p>${unterlage.bemerkungen()}</p>
            @endif
        </section>

        @if (!unterlage.status().equalsIgnoreCase("Storniert"))
            <section class="workspace-card">
                <div class="section-heading"><h2>Metadaten bearbeiten</h2></div>
                <form method="post" action="/unterlagen/${unterlage.tid().toString()}" class="form-grid">
                    <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                    <label>Titel<input name="title" value="${unterlage.title()}" required></label>
                    <label>Unterlagentyp-Code<input name="typCode" value="${unterlage.typCode()}" required></label>
                    <label>Unterlagendatum<input type="date" name="documentDate" value="${unterlage.documentDate() == null ? "" : unterlage.documentDate().toString()}"></label>
                    <label>Eingangsdatum<input type="date" name="incomingDate" value="${unterlage.incomingDate() == null ? "" : unterlage.incomingDate().toString()}"></label>
                    <label>Ausgangsdatum<input type="date" name="outgoingDate" value="${unterlage.outgoingDate() == null ? "" : unterlage.outgoingDate().toString()}"></label>
                    <label>Dateiformat<input name="dateiformat" value="${unterlage.dateiformat() == null ? "" : unterlage.dateiformat()}"></label>
                    <label>Bemerkungen<textarea name="bemerkungen">${unterlage.bemerkungen() == null ? "" : unterlage.bemerkungen()}</textarea></label>
                    <div class="form-actions"><button class="button" type="submit">Metadaten speichern</button></div>
                </form>
            </section>

            <section class="workspace-card">
                <div class="section-heading"><h2>Geschäftskontext</h2></div>
                @if (unterlage.geschaeftsnummer() == null)
                    <form method="post" action="/unterlagen/${unterlage.tid().toString()}/geschaeft" class="inline-form">
                        <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                        <label>Geschäftsnummer<input name="geschaeftNumber" required></label>
                        <button class="button button--quiet" type="submit">Geschäft zuordnen</button>
                    </form>
                @else
                    <p><a href="/geschaefte/${unterlage.geschaeftsnummer()}">${unterlage.geschaeftsnummer()}</a></p>
                    <form method="post" action="/unterlagen/${unterlage.tid().toString()}/geschaeft/entfernen" class="inline-form">
                        <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                        <button class="button button--quiet" type="submit">Geschäftszuordnung entfernen</button>
                    </form>
                @endif
            </section>

            <section class="workspace-card">
                <div class="section-heading"><h2>Lebenszyklus</h2></div>
                @if (unterlage.status().equalsIgnoreCase("In_Arbeit"))
                    <form method="post" action="/unterlagen/${unterlage.tid().toString()}/finalisieren" class="inline-form">
                        <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                        <button class="button" type="submit">Unterlage finalisieren</button>
                    </form>
                @endif
                @if (unterlage.status().equalsIgnoreCase("Final"))
                    <form method="post" action="/unterlagen/${unterlage.tid().toString()}/aktenrelevant-registrieren" class="inline-form">
                        <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                        <button class="button" type="submit">Aktenrelevant registrieren</button>
                    </form>
                @endif
                <form method="post" action="/unterlagen/${unterlage.tid().toString()}/stornieren" class="form-grid compact-form">
                    <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                    <label>Stornogrund<input name="reason"></label>
                    <div class="form-actions"><button class="button button--quiet" type="submit">Unterlage stornieren</button></div>
                </form>
            </section>
        @else
            <section class="workspace-card"><p class="muted">Diese Unterlage ist storniert und kann nicht mehr geändert werden.</p></section>
        @endif
    `
)
