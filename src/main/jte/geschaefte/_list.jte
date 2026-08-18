@param guru.interlis.mabillon.query.SearchPage<guru.interlis.mabillon.geschaeft.GeschaeftView> searchPage
<section id="geschaeft-results" class="mabillon-tile mabillon-result-tile">
    <div class="mabillon-section-heading"><h2>Treffer</h2><span class="mabillon-count-badge">${searchPage.totalElements()}</span></div>
    @if (searchPage.items().isEmpty())
        <p class="mabillon-muted">Keine Geschäfte gefunden.</p>
    @else
        <div class="mabillon-table-wrap"><table><thead><tr><th>Nummer</th><th>Titel</th><th>Geschäftsart</th><th>Status</th><th>Lifecycle</th></tr></thead><tbody>
        @for (var business : searchPage.items())
            <tr><td><a href="/geschaefte/${business.number()}">${business.number()}</a></td><td>${business.title()}</td><td>${business.geschaeftsartCode() == null ? "—" : business.geschaeftsartCode()}</td><td>${business.processStatusName() == null ? "—" : business.processStatusName()}</td><td><span class="mabillon-status-badge">${business.lifecycleStatus()}</span></td></tr>
        @endfor
        </tbody></table></div>
    @endif
</section>
