@param String title
@param String active
@param java.util.List<guru.interlis.mabillon.aufgabe.AufgabeView> aufgaben
@template.layout(
    title = title,
    active = active,
    content = @`
        <nav class="mabillon-breadcrumbs" aria-label="Brotkrümel">Meine Aufgaben</nav>
        <div class="mabillon-page-header">
            <div>
                <p class="mabillon-eyebrow">Arbeitsvorrat</p>
                <h1>${title}</h1>
                <p class="mabillon-page-subtitle">Offene, persönlich zugewiesene Aufgaben.</p>
            </div>
        </div>
        <section class="mabillon-tile">
            <div class="mabillon-section-heading"><h2>Offene Aufgaben</h2><span class="mabillon-count-badge">${aufgaben.size()}</span></div>
            <ul class="mabillon-record-list">
                @for (var aufgabe : aufgaben)
                    <li>
                        <a href="/aufgaben/${aufgabe.tid()}">
                            <strong>${aufgabe.title()}</strong>
                            <span>${aufgabe.status()} · ${aufgabe.geschaeftsnummer()}@if (aufgabe.dueDate() != null) · fällig ${aufgabe.dueDate().toString()}@endif</span>
                        </a>
                    </li>
                @endfor
            </ul>
        </section>
    `
)
