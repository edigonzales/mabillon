@param guru.interlis.mabillon.geschaeft.GeschaeftskontrolleCriteria criteria
@param guru.interlis.mabillon.geschaeft.GeschaeftskontrolleView control
<div class="mabillon-page-header"><div><nav class="mabillon-breadcrumbs" aria-label="Brotkrümel">Leitungssicht</nav><h1>Geschäftskontrolle</h1><p class="mabillon-page-subtitle">Fristen, offene Arbeiten und inaktive Geschäfte auf einen Blick.</p></div></div>
<section class="mabillon-summary-grid" aria-label="Geschäftskontrolle">
    <div class="mabillon-summary-card"><span class="mabillon-summary-label">Offene Geschäfte</span><strong>${control.offeneGeschaefte().size()}</strong></div>
    <div class="mabillon-summary-card"><span class="mabillon-summary-label">Überfällige Geschäfte</span><strong>${control.ueberfaelligeGeschaefte().size()}</strong></div>
    <div class="mabillon-summary-card"><span class="mabillon-summary-label">Offene Aufgaben</span><strong>${control.offeneAufgaben().size()}</strong></div>
    <div class="mabillon-summary-card"><span class="mabillon-summary-label">Überfällige Aufgaben</span><strong>${control.ueberfaelligeAufgaben().size()}</strong></div>
</section>
<div class="mabillon-dashboard-grid">
    <section class="mabillon-tile"><div class="mabillon-section-heading"><h2>Überfällige Geschäfte</h2><span class="mabillon-count-badge">${control.ueberfaelligeGeschaefte().size()}</span></div>
        @if (control.ueberfaelligeGeschaefte().isEmpty())<p class="mabillon-muted">Keine überfälligen Geschäfte.</p>@else<ul class="mabillon-record-list">@for (var item : control.ueberfaelligeGeschaefte())<li><a href="/geschaefte/${item.number()}"><strong>${item.number()}</strong><span>${item.title()}</span></a></li>@endfor</ul>@endif
    </section>
    <section class="mabillon-tile"><div class="mabillon-section-heading"><h2>Überfällige Aufgaben</h2><span class="mabillon-count-badge">${control.ueberfaelligeAufgaben().size()}</span></div>
        @if (control.ueberfaelligeAufgaben().isEmpty())<p class="mabillon-muted">Keine überfälligen Aufgaben.</p>@else<ul class="mabillon-record-list">@for (var item : control.ueberfaelligeAufgaben())<li><a href="/geschaefte/${item.geschaeftsnummer()}"><strong>${item.title()}</strong><span>${item.geschaeftsnummer()} · ${item.dueDate().toString()}</span></a></li>@endfor</ul>@endif
    </section>
    <section class="mabillon-tile"><div class="mabillon-section-heading"><h2>Prozessstatus</h2><span class="mabillon-count-badge">${control.verteilungNachProzessstatus().size()}</span></div>
        <ul class="mabillon-record-list">@for (var entry : control.verteilungNachProzessstatus().entrySet())<li><span><strong>${entry.getKey()}</strong><span>${entry.getValue()} Geschäfte</span></span></li>@endfor</ul>
    </section>
    <section class="mabillon-tile"><div class="mabillon-section-heading"><h2>Inaktiv seit ${criteria.inactiveSinceDays()} Tagen</h2><span class="mabillon-count-badge">${control.inaktiveGeschaefte().size()}</span></div>
        @if (control.inaktiveGeschaefte().isEmpty())<p class="mabillon-muted">Keine inaktiven Geschäfte.</p>@else<ul class="mabillon-record-list">@for (var item : control.inaktiveGeschaefte())<li><a href="/geschaefte/${item.number()}"><strong>${item.number()}</strong><span>${item.title()}</span></a></li>@endfor</ul>@endif
    </section>
</div>
