package guru.interlis.mabillon.geschaeft;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import guru.interlis.mabillon.numbering.GeschaeftNumber;
import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Benutzer;
import guru.interlis.mabillon.persistence.cayenne.Geschaeft;
import guru.interlis.mabillon.persistence.cayenne.Geschaeftsart;
import guru.interlis.mabillon.persistence.cayenne.Organisationseinheit;
import guru.interlis.mabillon.persistence.cayenne.Prozessstatus;
import guru.interlis.mabillon.query.SearchPage;
import org.apache.cayenne.exp.Expression;
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
        requirePage(page, size);
        GeschaeftSearchCriteria filter = criteria == null ? GeschaeftSearchCriteria.empty() : criteria;
        return unitOfWork.read(context -> {
            ObjectSelect<Geschaeft> query = searchQuery(filter);
            long total = query.selectCount(context);
            long offset = (long) page * size;
            if (offset >= total) {
                return new SearchPage<>(List.of(), page, size, total);
            }
            List<GeschaeftView> items = query
                    .orderBy(Geschaeft.GESCHAEFTSNUMMER.asc())
                    .offset(Math.toIntExact(offset))
                    .limit(size)
                    .select(context).stream()
                    .map(this::toView)
                    .toList();
            return new SearchPage<>(items, page, size, total);
        });
    }

    public List<GeschaeftView> activeForUser(String username, int limit) {
        if (username == null || username.isBlank() || limit < 1) {
            throw new IllegalArgumentException("Benutzer und limit sind erforderlich.");
        }
        return unitOfWork.read(context -> ObjectSelect.query(Geschaeft.class)
                .where(Geschaeft.BENUTZER.dot(Benutzer.USERNAME).eq(username))
                .and(Geschaeft.LIFECYCLESTATUS.nlikeIgnoreCase("Abgeschlossen"))
                .and(Geschaeft.LIFECYCLESTATUS.nlikeIgnoreCase("Archiviert"))
                .and(Geschaeft.LIFECYCLESTATUS.nlikeIgnoreCase("Vernichtet"))
                .orderBy(Geschaeft.GESCHAEFTSNUMMER.desc())
                .limit(limit)
                .select(context).stream()
                .map(this::toView)
                .toList());
    }

    public List<GeschaeftView> recentlyChangedForUser(String username, int limit) {
        if (username == null || username.isBlank() || limit < 1) {
            throw new IllegalArgumentException("Benutzer und limit sind erforderlich.");
        }
        return unitOfWork.read(context -> {
            List<Geschaeft> values = new ArrayList<>(ObjectSelect.query(Geschaeft.class)
                    .where(Geschaeft.BENUTZER.dot(Benutzer.USERNAME).eq(username))
                    .and(Geschaeft.GEAENDERTAM.isNotNull())
                    .orderBy(Geschaeft.GEAENDERTAM.desc())
                    .limit(limit)
                    .select(context));
            if (values.size() < limit) {
                values.addAll(ObjectSelect.query(Geschaeft.class)
                        .where(Geschaeft.BENUTZER.dot(Benutzer.USERNAME).eq(username))
                        .and(Geschaeft.GEAENDERTAM.isNull())
                        .orderBy(Geschaeft.GESCHAEFTSNUMMER.asc())
                        .limit(limit - values.size())
                        .select(context));
            }
            return values.stream().map(this::toView).toList();
        });
    }

    private static ObjectSelect<Geschaeft> searchQuery(GeschaeftSearchCriteria filter) {
        ObjectSelect<Geschaeft> query = ObjectSelect.query(Geschaeft.class);
        addFilter(query, filter.number() == null ? null : Geschaeft.GESCHAEFTSNUMMER.contains(filter.number()));
        addFilter(query, filter.title() == null ? null : Geschaeft.TITEL.containsIgnoreCase(filter.title()));
        addFilter(query, filter.geschaeftsartCode() == null ? null
                : Geschaeft.GESCHAEFTSART.dot(Geschaeftsart.ACODE).eq(filter.geschaeftsartCode()));
        addFilter(query, filter.processStatusCode() == null ? null
                : Geschaeft.PROZESSSTATUS.dot(Prozessstatus.ACODE).eq(filter.processStatusCode()));
        addFilter(query, filter.lifecycleStatus() == null ? null
                : Geschaeft.LIFECYCLESTATUS.likeIgnoreCase(filter.lifecycleStatus()));
        addFilter(query, filter.verantwortlicherUsername() == null ? null
                : Geschaeft.BENUTZER.dot(Benutzer.USERNAME).eq(filter.verantwortlicherUsername()));
        addFilter(query, filter.organisationseinheitKuerzel() == null ? null
                : Geschaeft.ORGANISATIONSEINHEIT.dot(Organisationseinheit.KUERZEL)
                        .eq(filter.organisationseinheitKuerzel()));
        addFilter(query, filter.dueFrom() == null ? null : Geschaeft.FAELLIGAM.gte(filter.dueFrom()));
        addFilter(query, filter.dueTo() == null ? null : Geschaeft.FAELLIGAM.lte(filter.dueTo()));
        return query;
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
