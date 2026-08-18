@param String title
@param String active
@template.layout(
    title = title,
    active = active,
    content = @`
        <nav class="mabillon-breadcrumbs" aria-label="Brotkrümel"><a href="/">Übersicht</a> / Administration</nav>
        <div class="mabillon-page-header">
            <div>
                <p class="mabillon-eyebrow">Administration</p>
                <h1>${title}</h1>
                <p class="mabillon-page-subtitle">Kataloge, Stammdaten und Registraturplan.</p>
            </div>
        </div>
        <section class="mabillon-summary-grid" aria-label="Administrationsbereiche">
            <a class="mabillon-summary-card" href="/admin/kataloge">
                <span class="mabillon-summary-label">Konfiguration</span>
                <strong>Kataloge</strong>
                <span>Geschäftsarten, Status und weitere Werte</span>
            </a>
            <a class="mabillon-summary-card" href="/admin/stammdaten">
                <span class="mabillon-summary-label">Organisation</span>
                <strong>Stammdaten</strong>
                <span>Organisationseinheiten und Benutzer</span>
            </a>
            <a class="mabillon-summary-card" href="/admin/registraturplan">
                <span class="mabillon-summary-label">Ordnungssystem</span>
                <strong>Registraturplan</strong>
                <span>Gültige Positionen und Hierarchie</span>
            </a>
            <a class="mabillon-summary-card" href="/admin/interlis">
                <span class="mabillon-summary-label">Datenaustausch</span>
                <strong>INTERLIS</strong>
                <span>XTF validieren, importieren und exportieren</span>
            </a>
        </section>
    `
)
