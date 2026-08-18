@param guru.interlis.mabillon.query.SearchPage<guru.interlis.mabillon.dossier.DossierView> searchPage
<section id="dossier-results" class="mabillon-tile mabillon-result-tile">
    <div class="mabillon-section-heading"><h2>Treffer</h2><span class="mabillon-count-badge">${searchPage.totalElements()}</span></div>
    @if (searchPage.items().isEmpty())
        <p class="mabillon-muted">Keine Dossiers gefunden.</p>
    @else
        <div class="mabillon-table-wrap"><table><thead><tr><th>Nummer</th><th>Titel</th><th>Status</th><th>Geschäfte</th></tr></thead><tbody>
        @for (var dossier : searchPage.items())
            <tr><td><a href="/dossiers/${dossier.number()}">${dossier.number()}</a></td><td>${dossier.title()}</td><td><span class="mabillon-status-badge">${dossier.status()}</span></td><td>${dossier.geschaefte().size()}</td></tr>
        @endfor
        </tbody></table></div>
    @endif
</section>
