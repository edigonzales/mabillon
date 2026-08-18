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
<section id="geschaeft-detail" class="mabillon-detail-workspace">
    <div class="mabillon-object-summary">
        <div>
            <p class="mabillon-eyebrow">Geschäft</p>
            <h1>${geschaeft.title()}</h1>
            <p class="mabillon-object-number">${geschaeft.number()}</p>
        </div>
        <div class="mabillon-page-actions">
            <span class="mabillon-status-badge">${geschaeft.lifecycleStatus()}</span>
            @if (!geschaeft.lifecycleStatus().equalsIgnoreCase("Abgeschlossen"))
                <form method="post" action="/geschaefte/${geschaeft.number()}/abschluss" class="mabillon-inline-form">
                    <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                    <button class="mabillon-button mabillon-button--secondary" type="submit">Geschäft abschliessen</button>
                </form>
            @endif
        </div>
    </div>
    <section class="mabillon-tile">
        <dl class="mabillon-definition-grid">
            <div><dt>Dossier</dt><dd><a href="/dossiers/${geschaeft.dossierNumber()}">${geschaeft.dossierNumber()}</a></dd></div>
            <div><dt>Kurzbeschreibung</dt><dd>${geschaeft.shortDescription()}</dd></div>
        </dl>
        @if (!geschaeft.lifecycleStatus().equalsIgnoreCase("Abgeschlossen"))
            <form method="post" action="/geschaefte/${geschaeft.number()}" class="mabillon-form-grid mabillon-inline-edit-form">
                <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                <label>Titel<input name="title" value="${geschaeft.title()}" required></label>
                <label>Verantwortlicher<input name="responsible" placeholder="Username (optional)"></label>
                <label>Kurzbeschreibung<textarea name="shortDescription">${geschaeft.shortDescription() == null ? "" : geschaeft.shortDescription()}</textarea></label>
                <label>Fälligkeit<input type="date" name="dueDate"></label>
                <label>Priorität<input type="number" name="priority" min="0" max="5" value="0"></label>
                <div class="mabillon-form-actions"><button class="mabillon-button" type="submit">Geschäft speichern</button></div>
            </form>
        @endif
        @template.geschaefte._status-panel(geschaeft = geschaeft, processStatuses = processStatuses, resultStatuses = resultStatuses, csrfToken = csrfToken)
    </section>
    <section class="mabillon-tile">
        <div class="mabillon-section-heading"><h2>Journal / Audit</h2><span class="mabillon-count-badge">${journalEntries.size()}</span></div>
        @if (journalEntries.isEmpty())
            <p class="mabillon-muted">Keine Journalereignisse vorhanden.</p>
        @else
            <ul class="mabillon-record-list">
                @for (var entry : journalEntries)
                    <li><span><strong>${entry.typ().name()}</strong><span>${entry.zeitpunkt().toString()} · ${entry.username() == null ? "unbekannt" : entry.username()}</span><span>${entry.bemerkung() == null ? "" : entry.bemerkung()}</span></span></li>
                @endfor
            </ul>
        @endif
    </section>
    <section class="mabillon-tile">
        <div class="mabillon-section-heading">
            <h2>Fachsystemreferenzen</h2>
            <span class="mabillon-count-badge">${fachsystemReferenzen.size()}</span>
        </div>
        @if (fachsystemReferenzen.isEmpty())
            <p class="mabillon-muted">Keine Fachsystemreferenzen erfasst.</p>
        @else
            <ul class="mabillon-record-list">
                @for (var referenz : fachsystemReferenzen)
                    <li>
                        <span><strong>${referenz.systemCode()}: ${referenz.objektId()}</strong><span>${referenz.objektTyp()}${referenz.mutationId() == null ? "" : " · Mutation " + referenz.mutationId()}</span></span>
                        @if (referenz.link() != null && !referenz.link().isBlank())<a href="${referenz.link()}" rel="noreferrer">Öffnen</a>@endif
                    </li>
                @endfor
            </ul>
        @endif
        @if (!geschaeft.lifecycleStatus().equalsIgnoreCase("Abgeschlossen"))
            <form method="post" action="/geschaefte/${geschaeft.number()}/fachsystem-referenzen" class="mabillon-form-grid mabillon-form-grid--compact">
                <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                <label>Systemcode<input name="systemCode" required></label>
                <label>Objekttyp<input name="objektTyp" required></label>
                <label>Objekt-ID<input name="objektId" required></label>
                <label>Mutations-ID<input name="mutationId"></label>
                <label>Link<input type="url" name="link"></label>
                <label>Beschreibung<textarea name="beschreibung"></textarea></label>
                <div class="mabillon-form-actions"><button class="mabillon-button" type="submit">Referenz erfassen</button></div>
            </form>
        @endif
    </section>
    <section class="mabillon-tile">
        <div class="mabillon-section-heading">
            <h2>Aufgaben</h2>
            <span class="mabillon-count-badge">${aufgaben.size()}</span>
        </div>
        @if (aufgaben.isEmpty())
            <p class="mabillon-muted">Keine Aufgaben erfasst.</p>
        @else
            <ul id="geschaeft-aufgaben" class="mabillon-record-list">
                @for (var aufgabe : aufgaben)
                    @template.geschaefte._task-row(aufgabe = aufgabe, csrfToken = csrfToken)
                @endfor
            </ul>
        @endif
        @if (!geschaeft.lifecycleStatus().equalsIgnoreCase("Abgeschlossen"))
            <form method="post" action="/aufgaben" class="mabillon-form-grid mabillon-form-grid--compact">
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
                <div class="mabillon-form-actions"><button class="mabillon-button" type="submit">Aufgabe erfassen</button></div>
            </form>
        @endif
    </section>
    <section class="mabillon-tile">
        <div class="mabillon-section-heading">
            <h2>Beteiligte</h2>
            <span class="mabillon-count-badge">${beteiligungen.size()}</span>
        </div>
        @if (beteiligungen.isEmpty())
            <p class="mabillon-muted">Keine Beteiligungen erfasst.</p>
        @else
            <ul class="mabillon-record-list">
                @for (var beteiligung : beteiligungen)
                    <li class="mabillon-record-row">
                        <span>
                            <strong><a href="/beteiligte/${beteiligung.beteiligterTid().toString()}">${beteiligung.beteiligterName()}</a></strong>
                            <span>${beteiligung.rollenName()}${beteiligung.rollenbezeichnung() == null ? "" : " · " + beteiligung.rollenbezeichnung()}</span>
                            <span>${beteiligung.gueltigVon() == null ? "" : "gültig ab " + beteiligung.gueltigVon().toString()}${beteiligung.gueltigBis() == null ? "" : " · bis " + beteiligung.gueltigBis().toString()}</span>
                        </span>
                        @if (!geschaeft.lifecycleStatus().equalsIgnoreCase("Abgeschlossen"))
                            <details>
                                <summary>Bearbeiten</summary>
                                <form method="post" action="/beteiligungen/${beteiligung.tid().toString()}" class="mabillon-form-grid mabillon-form-grid--compact">
                                    <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                                    <label>Rollenbezeichnung<input name="rollenbezeichnung" value="${beteiligung.rollenbezeichnung() == null ? "" : beteiligung.rollenbezeichnung()}"></label>
                                    <label>Gültig von<input type="date" name="gueltigVon" value="${beteiligung.gueltigVon() == null ? "" : beteiligung.gueltigVon().toString()}"></label>
                                    <label>Gültig bis<input type="date" name="gueltigBis" value="${beteiligung.gueltigBis() == null ? "" : beteiligung.gueltigBis().toString()}"></label>
                                    <label>Bemerkung<textarea name="bemerkung">${beteiligung.bemerkung() == null ? "" : beteiligung.bemerkung()}</textarea></label>
                                    <div class="mabillon-form-actions"><button class="mabillon-button mabillon-button--secondary" type="submit">Beteiligung speichern</button></div>
                                </form>
                                <form method="post" action="/beteiligungen/${beteiligung.tid().toString()}/end" class="mabillon-inline-form">
                                    <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                                    <input type="hidden" name="geschaeftNumber" value="${geschaeft.number()}">
                                    <label>Enddatum<input type="date" name="endDate" required></label>
                                    <button class="mabillon-button mabillon-button--secondary" type="submit">Beteiligung beenden</button>
                                </form>
                            </details>
                        @endif
                    </li>
                @endfor
            </ul>
        @endif
        @if (!geschaeft.lifecycleStatus().equalsIgnoreCase("Abgeschlossen"))
            <form method="post" action="/beteiligungen" class="mabillon-form-grid mabillon-form-grid--compact">
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
                <div class="mabillon-form-actions"><button class="mabillon-button" type="submit">Beteiligung erfassen</button></div>
            </form>
        @endif
    </section>
    <section class="mabillon-tile">
        <div class="mabillon-section-heading">
            <h2>Unterlagen im Geschäft</h2>
            <span class="mabillon-count-badge">${geschaeft.unterlagen().size()}</span>
        </div>
        @if (geschaeft.unterlagen().isEmpty())
            <p class="mabillon-muted">Keine Unterlagen zugeordnet.</p>
        @else
            <ul class="mabillon-record-list">
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
