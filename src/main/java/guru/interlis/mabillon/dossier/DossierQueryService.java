package guru.interlis.mabillon.dossier;

import java.util.Optional;
import java.util.Comparator;
import java.util.List;

import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Dossier;
import guru.interlis.mabillon.query.SearchPage;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.stereotype.Service;

@Service
public final class DossierQueryService {

    private final CayenneUnitOfWork unitOfWork;

    public DossierQueryService(CayenneUnitOfWork unitOfWork) {
        this.unitOfWork = unitOfWork;
    }

    public Optional<DossierView> findByNumber(String number) {
        return unitOfWork.read(context -> Optional.ofNullable(
                        ObjectSelect.query(Dossier.class)
                                .where(Dossier.DOSSIERNUMMER.eq(number))
                                .selectFirst(context))
                .map(dossier -> new DossierView(
                        dossier.getDossiernummer(),
                        dossier.getTitel(),
                        dossier.getBeschreibung(),
                        dossier.getAstatus(),
                        dossier.getGeschaefts().stream()
                                .map(geschaeft -> new DossierView.GeschaeftSummary(
                                        geschaeft.getGeschaeftsnummer(),
                                        geschaeft.getTitel(),
                                        geschaeft.getLifecyclestatus()))
                                .toList(),
                        dossier.getUnterlages().stream()
                                .map(unterlage -> new DossierView.UnterlageSummary(
                                        unterlage.getTIliTid(),
                                        unterlage.getTitel(),
                                        unterlage.getDateiname(),
                                        unterlage.getAstatus(),
                                        isManagedContent(unterlage.getStorageuri())))
                                .toList())));
    }

    public DossierView get(DossierNumber number) {
        return findByNumber(number.value())
                .orElseThrow(() -> new IllegalArgumentException("Unbekanntes Dossier: " + number.value()));
    }

    public SearchPage<DossierView> search(DossierSearchCriteria criteria, int page, int size) {
        if (criteria == null) {
            criteria = DossierSearchCriteria.empty();
        }
        final DossierSearchCriteria filter = criteria;
        return unitOfWork.read(context -> {
            List<DossierView> matches = ObjectSelect.query(Dossier.class).select(context).stream()
                    .filter(dossier -> contains(dossier.getDossiernummer(), filter.number()))
                    .filter(dossier -> containsIgnoreCase(dossier.getTitel(), filter.title()))
                    .filter(dossier -> filter.status() == null || filter.status().equalsIgnoreCase(dossier.getAstatus()))
                    .filter(dossier -> filter.registraturplanPositionCode() == null
                            || dossier.getOrdnungssystemposition() != null
                            && filter.registraturplanPositionCode().equals(dossier.getOrdnungssystemposition().getAcode()))
                    .filter(dossier -> filter.federfuehrungKuerzel() == null
                            || dossier.getOrganisationseinheit() != null
                            && filter.federfuehrungKuerzel().equals(dossier.getOrganisationseinheit().getKuerzel()))
                    .filter(dossier -> filter.openedFrom() == null || !dossier.getEroeffnetam().isBefore(filter.openedFrom()))
                    .filter(dossier -> filter.openedTo() == null || !dossier.getEroeffnetam().isAfter(filter.openedTo()))
                    .filter(dossier -> filter.closedFrom() == null || dossier.getGeschlossenam() != null
                            && !dossier.getGeschlossenam().isBefore(filter.closedFrom()))
                    .filter(dossier -> filter.closedTo() == null || dossier.getGeschlossenam() != null
                            && !dossier.getGeschlossenam().isAfter(filter.closedTo()))
                    .sorted(Comparator.comparing(Dossier::getDossiernummer))
                    .map(this::toView)
                    .toList();
            return page(matches, page, size);
        });
    }

    private DossierView toView(Dossier dossier) {
        return new DossierView(
                dossier.getDossiernummer(), dossier.getTitel(), dossier.getBeschreibung(), dossier.getAstatus(),
                dossier.getGeschaefts().stream()
                        .map(business -> new DossierView.GeschaeftSummary(
                                business.getGeschaeftsnummer(), business.getTitel(), business.getLifecyclestatus()))
                        .toList(),
                dossier.getUnterlages().stream()
                        .map(document -> new DossierView.UnterlageSummary(
                                document.getTIliTid(), document.getTitel(), document.getDateiname(), document.getAstatus(),
                                isManagedContent(document.getStorageuri())))
                        .toList());
    }

    private static boolean contains(String value, String filter) {
        return filter == null || (value != null && value.contains(filter));
    }

    private static boolean isManagedContent(String storageUri) {
        return storageUri != null && storageUri.startsWith("mabillon:objects/");
    }

    private static boolean containsIgnoreCase(String value, String filter) {
        return filter == null || (value != null && value.toLowerCase(java.util.Locale.ROOT)
                .contains(filter.toLowerCase(java.util.Locale.ROOT)));
    }

    private static <T> SearchPage<T> page(List<T> values, int page, int size) {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("Ungültige Seitendaten.");
        }
        int from = Math.min(page * size, values.size());
        int to = Math.min(from + size, values.size());
        return new SearchPage<>(values.subList(from, to), page, size, values.size());
    }
}
