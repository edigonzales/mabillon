@param guru.interlis.mabillon.geschaeft.GeschaeftView geschaeft
@param java.util.List<guru.interlis.mabillon.catalog.CatalogEntryView> processStatuses
@param java.util.List<guru.interlis.mabillon.catalog.CatalogEntryView> resultStatuses
@param org.springframework.security.web.csrf.CsrfToken csrfToken
<section id="geschaeft-status-panel" class="mabillon-status-panel">
    <div class="mabillon-section-heading">
        <h2>Prozessstatus</h2>
        @if (geschaeft.processStatusName() != null)
            <span class="mabillon-status-badge">${geschaeft.processStatusName()}</span>
        @endif
    </div>
    <p class="mabillon-muted">Lifecycle: ${geschaeft.lifecycleStatus()}</p>
    @if (!processStatuses.isEmpty())
        <form method="post" action="/geschaefte/${geschaeft.number()}/prozessstatus" class="mabillon-status-form">
            <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
            <label for="process-status">Status ändern</label>
            <select id="process-status" name="processStatusCode">
                @for (var status : processStatuses)
                    @if (status.code().equals(geschaeft.processStatusCode()))
                        <option value="${status.code()}" selected>${status.name()}</option>
                    @else
                        <option value="${status.code()}">${status.name()}</option>
                    @endif
                @endfor
            </select>
            <input name="comment" placeholder="Bemerkung (optional)">
            <button class="mabillon-button" type="submit">Status speichern</button>
        </form>
    @endif
    @if (!resultStatuses.isEmpty())
        <form method="post" action="/geschaefte/${geschaeft.number()}/resultat" class="mabillon-status-form">
            <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
            <label for="result-status">Resultat</label>
            <select id="result-status" name="resultStatusCode">
                <option value="">Noch kein Resultat</option>
                @for (var status : resultStatuses)
                    @if (status.code().equals(geschaeft.resultStatusCode()))
                        <option value="${status.code()}" selected>${status.name()}</option>
                    @else
                        <option value="${status.code()}">${status.name()}</option>
                    @endif
                @endfor
            </select>
            <input name="comment" placeholder="Bemerkung (optional)">
            <button class="mabillon-button" type="submit">Resultat speichern</button>
        </form>
    @endif
</section>
