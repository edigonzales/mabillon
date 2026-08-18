@param String title
@param String active
@param guru.interlis.mabillon.catalog.CatalogEntryView entry
@param java.util.List<guru.interlis.mabillon.catalog.CatalogEntryView> geschaeftsarten
@param org.springframework.security.web.csrf.CsrfToken csrfToken
@template.layout(
    title = title,
    active = active,
    content = @`
        <nav class="mabillon-breadcrumbs" aria-label="Brotkrümel"><a href="/admin/kataloge">Kataloge</a> / ${entry.code()}</nav>
        <div class="mabillon-page-header"><div><p class="mabillon-eyebrow">${entry.type().label()}</p><h1>${title}</h1></div></div>
        <section class="mabillon-tile">
            <form class="mabillon-form" method="post" action="/admin/kataloge/${entry.type().name().toLowerCase()}/${entry.code()}">
                <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                <label>Code <input value="${entry.code()}" disabled></label>
                <label>Name <input name="name" required value="${entry.name()}"></label>
                <label>Beschreibung <textarea name="description">${entry.description() == null ? "" : entry.description()}</textarea></label>
                @if (entry.type().name().equals("GESCHAEFTSART"))
                    @if (entry.resultatErforderlich())
                        <label class="mabillon-checkbox"><input type="checkbox" name="resultatErforderlich" checked> Resultat erforderlich</label>
                    @else
                        <label class="mabillon-checkbox"><input type="checkbox" name="resultatErforderlich"> Resultat erforderlich</label>
                    @endif
                @endif
                @if (entry.type().name().equals("PROZESSSTATUS") || entry.type().name().equals("RESULTATSTATUS"))
                    <label>Geschäftsart
                        <select name="geschaeftsartCode" required>
                            @for (var art : geschaeftsarten)
                                @if (art.code().equals(entry.geschaeftsartCode()))
                                    <option value="${art.code()}" selected>${art.name()} (${art.code()})</option>
                                @else
                                    <option value="${art.code()}">${art.name()} (${art.code()})</option>
                                @endif
                            @endfor
                        </select>
                    </label>
                    <label>Sortierung <input type="number" name="sortierung" value="${entry.sortierung() == null ? "" : entry.sortierung().toString()}"></label>
                @endif
                @if (entry.type().name().equals("PROZESSSTATUS"))
                    @if (entry.initial())
                        <label class="mabillon-checkbox"><input type="checkbox" name="initial" checked> Initialstatus</label>
                    @else
                        <label class="mabillon-checkbox"><input type="checkbox" name="initial"> Initialstatus</label>
                    @endif
                @endif
                @if (entry.type().name().equals("PROZESSSTATUS") || entry.type().name().equals("RESULTATSTATUS"))
                    @if (entry.terminal())
                        <label class="mabillon-checkbox"><input type="checkbox" name="terminal" checked> Terminalstatus</label>
                    @else
                        <label class="mabillon-checkbox"><input type="checkbox" name="terminal"> Terminalstatus</label>
                    @endif
                @endif
                <div class="mabillon-page-actions">
                    <button class="mabillon-button" type="submit">Speichern</button>
                    <a class="mabillon-button mabillon-button--secondary" href="/admin/kataloge">Abbrechen</a>
                </div>
            </form>
        </section>
    `
)
