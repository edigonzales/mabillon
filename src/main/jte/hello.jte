@param String title
@param guru.interlis.mabillon.dashboard.MyWorkView work
@template.layout(
    title = title,
    active = "dashboard",
    content = @`
        <div class="page-header">
            <div>
                <p class="breadcrumb">Startseite</p>
                <h1>${title}</h1>
                <p class="lead">Einfache und transparente Geschäftsverwaltung.</p>
            </div>
        </div>
        @if (work == null)
            <section class="summary-grid" aria-label="Schnellzugriff">
                <a class="summary-card" href="/dossiers/AGI-D-2026-000007">
                    <span class="summary-card__label">Dossier</span>
                    <strong>AGI-D-2026-000007</strong>
                    <span>Gemeinde Musterwil</span>
                </a>
                <a class="summary-card" href="/geschaefte/AGI-G-2026-000421">
                    <span class="summary-card__label">Geschäft</span>
                    <strong>AGI-G-2026-000421</strong>
                    <span>Nomenklaturmutation</span>
                </a>
            </section>
        @else
            <section class="summary-grid" aria-label="Meine Arbeit">
                <div class="summary-card"><span class="summary-card__label">Offene Aufgaben</span><strong>${work.openTasks().size()}</strong><span>mir zugewiesen</span></div>
                <div class="summary-card"><span class="summary-card__label">Überfällig</span><strong>${work.overdueTasks().size()}</strong><span>bitte prüfen</span></div>
                <div class="summary-card"><span class="summary-card__label">Aktive Geschäfte</span><strong>${work.activeBusinesses().size()}</strong><span>in meiner Verantwortung</span></div>
            </section>
            <div class="dashboard-grid">
                <section class="workspace-card">
                    <div class="section-heading"><h2>Meine offenen Aufgaben</h2><span class="count-badge">${work.openTasks().size()}</span></div>
                    @if (work.openTasks().isEmpty())
                        <p class="muted">Keine offenen Aufgaben.</p>
                    @else
                        <ul class="record-list">
                            @for (var task : work.openTasks())
                                <li><span><strong>${task.title()}</strong><span><a href="/geschaefte/${task.geschaeftsnummer()}">${task.geschaeftsnummer()}</a> · ${task.status()}${task.dueDate() == null ? "" : " · fällig " + task.dueDate().toString()}</span></span></li>
                            @endfor
                        </ul>
                    @endif
                </section>
                <section class="workspace-card">
                    <div class="section-heading"><h2>Überfällige Aufgaben</h2><span class="count-badge">${work.overdueTasks().size()}</span></div>
                    @if (work.overdueTasks().isEmpty())
                        <p class="muted">Keine überfälligen Aufgaben.</p>
                    @else
                        <ul class="record-list">
                            @for (var task : work.overdueTasks())
                                <li><span><strong>${task.title()}</strong><span><a href="/geschaefte/${task.geschaeftsnummer()}">${task.geschaeftsnummer()}</a> · fällig ${task.dueDate().toString()}</span></span></li>
                            @endfor
                        </ul>
                    @endif
                </section>
                <section class="workspace-card">
                    <div class="section-heading"><h2>Aktive Geschäfte</h2><span class="count-badge">${work.activeBusinesses().size()}</span></div>
                    @if (work.activeBusinesses().isEmpty())
                        <p class="muted">Keine aktiven Geschäfte.</p>
                    @else
                        <ul class="record-list">
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
