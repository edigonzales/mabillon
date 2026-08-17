@param String title
@param String active
@template.layout(
    title = title,
    active = active,
    content = @`
        <p class="breadcrumb"><a href="/">Übersicht</a> / Administration</p>
        <div class="page-header">
            <div>
                <p class="eyebrow">Administration</p>
                <h1>${title}</h1>
                <p class="lead">Kataloge, Stammdaten und Registraturplan.</p>
            </div>
        </div>
        <section class="summary-grid" aria-label="Administrationsbereiche">
            <a class="summary-card" href="/admin/kataloge">
                <span class="summary-card__label">Konfiguration</span>
                <strong>Kataloge</strong>
                <span>Geschäftsarten, Status und weitere Werte</span>
            </a>
            <a class="summary-card" href="/admin/stammdaten">
                <span class="summary-card__label">Organisation</span>
                <strong>Stammdaten</strong>
                <span>Organisationseinheiten und Benutzer</span>
            </a>
            <a class="summary-card" href="/admin/registraturplan">
                <span class="summary-card__label">Ordnungssystem</span>
                <strong>Registraturplan</strong>
                <span>Gültige Positionen und Hierarchie</span>
            </a>
            <a class="summary-card" href="/admin/interlis">
                <span class="summary-card__label">Datenaustausch</span>
                <strong>INTERLIS</strong>
                <span>XTF validieren, importieren und exportieren</span>
            </a>
        </section>
    `
)
