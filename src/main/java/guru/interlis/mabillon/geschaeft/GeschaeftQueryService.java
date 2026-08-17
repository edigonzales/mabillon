package guru.interlis.mabillon.geschaeft;

import java.util.Optional;
import java.util.Comparator;
import java.util.List;

import guru.interlis.mabillon.numbering.GeschaeftNumber;
import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Geschaeft;
import guru.interlis.mabillon.query.SearchPage;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.stereotype.Service;

@Service
public final class GeschaeftQueryService {

    private final CayenneUnitOfWork unitOfWork;

    public GeschaeftQueryService(CayenneUnitOfWork unitOfWork) {
        this.unitOfWork = unitOfWork;
    }

    public Optional<GeschaeftView> findByNumber(String number) {
        return unitOfWork.read(context -> Optional.ofNullable(
                        ObjectSelect.query(Geschaeft.class)
                                .where(Geschaeft.GESCHAEFTSNUMMER.eq(number))
                                .selectFirst(context))
                .map(geschaeft -> new GeschaeftView(
                        geschaeft.getGeschaeftsnummer(),
                        geschaeft.getTitel(),
                        geschaeft.getKurzbeschreibung(),
                        geschaeft.getLifecyclestatus(),
                        geschaeft.getDossier() == null ? null : geschaeft.getDossier().getDossiernummer(),
                        geschaeft.getUnterlages().stream()
                                .map(unterlage -> new GeschaeftView.UnterlageSummary(
                                        unterlage.getTIliTid(),
                                        unterlage.getTitel(),
                                        unterlage.getDateiname(),
                                        unterlage.getAstatus(),
                                        isManagedContent(unterlage.getStorageuri())))
                                .toList(),
                        geschaeft.getGeschaeftsart() == null ? null : geschaeft.getGeschaeftsart().getAcode(),
                        geschaeft.getProzessstatus() == null ? null : geschaeft.getProzessstatus().getAcode(),
                        geschaeft.getProzessstatus() == null ? null : geschaeft.getProzessstatus().getAname(),
                        geschaeft.getResultatstatus() == null ? null : geschaeft.getResultatstatus().getAcode())));
    }

    public GeschaeftView get(GeschaeftNumber number) {
        return findByNumber(number.value())
                .orElseThrow(() -> new IllegalArgumentException("Unbekanntes Geschäft: " + number.value()));
    }

    public SearchPage<GeschaeftView> search(GeschaeftSearchCriteria criteria, int page, int size) {
        if (criteria == null) {
            criteria = GeschaeftSearchCriteria.empty();
        }
        final GeschaeftSearchCriteria filter = criteria;
        return unitOfWork.read(context -> {
            List<GeschaeftView> matches = ObjectSelect.query(Geschaeft.class).select(context).stream()
                    .filter(business -> contains(business.getGeschaeftsnummer(), filter.number()))
                    .filter(business -> containsIgnoreCase(business.getTitel(), filter.title()))
                    .filter(business -> filter.geschaeftsartCode() == null
                            || business.getGeschaeftsart() != null
                            && filter.geschaeftsartCode().equals(business.getGeschaeftsart().getAcode()))
                    .filter(business -> filter.processStatusCode() == null
                            || business.getProzessstatus() != null
                            && filter.processStatusCode().equals(business.getProzessstatus().getAcode()))
                    .filter(business -> filter.lifecycleStatus() == null
                            || filter.lifecycleStatus().equalsIgnoreCase(business.getLifecyclestatus()))
                    .filter(business -> filter.verantwortlicherUsername() == null
                            || business.getBenutzer() != null
                            && filter.verantwortlicherUsername().equals(business.getBenutzer().getUsername()))
                    .filter(business -> filter.organisationseinheitKuerzel() == null
                            || business.getOrganisationseinheit() != null
                            && filter.organisationseinheitKuerzel().equals(
                            business.getOrganisationseinheit().getKuerzel()))
                    .filter(business -> filter.dueFrom() == null || business.getFaelligam() != null
                            && !business.getFaelligam().isBefore(filter.dueFrom()))
                    .filter(business -> filter.dueTo() == null || business.getFaelligam() != null
                            && !business.getFaelligam().isAfter(filter.dueTo()))
                    .sorted(Comparator.comparing(Geschaeft::getGeschaeftsnummer))
                    .map(this::toView)
                    .toList();
            return page(matches, page, size);
        });
    }

    public List<GeschaeftView> activeForUser(String username, int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit muss positiv sein.");
        }
        return unitOfWork.read(context -> ObjectSelect.query(Geschaeft.class).select(context).stream()
                .filter(business -> business.getBenutzer() != null
                        && username.equals(business.getBenutzer().getUsername()))
                .filter(business -> !"Abgeschlossen".equalsIgnoreCase(business.getLifecyclestatus())
                        && !"Archiviert".equalsIgnoreCase(business.getLifecyclestatus())
                        && !"Vernichtet".equalsIgnoreCase(business.getLifecyclestatus()))
                .sorted(Comparator.comparing(Geschaeft::getGeschaeftsnummer).reversed())
                .limit(limit)
                .map(this::toView)
                .toList());
    }

    public List<GeschaeftView> recentlyChangedForUser(String username, int limit) {
        if (username == null || username.isBlank() || limit < 1) {
            throw new IllegalArgumentException("Benutzer und limit sind erforderlich.");
        }
        return unitOfWork.read(context -> ObjectSelect.query(Geschaeft.class).select(context).stream()
                .filter(business -> business.getBenutzer() != null
                        && username.equals(business.getBenutzer().getUsername()))
                .sorted(Comparator.comparing(Geschaeft::getGeaendertam,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .map(this::toView)
                .toList());
    }

    private GeschaeftView toView(Geschaeft geschaeft) {
        return new GeschaeftView(
                geschaeft.getGeschaeftsnummer(), geschaeft.getTitel(), geschaeft.getKurzbeschreibung(),
                geschaeft.getLifecyclestatus(), geschaeft.getDossier() == null ? null : geschaeft.getDossier().getDossiernummer(),
                geschaeft.getUnterlages().stream()
                        .map(unterlage -> new GeschaeftView.UnterlageSummary(
                                unterlage.getTIliTid(), unterlage.getTitel(), unterlage.getDateiname(), unterlage.getAstatus(),
                                isManagedContent(unterlage.getStorageuri())))
                        .toList(),
                geschaeft.getGeschaeftsart() == null ? null : geschaeft.getGeschaeftsart().getAcode(),
                geschaeft.getProzessstatus() == null ? null : geschaeft.getProzessstatus().getAcode(),
                geschaeft.getProzessstatus() == null ? null : geschaeft.getProzessstatus().getAname(),
                geschaeft.getResultatstatus() == null ? null : geschaeft.getResultatstatus().getAcode());
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
