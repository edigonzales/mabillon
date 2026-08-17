package guru.interlis.mabillon.aufgabe;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import guru.interlis.mabillon.numbering.GeschaeftNumber;
import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Aufgabe;
import guru.interlis.mabillon.persistence.cayenne.Geschaeft;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.stereotype.Service;

@Service
public final class AufgabeQueryService {

    private final CayenneUnitOfWork unitOfWork;

    public AufgabeQueryService(CayenneUnitOfWork unitOfWork) {
        this.unitOfWork = unitOfWork;
    }

    public AufgabeView get(UUID tid) {
        return unitOfWork.read(context -> {
            Aufgabe task = ObjectSelect.query(Aufgabe.class)
                    .where(Aufgabe.T_ILI_TID.eq(tid))
                    .selectFirst(context);
            if (task == null) {
                throw new IllegalArgumentException("Unbekannte Aufgabe: " + tid);
            }
            return toView(task);
        });
    }

    public List<AufgabeView> forGeschaeft(GeschaeftNumber number) {
        return unitOfWork.read(context -> {
            Geschaeft business = ObjectSelect.query(Geschaeft.class)
                    .where(Geschaeft.GESCHAEFTSNUMMER.eq(number.value())).selectFirst(context);
            if (business == null) {
                throw new IllegalArgumentException("Unbekanntes Geschäft: " + number.value());
            }
            return business.getAufgabes().stream()
                    .sorted(Comparator.comparing(Aufgabe::getFaelligam,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(Aufgabe::getTitel))
                    .map(this::toView)
                    .toList();
        });
    }

    public List<AufgabeView> myOpenTasks(String username, int limit) {
        if (username == null || username.isBlank() || limit < 1) {
            throw new IllegalArgumentException("Benutzer und limit sind erforderlich.");
        }
        return unitOfWork.read(context -> ObjectSelect.query(Aufgabe.class).select(context).stream()
                .filter(task -> task.getBenutzer() != null && username.equals(task.getBenutzer().getUsername()))
                .filter(this::isOpen)
                .sorted(taskComparator())
                .limit(limit)
                .map(this::toView)
                .toList());
    }

    public List<AufgabeView> overdueForUser(String username, LocalDate today, int limit) {
        if (username == null || username.isBlank() || today == null || limit < 1) {
            throw new IllegalArgumentException("Benutzer, Datum und limit sind erforderlich.");
        }
        return unitOfWork.read(context -> ObjectSelect.query(Aufgabe.class).select(context).stream()
                .filter(task -> task.getBenutzer() != null && username.equals(task.getBenutzer().getUsername()))
                .filter(this::isOpen)
                .filter(task -> task.getFaelligam() != null && task.getFaelligam().isBefore(today))
                .sorted(taskComparator())
                .limit(limit)
                .map(this::toView)
                .toList());
    }

    private boolean isOpen(Aufgabe task) {
        return !"Erledigt".equalsIgnoreCase(task.getAstatus())
                && !"Abgebrochen".equalsIgnoreCase(task.getAstatus());
    }

    private Comparator<Aufgabe> taskComparator() {
        return Comparator.comparing(Aufgabe::getFaelligam, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Comparator.comparing(Aufgabe::getPrioritaet,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .thenComparing(Aufgabe::getTitel);
    }

    private AufgabeView toView(Aufgabe value) {
        return new AufgabeView(
                value.getTIliTid(),
                value.getGeschaeft() == null ? null : value.getGeschaeft().getGeschaeftsnummer(),
                value.getTitel(), value.getBeschreibung(),
                value.getAufgabentyp() == null ? null : value.getAufgabentyp().getAcode(),
                value.getAufgabentyp() == null ? null : value.getAufgabentyp().getAname(),
                value.getAstatus(), value.getFaelligam(), value.getPrioritaet(), value.getErstelltam(),
                value.getErledigtam(), value.getBenutzer() == null ? null : value.getBenutzer().getUsername(),
                value.getOrganisationseinheit() == null ? null : value.getOrganisationseinheit().getKuerzel());
    }
}
