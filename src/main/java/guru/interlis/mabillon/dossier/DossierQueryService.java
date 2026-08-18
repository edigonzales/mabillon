package guru.interlis.mabillon.dossier;

import java.util.List;
import java.util.Optional;

import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Dossier;
import guru.interlis.mabillon.persistence.cayenne.Ordnungssystemposition;
import guru.interlis.mabillon.persistence.cayenne.Organisationseinheit;
import guru.interlis.mabillon.query.SearchPage;
import org.apache.cayenne.exp.Expression;
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
        requirePage(page, size);
        DossierSearchCriteria filter = criteria == null ? DossierSearchCriteria.empty() : criteria;
        return unitOfWork.read(context -> {
            ObjectSelect<Dossier> query = ObjectSelect.query(Dossier.class);
            addFilter(query, filter.number() == null ? null : Dossier.DOSSIERNUMMER.contains(filter.number()));
            addFilter(query, filter.title() == null ? null : Dossier.TITEL.containsIgnoreCase(filter.title()));
            addFilter(query, filter.status() == null ? null : Dossier.ASTATUS.likeIgnoreCase(filter.status()));
            addFilter(query, filter.registraturplanPositionCode() == null ? null
                    : Dossier.ORDNUNGSSYSTEMPOSITION.dot(Ordnungssystemposition.ACODE)
                            .eq(filter.registraturplanPositionCode()));
            addFilter(query, filter.federfuehrungKuerzel() == null ? null
                    : Dossier.ORGANISATIONSEINHEIT.dot(Organisationseinheit.KUERZEL)
                            .eq(filter.federfuehrungKuerzel()));
            addFilter(query, filter.openedFrom() == null ? null : Dossier.EROEFFNETAM.gte(filter.openedFrom()));
            addFilter(query, filter.openedTo() == null ? null : Dossier.EROEFFNETAM.lte(filter.openedTo()));
            addFilter(query, filter.closedFrom() == null ? null : Dossier.GESCHLOSSENAM.gte(filter.closedFrom()));
            addFilter(query, filter.closedTo() == null ? null : Dossier.GESCHLOSSENAM.lte(filter.closedTo()));

            long total = query.selectCount(context);
            long offset = (long) page * size;
            if (offset >= total) {
                return new SearchPage<>(List.of(), page, size, total);
            }

            List<DossierView> items = query
                    .orderBy(Dossier.DOSSIERNUMMER.asc())
                    .offset(Math.toIntExact(offset))
                    .limit(size)
                    .select(context).stream()
                    .map(this::toView)
                    .toList();
            return new SearchPage<>(items, page, size, total);
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

    private static <T> void addFilter(ObjectSelect<T> query, Expression expression) {
        if (expression == null) {
            return;
        }
        if (query.getWhere() == null) {
            query.where(expression);
        } else {
            query.and(expression);
        }
    }

    private static void requirePage(int page, int size) {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("Ungültige Seitendaten.");
        }
    }

    private static boolean isManagedContent(String storageUri) {
        return storageUri != null && storageUri.startsWith("mabillon:objects/");
    }
}
