@param guru.interlis.mabillon.search.GlobalSearchResult result
<section class="workspace-card">
    <div class="section-heading"><h2>Treffer</h2><span class="count-badge">${result.totalElements()}</span></div>
    @if (result.items().isEmpty())
        <p class="muted">Keine Treffer.</p>
    @else
        <ul class="record-list">
            @for (var hit : result.items())
                <li><a href="${hit.href()}"><strong>${hit.title()}</strong><span>${hit.objectType()} · ${hit.identifier()}${hit.context() == null ? "" : " · " + hit.context()}</span></a></li>
            @endfor
        </ul>
    @endif
    @if (result.page() > 0 || result.hasNext())
        <nav class="pagination" aria-label="Suchseiten">
            @if (result.page() > 0)<a href="/suche?page=${result.page() - 1}">Zurück</a>@endif
            <span>Seite ${result.page() + 1}</span>
            @if (result.hasNext())<a href="/suche?page=${result.page() + 1}">Weiter</a>@endif
        </nav>
    @endif
</section>
