@param String title
@param String active
@param java.util.List<guru.interlis.mabillon.aufgabe.AufgabeView> aufgaben
@template.layout(
    title = title,
    active = active,
    content = @`
        <p class="breadcrumb">Meine Aufgaben</p>
        <div class="page-header">
            <div>
                <p class="eyebrow">Arbeitsvorrat</p>
                <h1>${title}</h1>
                <p class="lead">Offene, persönlich zugewiesene Aufgaben.</p>
            </div>
        </div>
        <section class="workspace-card">
            <div class="section-heading"><h2>Offene Aufgaben</h2><span class="count-badge">${aufgaben.size()}</span></div>
            <ul class="record-list">
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
