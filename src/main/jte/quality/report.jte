@param String title
@param guru.interlis.mabillon.quality.QualityReport report
@template.layout(title = title, active = "control", content = @`
    <div class="page-header"><div><p class="breadcrumb">Datenqualität</p><h1>${title}</h1><p class="lead">Strukturierter Prüfbericht für ${report.objectType()} ${report.objectId()}.</p></div></div>
    <section class="summary-grid" aria-label="Prüfergebnis">
        <div class="summary-card"><span class="summary-card__label">Fehler</span><strong>${report.errorCount()}</strong></div>
        <div class="summary-card"><span class="summary-card__label">Warnungen</span><strong>${report.warningCount()}</strong></div>
        <div class="summary-card"><span class="summary-card__label">Befunde</span><strong>${report.findings().size()}</strong></div>
    </section>
    <section class="workspace-card">
        @if (report.findings().isEmpty())
            <p class="success-message">Keine Datenqualitätsbefunde.</p>
        @else
            <ul class="record-list">
                @for (var finding : report.findings())
                    <li><span><strong>${finding.ruleCode()} · ${finding.severity().name()}</strong><span>${finding.objectType()} ${finding.objectId()} · ${finding.message()}</span></span></li>
                @endfor
            </ul>
        @endif
    </section>
`)
