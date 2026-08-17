@param guru.interlis.mabillon.geschaeft.GeschaeftView geschaeft
@param java.util.List<guru.interlis.mabillon.catalog.CatalogEntryView> processStatuses
@param java.util.List<guru.interlis.mabillon.catalog.CatalogEntryView> resultStatuses
@param java.util.List<guru.interlis.mabillon.catalog.CatalogEntryView> aufgabentypen
@param java.util.List<guru.interlis.mabillon.beteiligung.BeteiligungView> beteiligungen
@param java.util.List<guru.interlis.mabillon.beteiligung.BeteiligterView> beteiligte
@param java.util.List<guru.interlis.mabillon.aufgabe.AufgabeView> aufgaben
@param java.util.List<guru.interlis.mabillon.fachsystem.FachsystemReferenzView> fachsystemReferenzen
@param java.util.List<guru.interlis.mabillon.journal.JournalEntryView> journalEntries
@param org.springframework.security.web.csrf.CsrfToken csrfToken
<section id="geschaeft-detail" class="workspace-card">
    <div class="object-summary">
        <div>
            <p class="eyebrow">Geschäft</p>
            <h1>${geschaeft.title()}</h1>
            <p class="object-number">${geschaeft.number()}</p>
        </div>
        <span class="status-tag">${geschaeft.lifecycleStatus()}</span>
    </div>
    @if (!geschaeft.lifecycleStatus().equalsIgnoreCase("Abgeschlossen"))
        <form method="post" action="/geschaefte/${geschaeft.number()}/abschluss" class="inline-form">
            <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
            <button class="button button--secondary" type="submit">Geschäft abschliessen</button>
        </form>
    @endif
    <dl class="facts-grid">
        <div><dt>Dossier</dt><dd><a href="/dossiers/${geschaeft.dossierNumber()}">${geschaeft.dossierNumber()}</a></dd></div>
        <div><dt>Kurzbeschreibung</dt><dd>${geschaeft.shortDescription()}</dd></div>
    </dl>
    @if (!geschaeft.lifecycleStatus().equalsIgnoreCase("Abgeschlossen"))
        <form method="post" action="/geschaefte/${geschaeft.number()}" class="form-grid inline-edit-form">
            <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
            <label>Titel<input name="title" value="${geschaeft.title()}" required></label>
            <label>Verantwortlicher<input name="responsible" placeholder="Username (optional)"></label>
            <label>Kurzbeschreibung<textarea name="shortDescription">${geschaeft.shortDescription() == null ? "" : geschaeft.shortDescription()}</textarea></label>
            <label>Fälligkeit<input type="date" name="dueDate"></label>
            <label>Priorität<input type="number" name="priority" min="0" max="5" value="0"></label>
            <div class="form-actions"><button class="button" type="submit">Geschäft speichern</button></div>
        </form>
    @endif
    @template.geschaefte._status-panel(geschaeft = geschaeft, processStatuses = processStatuses, resultStatuses = resultStatuses, csrfToken = csrfToken)
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
    <section>
        <div class="section-heading">
            <h2>Fachsystemreferenzen</h2>
            <span class="count-badge">${fachsystemReferenzen.size()}</span>
        </div>
        @if (fachsystemReferenzen.isEmpty())
            <p class="muted">Keine Fachsystemreferenzen erfasst.</p>
        @else
            <ul class="record-list">
                @for (var referenz : fachsystemReferenzen)
                    <li>
                        <span><strong>${referenz.systemCode()}: ${referenz.objektId()}</strong><span>${referenz.objektTyp()}${referenz.mutationId() == null ? "" : " · Mutation " + referenz.mutationId()}</span></span>
                        @if (referenz.link() != null && !referenz.link().isBlank())<a href="${referenz.link()}" rel="noreferrer">Öffnen</a>@endif
                    </li>
                @endfor
            </ul>
        @endif
        @if (!geschaeft.lifecycleStatus().equalsIgnoreCase("Abgeschlossen"))
            <form method="post" action="/geschaefte/${geschaeft.number()}/fachsystem-referenzen" class="form-grid compact-form">
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
            <h2>Aufgaben</h2>
            <span class="count-badge">${aufgaben.size()}</span>
        </div>
        @if (aufgaben.isEmpty())
            <p class="muted">Keine Aufgaben erfasst.</p>
        @else
            <ul id="geschaeft-aufgaben" class="record-list">
                @for (var aufgabe : aufgaben)
                    @template.geschaefte._task-row(aufgabe = aufgabe, csrfToken = csrfToken)
                @endfor
            </ul>
        @endif
        @if (!geschaeft.lifecycleStatus().equalsIgnoreCase("Abgeschlossen"))
            <form method="post" action="/aufgaben" class="form-grid compact-form">
                <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                <input type="hidden" name="geschaeftNumber" value="${geschaeft.number()}">
                <label>Titel<input name="title" required></label>
                <label>Aufgabentyp<select name="typCode" required>
                    <option value="">Bitte wählen</option>
                    @for (var typ : aufgabentypen)
                        <option value="${typ.code()}">${typ.name()}</option>
                    @endfor
                </select></label>
                <label>Fälligkeit<input type="date" name="dueDate"></label>
                <label>Priorität<input type="number" name="priority" min="0" max="5" value="0"></label>
                <label>Zuweisung<input name="assignedUsername" placeholder="Username (optional)"></label>
                <label>Beschreibung<textarea name="description"></textarea></label>
                <div class="form-actions"><button class="button" type="submit">Aufgabe erfassen</button></div>
            </form>
        @endif
    </section>
    <section>
        <div class="section-heading">
            <h2>Beteiligte</h2>
            <span class="count-badge">${beteiligungen.size()}</span>
        </div>
        @if (beteiligungen.isEmpty())
            <p class="muted">Keine Beteiligungen erfasst.</p>
        @else
            <ul class="record-list">
                @for (var beteiligung : beteiligungen)
                    <li><span><strong>${beteiligung.beteiligterName()}</strong><span>${beteiligung.rollenName()}${beteiligung.rollenbezeichnung() == null ? "" : " · " + beteiligung.rollenbezeichnung()}</span></span></li>
                @endfor
            </ul>
        @endif
        @if (!geschaeft.lifecycleStatus().equalsIgnoreCase("Abgeschlossen"))
            <form method="post" action="/beteiligungen" class="form-grid compact-form">
                <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                <input type="hidden" name="geschaeftNumber" value="${geschaeft.number()}">
                <label>Beteiligter<select name="beteiligterTid" required>
                    <option value="">Bitte wählen</option>
                    @for (var person : beteiligte)
                        <option value="${person.tid().toString()}">${person.name()}${person.organisation() == null ? "" : " · " + person.organisation()}</option>
                    @endfor
                </select></label>
                <label>Rolle<select name="rollenCode" required>
                    <option value="">Bitte wählen</option>
                    <option value="ANTRAGSTELLERIN">Antragstellerin</option>
                    <option value="ADRESSATIN">Adressatin</option>
                    <option value="FACHSTELLE">Fachstelle</option>
                </select></label>
                <div class="form-actions"><button class="button" type="submit">Beteiligung erfassen</button></div>
            </form>
        @endif
    </section>
    <section>
        <div class="section-heading">
            <h2>Unterlagen im Geschäft</h2>
            <span class="count-badge">${geschaeft.unterlagen().size()}</span>
        </div>
        @if (geschaeft.unterlagen().isEmpty())
            <p class="muted">Keine Unterlagen zugeordnet.</p>
        @else
            <ul class="record-list">
                @for (var unterlage : geschaeft.unterlagen())
                    <li>
                        <span>
                            <strong>${unterlage.title()}</strong>
                            <span>${unterlage.filename()} · ${unterlage.status()}</span>
                            @if (unterlage.downloadable())
                                <a href="/unterlagen/${unterlage.tid().toString()}/download">Herunterladen</a>
                            @endif
                        </span>
                    </li>
                @endfor
            </ul>
        @endif
    </section>
</section>
