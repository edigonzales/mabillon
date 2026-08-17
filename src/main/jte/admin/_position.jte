@param guru.interlis.mabillon.registraturplan.RegistraturplanPositionView position
@param org.springframework.security.web.csrf.CsrfToken csrfToken
<li>
    <div class="tree-item">
        <span><strong>${position.code()}</strong> ${position.titel()} <span class="muted">· ${position.status()}</span></span>
        @if (position.leaf() && position.active())
            <span class="status-tag">auswählbar</span>
        @endif
    </div>
    <details>
        <summary>Bearbeiten</summary>
        <form class="admin-form" method="post" action="/admin/registraturplan/positionen/${position.code()}">
            <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
            <label>Titel <input name="titel" required value="${position.titel()}"></label>
            <label>Beschreibung <textarea name="beschreibung">${position.beschreibung() == null ? "" : position.beschreibung()}</textarea></label>
            <label>Status <input name="status" required value="${position.status()}"></label>
            <button class="button button--quiet" type="submit">Speichern</button>
        </form>
        <form class="admin-form" method="post" action="/admin/registraturplan/positionen/${position.code()}/verschieben">
            <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
            <label>Neue Oberposition <input name="oberposition" value="${position.parentCode() == null ? "" : position.parentCode()}"></label>
            <button class="button button--quiet" type="submit">Verschieben</button>
        </form>
        @if (position.active())
            <form method="post" action="/admin/registraturplan/positionen/${position.code()}/deactivate">
                <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                <button class="button button--quiet" type="submit">Deaktivieren</button>
            </form>
        @endif
    </details>
    @if (!position.children().isEmpty())
        <ul class="tree-list">
            @for (var child : position.children())
                @template.admin._position(position = child, csrfToken = csrfToken)
            @endfor
        </ul>
    @endif
</li>
