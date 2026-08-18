@param guru.interlis.mabillon.aufgabe.AufgabeView aufgabe
@param org.springframework.security.web.csrf.CsrfToken csrfToken
<li id="aufgabe-${aufgabe.tid().toString()}" class="mabillon-record-list__item">
    <span>
        <strong>${aufgabe.title()}</strong>
        <span>${aufgabe.typName()} · ${aufgabe.status()}${aufgabe.dueDate() == null ? "" : " · fällig " + aufgabe.dueDate().toString()}${aufgabe.assignedUsername() == null ? "" : " · " + aufgabe.assignedUsername()}</span>
    </span>
    @if (aufgabe.open())
        <span class="mabillon-inline-actions">
            @if (!aufgabe.status().equalsIgnoreCase("In_Arbeit"))
                <form method="post" action="/aufgaben/${aufgabe.tid().toString()}/start">
                    <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                    <button class="mabillon-button mabillon-button--secondary" type="submit">Starten</button>
                </form>
            @endif
            <form method="post" action="/aufgaben/${aufgabe.tid().toString()}/complete">
                <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                <button class="mabillon-button" type="submit">Erledigt</button>
            </form>
        </span>
    @endif
</li>
