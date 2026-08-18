@param String title
@param guru.interlis.mabillon.quality.QualityReport report
@template.layout(title = title, active = "control", content = @`
    <div class="mabillon-page-header"><div><nav class="mabillon-breadcrumbs" aria-label="Brotkrümel">Datenqualität</nav><h1>${title}</h1><p class="mabillon-page-subtitle">Strukturierter Prüfbericht für ${report.objectType()} ${report.objectId()}.</p></div></div>
    <section class="mabillon-summary-grid" aria-label="Prüfergebnis">
        <div class="mabillon-summary-card"><span class="mabillon-summary-label">Fehler</span><strong>${report.errorCount()}</strong></div>
        <div class="mabillon-summary-card"><span class="mabillon-summary-label">Warnungen</span><strong>${report.warningCount()}</strong></div>
        <div class="mabillon-summary-card"><span class="mabillon-summary-label">Befunde</span><strong>${report.findings().size()}</strong></div>
    </section>
    <section class="mabillon-tile">
        @if (report.findings().isEmpty())
            <p class="mabillon-success-message">Keine Datenqualitätsbefunde.</p>
        @else
            <ul class="mabillon-record-list">
                @for (var finding : report.findings())
                    <li><span><strong>${finding.ruleCode()} · ${finding.severity().name()}</strong><span>${finding.objectType()} ${finding.objectId()} · ${finding.message()}</span></span></li>
                @endfor
            </ul>
        @endif
    </section>
`)
