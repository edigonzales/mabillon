@param String title
@param guru.interlis.mabillon.dashboard.MyWorkView work
@template.layout(
    title = title,
    active = "dashboard",
    content = @`
        <div class="mabillon-page-header">
            <div>
                <nav class="mabillon-breadcrumbs" aria-label="Brotkrümel">Startseite</nav>
                <h1>${title}</h1>
                <p class="mabillon-page-subtitle">Einfache und transparente Geschäftsverwaltung.</p>
            </div>
        </div>
        @if (work == null)
            <section class="mabillon-summary-grid" aria-label="Schnellzugriff">
                <a class="mabillon-summary-card" href="/dossiers/AGI-D-2026-000007">
                    <span class="mabillon-summary-label">Dossier</span>
                    <strong>AGI-D-2026-000007</strong>
                    <span>Gemeinde Musterwil</span>
                </a>
                <a class="mabillon-summary-card" href="/geschaefte/AGI-G-2026-000421">
                    <span class="mabillon-summary-label">Geschäft</span>
                    <strong>AGI-G-2026-000421</strong>
                    <span>Nomenklaturmutation</span>
                </a>
            </section>
        @else
            <div class="mabillon-dashboard-grid">
                <section class="mabillon-tile">
                    <div class="mabillon-section-heading"><h2>Meine offenen Aufgaben</h2><span class="mabillon-count-badge">${work.openTasks().size()}</span></div>
                    @if (work.openTasks().isEmpty())
                        <p class="mabillon-muted">Keine offenen Aufgaben.</p>
                    @else
                        <ul class="mabillon-record-list">
                            @for (var task : work.openTasks())
                                <li><span><strong>${task.title()}</strong><span><a href="/geschaefte/${task.geschaeftsnummer()}">${task.geschaeftsnummer()}</a> · ${task.status()}${task.dueDate() == null ? "" : " · fällig " + task.dueDate().toString()}</span></span></li>
                            @endfor
                        </ul>
                    @endif
                </section>
                <section class="mabillon-tile">
                    <div class="mabillon-section-heading"><h2>Überfällige Aufgaben</h2><span class="mabillon-count-badge">${work.overdueTasks().size()}</span></div>
                    @if (work.overdueTasks().isEmpty())
                        <p class="mabillon-muted">Keine überfälligen Aufgaben.</p>
                    @else
                        <ul class="mabillon-record-list">
                            @for (var task : work.overdueTasks())
                                <li><span><strong>${task.title()}</strong><span><a href="/geschaefte/${task.geschaeftsnummer()}">${task.geschaeftsnummer()}</a> · fällig ${task.dueDate().toString()}</span></span></li>
                            @endfor
                        </ul>
                    @endif
                </section>
                <section class="mabillon-tile">
                    <div class="mabillon-section-heading"><h2>Aktive Geschäfte</h2><span class="mabillon-count-badge">${work.activeBusinesses().size()}</span></div>
                    @if (work.activeBusinesses().isEmpty())
                        <p class="mabillon-muted">Keine aktiven Geschäfte.</p>
                    @else
                        <ul class="mabillon-record-list">
                            @for (var business : work.activeBusinesses())
                                <li><a href="/geschaefte/${business.number()}"><strong>${business.number()}</strong><span>${business.title()}${business.dossierNumber() == null ? "" : " · Dossier " + business.dossierNumber()}</span></a></li>
                            @endfor
                        </ul>
                    @endif
                </section>
            </div>
        @endif
    `
)
