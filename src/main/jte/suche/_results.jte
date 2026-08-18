@param guru.interlis.mabillon.search.GlobalSearchResult result
<section class="mabillon-tile">
    <div class="mabillon-section-heading"><h2>Treffer</h2><span class="mabillon-count-badge">${result.totalElements()}</span></div>
    @if (result.items().isEmpty())
        <p class="mabillon-muted">Keine Treffer.</p>
    @else
        <ul class="mabillon-record-list">
            @for (var hit : result.items())
                <li><a href="${hit.href()}"><strong>${hit.title()}</strong><span>${hit.objectType()} · ${hit.identifier()}${hit.context() == null ? "" : " · " + hit.context()}</span></a></li>
            @endfor
        </ul>
    @endif
    @if (result.page() > 0 || result.hasNext())
        <nav class="mabillon-pagination" aria-label="Suchseiten">
            @if (result.page() > 0)<a href="/suche?page=${result.page() - 1}">Zurück</a>@endif
            <span>Seite ${result.page() + 1}</span>
            @if (result.hasNext())<a href="/suche?page=${result.page() + 1}">Weiter</a>@endif
        </nav>
    @endif
</section>
