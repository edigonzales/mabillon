package guru.interlis.mabillon.geschaeft;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import guru.interlis.mabillon.aufgabe.AufgabeView;
import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Aufgabe;
import guru.interlis.mabillon.persistence.cayenne.Geschaeft;
import guru.interlis.mabillon.persistence.cayenne.Prozessstatus;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.stereotype.Service;

@Service
public final class GeschaeftskontrolleQueryService {

    private final CayenneUnitOfWork unitOfWork;

    public GeschaeftskontrolleQueryService(CayenneUnitOfWork unitOfWork) {
        this.unitOfWork = unitOfWork;
    }

    public GeschaeftskontrolleView load(GeschaeftskontrolleCriteria criteria) {
        return unitOfWork.read(context -> {
            List<GeschaeftView> openViews = openBusinesses()
                    .orderBy(Geschaeft.GESCHAEFTSNUMMER.asc())
                    .limit(criteria.limit())
                    .select(context).stream()
                    .map(this::toBusinessView)
                    .toList();

            List<GeschaeftView> overdueBusinesses = openBusinesses()
                    .and(Geschaeft.FAELLIGAM.lt(criteria.today()))
                    .orderBy(Geschaeft.FAELLIGAM.asc())
                    .limit(criteria.limit())
                    .select(context).stream()
                    .map(this::toBusinessView)
                    .toList();

            List<AufgabeView> openTasks = openTasks()
                    .orderBy(Aufgabe.FAELLIGAM.asc())
                    .orderBy(Aufgabe.PRIORITAET.desc())
                    .orderBy(Aufgabe.TITEL.asc())
                    .limit(criteria.limit())
                    .select(context).stream()
                    .map(this::toTaskView)
                    .toList();

            List<AufgabeView> overdueTasks = openTasks()
                    .and(Aufgabe.FAELLIGAM.lt(criteria.today()))
                    .orderBy(Aufgabe.FAELLIGAM.asc())
                    .orderBy(Aufgabe.PRIORITAET.desc())
                    .orderBy(Aufgabe.TITEL.asc())
                    .limit(criteria.limit())
                    .select(context).stream()
                    .map(this::toTaskView)
                    .toList();

            Map<String, Long> processStatusCounts = processStatusCounts(context);
            List<GeschaeftView> inactive = inactiveBusinesses(context, criteria).stream()
                    .map(this::toBusinessView)
                    .toList();

            return new GeschaeftskontrolleView(openViews, overdueBusinesses, openTasks, overdueTasks,
                    processStatusCounts, inactive);
        });
    }

    private Map<String, Long> processStatusCounts(org.apache.cayenne.ObjectContext context) {
        var statusCode = Geschaeft.PROZESSSTATUS.dot(Prozessstatus.ACODE);
        List<Object[]> rows = ObjectSelect.columnQuery(Geschaeft.class, statusCode, statusCode.count())
                .where(openBusinessExpression())
                .select(context);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Object[] row : rows) {
            counts.put((String) row[0], (Long) row[1]);
        }
        long unknown = ObjectSelect.query(Geschaeft.class)
                .where(openBusinessExpression())
                .and(Geschaeft.PROZESSSTATUS.isNull())
                .selectCount(context);
        if (unknown > 0) {
            counts.put("UNBEKANNT", unknown);
        }
        return counts;
    }

    private List<Geschaeft> inactiveBusinesses(
            org.apache.cayenne.ObjectContext context,
            GeschaeftskontrolleCriteria criteria) {
        LocalDate inactiveSince = criteria.today().minusDays(criteria.inactiveSinceDays());
        LocalDateTime boundary = inactiveSince.atStartOfDay();
        List<Geschaeft> candidates = new ArrayList<>(openBusinesses()
                .and(Geschaeft.GEAENDERTAM.isNotNull())
                .and(Geschaeft.GEAENDERTAM.lt(boundary))
                .orderBy(Geschaeft.GEAENDERTAM.asc())
                .limit(criteria.limit())
                .select(context));
        candidates.addAll(openBusinesses()
                .and(Geschaeft.GEAENDERTAM.isNull())
                .and(Geschaeft.ERSTELLTAM.lt(boundary))
                .orderBy(Geschaeft.ERSTELLTAM.asc())
                .limit(criteria.limit())
                .select(context));
        return candidates.stream()
                .sorted(Comparator.comparing(this::lastChangedDate))
                .limit(criteria.limit())
                .toList();
    }

    private static ObjectSelect<Geschaeft> openBusinesses() {
        return ObjectSelect.query(Geschaeft.class).where(openBusinessExpression());
    }

    private static Expression openBusinessExpression() {
        return ExpressionFactory.and(
                Geschaeft.LIFECYCLESTATUS.nlikeIgnoreCase("Abgeschlossen"),
                Geschaeft.LIFECYCLESTATUS.nlikeIgnoreCase("Archiviert"),
                Geschaeft.LIFECYCLESTATUS.nlikeIgnoreCase("Vernichtet"));
    }

    private static ObjectSelect<Aufgabe> openTasks() {
        return ObjectSelect.query(Aufgabe.class)
                .where(Aufgabe.ASTATUS.nlikeIgnoreCase("Erledigt"))
                .and(Aufgabe.ASTATUS.nlikeIgnoreCase("Abgebrochen"));
    }

    private LocalDate lastChangedDate(Geschaeft business) {
        LocalDateTime changed = business.getGeaendertam();
        return changed == null ? business.getErstelltam().toLocalDate() : changed.toLocalDate();
    }

    private GeschaeftView toBusinessView(Geschaeft business) {
        return new GeschaeftView(
                business.getGeschaeftsnummer(), business.getTitel(), business.getKurzbeschreibung(),
                business.getLifecyclestatus(), business.getDossier() == null ? null : business.getDossier().getDossiernummer(),
                business.getUnterlages().stream()
                        .map(document -> new GeschaeftView.UnterlageSummary(
                                document.getTIliTid(), document.getTitel(), document.getDateiname(), document.getAstatus()))
                        .toList(),
                business.getGeschaeftsart() == null ? null : business.getGeschaeftsart().getAcode(),
                business.getProzessstatus() == null ? null : business.getProzessstatus().getAcode(),
                business.getProzessstatus() == null ? null : business.getProzessstatus().getAname(),
                business.getResultatstatus() == null ? null : business.getResultatstatus().getAcode());
    }

    private AufgabeView toTaskView(Aufgabe value) {
        return new AufgabeView(
                value.getTIliTid(), value.getGeschaeft() == null ? null : value.getGeschaeft().getGeschaeftsnummer(),
                value.getTitel(), value.getBeschreibung(),
                value.getAufgabentyp() == null ? null : value.getAufgabentyp().getAcode(),
                value.getAufgabentyp() == null ? null : value.getAufgabentyp().getAname(), value.getAstatus(),
                value.getFaelligam(), value.getPrioritaet(), value.getErstelltam(), value.getErledigtam(),
                value.getBenutzer() == null ? null : value.getBenutzer().getUsername(),
                value.getOrganisationseinheit() == null ? null : value.getOrganisationseinheit().getKuerzel());
    }
}
