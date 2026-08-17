package guru.interlis.mabillon.geschaeft;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import guru.interlis.mabillon.aufgabe.AufgabeView;
import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Aufgabe;
import guru.interlis.mabillon.persistence.cayenne.Geschaeft;
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
            List<Geschaeft> businesses = ObjectSelect.query(Geschaeft.class).select(context);
            List<Aufgabe> tasks = ObjectSelect.query(Aufgabe.class).select(context);
            Predicate<Geschaeft> open = business -> !isClosed(business);
            List<GeschaeftView> openViews = businesses.stream()
                    .filter(open)
                    .sorted(Comparator.comparing(Geschaeft::getGeschaeftsnummer))
                    .limit(criteria.limit())
                    .map(this::toBusinessView)
                    .toList();
            List<GeschaeftView> overdueBusinesses = businesses.stream()
                    .filter(open)
                    .filter(business -> business.getFaelligam() != null
                            && business.getFaelligam().isBefore(criteria.today()))
                    .sorted(Comparator.comparing(Geschaeft::getFaelligam))
                    .limit(criteria.limit())
                    .map(this::toBusinessView)
                    .toList();
            List<AufgabeView> openTasks = tasks.stream()
                    .filter(this::isOpen)
                    .sorted(taskComparator())
                    .limit(criteria.limit())
                    .map(this::toTaskView)
                    .toList();
            List<AufgabeView> overdueTasks = tasks.stream()
                    .filter(this::isOpen)
                    .filter(task -> task.getFaelligam() != null && task.getFaelligam().isBefore(criteria.today()))
                    .sorted(taskComparator())
                    .limit(criteria.limit())
                    .map(this::toTaskView)
                    .toList();
            Map<String, Long> processStatusCounts = businesses.stream()
                    .filter(open)
                    .collect(Collectors.groupingBy(
                            business -> business.getProzessstatus() == null
                                    ? "UNBEKANNT" : business.getProzessstatus().getAcode(),
                            Collectors.counting()));
            LocalDate inactiveSince = criteria.today().minusDays(criteria.inactiveSinceDays());
            List<GeschaeftView> inactive = businesses.stream()
                    .filter(open)
                    .filter(business -> lastChangedDate(business) != null
                            && lastChangedDate(business).isBefore(inactiveSince))
                    .sorted(Comparator.comparing(this::lastChangedDate))
                    .limit(criteria.limit())
                    .map(this::toBusinessView)
                    .toList();
            return new GeschaeftskontrolleView(openViews, overdueBusinesses, openTasks, overdueTasks,
                    processStatusCounts, inactive);
        });
    }

    private boolean isClosed(Geschaeft business) {
        return "Abgeschlossen".equalsIgnoreCase(business.getLifecyclestatus())
                || "Archiviert".equalsIgnoreCase(business.getLifecyclestatus())
                || "Vernichtet".equalsIgnoreCase(business.getLifecyclestatus());
    }

    private boolean isOpen(Aufgabe task) {
        return !"Erledigt".equalsIgnoreCase(task.getAstatus())
                && !"Abgebrochen".equalsIgnoreCase(task.getAstatus());
    }

    private LocalDate lastChangedDate(Geschaeft business) {
        LocalDateTime changed = business.getGeaendertam();
        return changed == null ? business.getErstelltam().toLocalDate() : changed.toLocalDate();
    }

    private Comparator<Aufgabe> taskComparator() {
        return Comparator.comparing(Aufgabe::getFaelligam, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Comparator.comparing(Aufgabe::getPrioritaet,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .thenComparing(Aufgabe::getTitel);
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
