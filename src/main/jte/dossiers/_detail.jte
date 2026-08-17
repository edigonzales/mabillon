@param guru.interlis.mabillon.dossier.DossierView dossier
@param java.util.List<guru.interlis.mabillon.catalog.CatalogEntryView> unterlagentypen
@param java.util.List<guru.interlis.mabillon.fachsystem.FachsystemReferenzView> fachsystemReferenzen
@param java.util.List<guru.interlis.mabillon.journal.JournalEntryView> journalEntries
@param org.springframework.security.web.csrf.CsrfToken csrfToken
<section id="dossier-detail" class="workspace-card">
    <div class="object-summary">
        <div>
            <p class="eyebrow">Dossier</p>
            <h1>${dossier.title()}</h1>
            <p class="object-number">${dossier.number()}</p>
        </div>
        <span class="status-tag">${dossier.status()}</span>
    </div>
    @if (dossier.status().equalsIgnoreCase("Offen"))
        <form method="post" action="/dossiers/${dossier.number()}/abschluss" class="inline-form">
            <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
            <button class="button button--secondary" type="submit">Dossier abschliessen</button>
        </form>
    @endif
    <div class="object-description">
        <h2>Beschreibung</h2>
        @if (dossier.description() == null || dossier.description().isBlank())
            <p class="muted">Keine Beschreibung erfasst.</p>
        @else
            <p>${dossier.description()}</p>
        @endif
    </div>
    @if (dossier.status().equalsIgnoreCase("Offen"))
        <form method="post" action="/dossiers/${dossier.number()}" class="form-grid inline-edit-form">
            <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
            <label>Titel<input name="title" value="${dossier.title()}" required></label>
            <label>Verantwortlicher<input name="responsible" placeholder="Username (optional)"></label>
            <label>Beschreibung<textarea name="description">${dossier.description() == null ? "" : dossier.description()}</textarea></label>
            <label>Bemerkungen<textarea name="remarks"></textarea></label>
            <div class="form-actions"><button class="button" type="submit">Dossier speichern</button></div>
        </form>
    @endif
    <div class="detail-grid">
        <section>
            <div class="section-heading"><h2>Fachsystemreferenzen</h2><span class="count-badge">${fachsystemReferenzen.size()}</span></div>
            @if (fachsystemReferenzen.isEmpty())
                <p class="muted">Keine Fachsystemreferenzen erfasst.</p>
            @else
                <ul class="record-list">
                    @for (var referenz : fachsystemReferenzen)
                        <li>
                            <span><strong>${referenz.systemCode()}: ${referenz.objektId()}</strong><span>${referenz.objektTyp()}${referenz.geschaeftsnummer() == null ? "" : " · Geschäft " + referenz.geschaeftsnummer()}</span></span>
                            @if (referenz.link() != null && !referenz.link().isBlank())<a href="${referenz.link()}" rel="noreferrer">Öffnen</a>@endif
                        </li>
                    @endfor
                </ul>
            @endif
            @if (dossier.status().equalsIgnoreCase("Offen"))
                <form method="post" action="/dossiers/${dossier.number()}/fachsystem-referenzen" class="form-grid compact-form">
                    <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                    <label>Systemcode<input name="systemCode" required></label>
                    <label>Objekttyp<input name="objektTyp" required></label>
                    <label>Objekt-ID<input name="objektId" required></label>
                    <label>Mutations-ID<input name="mutationId"></label>
                    <label>Link<input type="url" name="link"></label>
                    <label>Beschreibung<textarea name="beschreibung"></textarea></label>
                    <div class="form-actions"><button class="button" type="submit">Referenz erfassen</button></div>
                </form>
            @endif
        </section>
        <section>
            <div class="section-heading">
                <h2>Geschäfte</h2>
                <span class="count-badge">${dossier.geschaefte().size()}</span>
            </div>
            @if (dossier.geschaefte().isEmpty())
                <p class="muted">Keine Geschäfte vorhanden.</p>
            @else
                <ul class="record-list">
                    @for (var geschaeft : dossier.geschaefte())
                        <li>
                            <a href="/geschaefte/${geschaeft.number()}">
                                <strong>${geschaeft.title()}</strong>
                                <span>${geschaeft.number()} · ${geschaeft.status()}</span>
                            </a>
                        </li>
                    @endfor
                </ul>
            @endif
        </section>
        <section>
            <div class="section-heading">
                <h2>Unterlagen</h2>
                <span class="count-badge">${dossier.unterlagen().size()}</span>
            </div>
            @if (dossier.unterlagen().isEmpty())
                <p class="muted">Keine Unterlagen vorhanden.</p>
            @else
                <ul class="record-list">
                    @for (var unterlage : dossier.unterlagen())
                        <li>
                            <span>
                        <strong>${unterlage.title()}</strong>
                                <span>${unterlage.filename() == null ? "ohne Datei" : unterlage.filename()} · ${unterlage.status()}</span>
                                @if (unterlage.downloadable())
                                    <a href="/unterlagen/${unterlage.tid().toString()}/download">Herunterladen</a>
                                @endif
                            </span>
                        </li>
                    @endfor
                </ul>
            @endif
            @if (dossier.status().equalsIgnoreCase("Offen"))
                <form method="post" action="/dossiers/${dossier.number()}/unterlagen" enctype="multipart/form-data" class="form-grid compact-form">
                    <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                    <label>Titel<input name="title" required></label>
                    <label>Unterlagentyp<select name="typCode" required>
                        <option value="">Bitte wählen</option>
                        @for (var typ : unterlagentypen)
                            <option value="${typ.code()}">${typ.name()}</option>
                        @endfor
                    </select></label>
                    <label>Datei<input type="file" name="file" required></label>
                    <label>Unterlagendatum<input type="date" name="documentDate"></label>
                    <label>Eingangsdatum<input type="date" name="incomingDate"></label>
                    <label>Technisches Format<input name="dateiformat" placeholder="PDF, EML, DOCX"></label>
                    <div class="form-actions"><button class="button" type="submit">Unterlage registrieren</button></div>
                </form>
            @endif
        </section>
    </div>
    <section>
        <div class="section-heading"><h2>Journal / Audit</h2><span class="count-badge">${journalEntries.size()}</span></div>
        @if (journalEntries.isEmpty())
            <p class="muted">Keine Journalereignisse vorhanden.</p>
        @else
            <ul class="record-list">
                @for (var entry : journalEntries)
                    <li><span><strong>${entry.typ().name()}</strong><span>${entry.zeitpunkt().toString()} · ${entry.username() == null ? "unbekannt" : entry.username()}</span><span>${entry.bemerkung() == null ? "" : entry.bemerkung()}</span></span></li>
                @endfor
            </ul>
        @endif
    </section>
</section>
