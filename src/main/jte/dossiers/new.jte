@param String title
@param java.util.List<guru.interlis.mabillon.registraturplan.RegistraturplanPositionView> positions
@param java.util.List<guru.interlis.mabillon.stammdaten.OrganisationseinheitView> organisationseinheiten
@param java.util.List<guru.interlis.mabillon.stammdaten.BenutzerView> benutzer
@param org.springframework.security.web.csrf.CsrfToken csrfToken
@template.layout(
    title = title,
    active = "dossiers",
    content = @`
        <div class="mabillon-page-header"><div><nav class="mabillon-breadcrumbs" aria-label="Brotkrümel"><a href="/dossiers">Dossiers</a> / Neu</nav><h1>Neues Dossier</h1></div></div>
        <section class="mabillon-tile mabillon-form-tile"><form method="post" action="/dossiers" class="mabillon-form-grid">
            <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
            <label>Titel<input name="title" required></label>
            <label>Beschreibung<textarea name="description"></textarea></label>
            <label>Registraturplanposition<select name="position" required>@for (var value : positions)<option value="${value.code()}">${value.code()} · ${value.titel()}</option>@endfor</select></label>
            <label>Federführung<select name="federation" required>@for (var value : organisationseinheiten)<option value="${value.kuerzel()}">${value.kuerzel()} · ${value.name()}</option>@endfor</select></label>
            <label>Verantwortlicher<select name="responsible" required>@for (var value : benutzer)<option value="${value.username()}">${value.username()} · ${value.name()}</option>@endfor</select></label>
            <label>Eröffnet am<input type="date" name="openingDate"></label>
            <div class="mabillon-form-actions"><button class="mabillon-button" type="submit">Dossier eröffnen</button><a href="/dossiers">Abbrechen</a></div>
        </form></section>
    `
)
