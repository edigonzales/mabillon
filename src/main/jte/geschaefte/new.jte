@param String title
@param java.util.List<guru.interlis.mabillon.dossier.DossierView> dossiers
@param java.util.List<guru.interlis.mabillon.catalog.CatalogEntryView> types
@param java.util.List<guru.interlis.mabillon.stammdaten.OrganisationseinheitView> organisationseinheiten
@param java.util.List<guru.interlis.mabillon.stammdaten.BenutzerView> benutzer
@param org.springframework.security.web.csrf.CsrfToken csrfToken
@template.layout(
    title = title,
    active = "geschaefte",
    content = @`
        <div class="page-header"><div><p class="breadcrumb"><a href="/geschaefte">Geschäfte</a> / Neu</p><h1>Neues Geschäft</h1></div></div>
        <section class="workspace-card form-card"><form method="post" action="/geschaefte" class="form-grid">
            <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
            <label>Dossier<select name="dossierNumber" required>@for (var value : dossiers)<option value="${value.number()}">${value.number()} · ${value.title()}</option>@endfor</select></label>
            <label>Titel<input name="title" required></label>
            <label>Kurzbeschreibung<textarea name="shortDescription"></textarea></label>
            <label>Geschäftsart<select name="type" required>@for (var value : types)<option value="${value.code()}">${value.name()}</option>@endfor</select></label>
            <label>Federführung<select name="federation" required>@for (var value : organisationseinheiten)<option value="${value.kuerzel()}">${value.kuerzel()} · ${value.name()}</option>@endfor</select></label>
            <label>Verantwortlicher<select name="responsible" required>@for (var value : benutzer)<option value="${value.username()}">${value.username()} · ${value.name()}</option>@endfor</select></label>
            <label>Eingangsdatum<input type="date" name="receivedDate"></label>
            <label>Eröffnungsdatum<input type="date" name="openingDate"></label>
            <label>Fälligkeit<input type="date" name="dueDate"></label>
            <label>Priorität<input type="number" name="priority" min="0" max="5" value="0"></label>
            <div class="form-actions"><button class="button" type="submit">Geschäft eröffnen</button><a href="/geschaefte">Abbrechen</a></div>
        </form></section>
    `
)
