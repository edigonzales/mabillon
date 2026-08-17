package guru.interlis.mabillon.aufgabe;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import guru.interlis.mabillon.domain.DomainRuleViolationException;
import guru.interlis.mabillon.journal.EreignisObjektTyp;
import guru.interlis.mabillon.journal.EreignisTyp;
import guru.interlis.mabillon.journal.JournalCommand;
import guru.interlis.mabillon.journal.JournalService;
import guru.interlis.mabillon.numbering.GeschaeftNumber;
import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Aufgabe;
import guru.interlis.mabillon.persistence.cayenne.Aufgabentyp;
import guru.interlis.mabillon.persistence.cayenne.Benutzer;
import guru.interlis.mabillon.persistence.cayenne.Dossier;
import guru.interlis.mabillon.persistence.cayenne.Geschaeft;
import guru.interlis.mabillon.persistence.cayenne.Organisationseinheit;
import guru.interlis.mabillon.security.AuthorizationService;
import guru.interlis.mabillon.security.CurrentActor;
import guru.interlis.mabillon.security.Permission;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.stereotype.Service;

@Service
public final class AufgabeService {

    private static final String ACTIVE = "aktiv";
    private static final String OPEN = "Offen";
    private static final String IN_PROGRESS = "In_Arbeit";
    private static final String DELEGATED = "Delegiert";
    private static final String COMPLETED = "Erledigt";
    private static final String CANCELLED = "Abgebrochen";

    private final CayenneUnitOfWork unitOfWork;
    private final JournalService journalService;
    private final AuthorizationService authorizationService;
    private final CurrentActor currentActor;
    private final Clock clock;

    public AufgabeService(
            CayenneUnitOfWork unitOfWork,
            JournalService journalService,
            AuthorizationService authorizationService,
            CurrentActor currentActor,
            Clock clock) {
        this.unitOfWork = unitOfWork;
        this.journalService = journalService;
        this.authorizationService = authorizationService;
        this.currentActor = currentActor;
        this.clock = clock;
    }

    public AufgabeView create(CreateAufgabeCommand command) {
        authorizationService.require(Permission.EDIT_AUFGABE);
        return unitOfWork.write(context -> {
            Geschaeft business = findBusiness(context, command.geschaeftNumber());
            requireOpenBusiness(business);
            Aufgabentyp type = findActiveType(context, command.typCode());
            Benutzer user = findActiveUser(context, command.assignedUsername());
            Organisationseinheit organisationseinheit = findActiveOrganisationseinheit(
                    context, command.assignedOrganisationseinheit());
            Aufgabe value = context.newObject(Aufgabe.class);
            value.setTitel(command.title().trim());
            value.setBeschreibung(command.description());
            value.setAufgabentyp(type);
            value.setAstatus(OPEN);
            value.setPrioritaet(command.priority() == null ? 0 : command.priority());
            value.setErstelltam(LocalDateTime.now(clock));
            value.setFaelligam(command.dueDate());
            value.setBenutzer(user);
            value.setOrganisationseinheit(organisationseinheit);
            value.setGeschaeft(business);
            value.setTBasket(business.getTBasket());
            value.setTIliTid(UUID.randomUUID());
            record(value, EreignisTyp.Aufgabe_erstellt, "Aufgabe erstellt.", context);
            return toView(value);
        });
    }

    public AufgabeView update(UpdateAufgabeCommand command) {
        authorizationService.require(Permission.EDIT_AUFGABE);
        return unitOfWork.write(context -> {
            Aufgabe value = requireTask(context, command.tid());
            requireOpenTask(value);
            value.setTitel(command.title().trim());
            value.setBeschreibung(command.description());
            value.setFaelligam(command.dueDate());
            value.setPrioritaet(command.priority() == null ? 0 : command.priority());
            record(value, EreignisTyp.Geaendert, "Aufgabe geändert.", context);
            return toView(value);
        });
    }

    public AufgabeView start(UUID tid) {
        authorizationService.require(Permission.EDIT_AUFGABE);
        return unitOfWork.write(context -> {
            Aufgabe value = requireTask(context, tid);
            requireOpenTask(value);
            if (COMPLETED.equals(value.getAstatus()) || CANCELLED.equals(value.getAstatus())) {
                throw new DomainRuleViolationException("Abgeschlossene Aufgaben können nicht gestartet werden.");
            }
            value.setAstatus(IN_PROGRESS);
            record(value, EreignisTyp.Status_geaendert, "Aufgabe in Bearbeitung genommen.", context);
            return toView(value);
        });
    }

    public AufgabeView complete(CompleteAufgabeCommand command) {
        authorizationService.require(Permission.EDIT_AUFGABE);
        return unitOfWork.write(context -> {
            Aufgabe value = requireTask(context, command.tid());
            requireOpenTask(value);
            value.setAstatus(COMPLETED);
            value.setErledigtam(LocalDateTime.now(clock));
            record(value, EreignisTyp.Aufgabe_erledigt, remark("Aufgabe erledigt", command.comment()), context);
            return toView(value);
        });
    }

    public AufgabeView cancel(CancelAufgabeCommand command) {
        authorizationService.require(Permission.EDIT_AUFGABE);
        return unitOfWork.write(context -> {
            Aufgabe value = requireTask(context, command.tid());
            requireOpenTask(value);
            value.setAstatus(CANCELLED);
            value.setErledigtam(LocalDateTime.now(clock));
            record(value, EreignisTyp.Geaendert, remark("Aufgabe abgebrochen", command.comment()), context);
            return toView(value);
        });
    }

    public AufgabeView delegate(DelegateAufgabeCommand command) {
        authorizationService.require(Permission.EDIT_AUFGABE);
        return unitOfWork.write(context -> {
            Aufgabe value = requireTask(context, command.tid());
            requireOpenTask(value);
            value.setBenutzer(findActiveUser(context, command.username()));
            value.setOrganisationseinheit(findActiveOrganisationseinheit(context, command.organisationseinheit()));
            value.setAstatus(DELEGATED);
            record(value, EreignisTyp.Zugewiesen, "Aufgabe delegiert.", context);
            return toView(value);
        });
    }

    private Aufgabe requireTask(ObjectContext context, UUID tid) {
        Aufgabe value = ObjectSelect.query(Aufgabe.class).where(Aufgabe.T_ILI_TID.eq(tid)).selectFirst(context);
        if (value == null) {
            throw new IllegalArgumentException("Unbekannte Aufgabe: " + tid);
        }
        return value;
    }

    private void requireOpenTask(Aufgabe value) {
        if (COMPLETED.equals(value.getAstatus()) || CANCELLED.equals(value.getAstatus())) {
            throw new DomainRuleViolationException("Aufgabe ist bereits abgeschlossen.");
        }
    }

    private Geschaeft findBusiness(ObjectContext context, GeschaeftNumber number) {
        return ObjectSelect.query(Geschaeft.class)
                .where(Geschaeft.GESCHAEFTSNUMMER.eq(number.value())).selectFirst(context);
    }

    private Aufgabentyp findActiveType(ObjectContext context, String code) {
        Aufgabentyp value = ObjectSelect.query(Aufgabentyp.class)
                .where(Aufgabentyp.ACODE.eq(code)).selectFirst(context);
        if (value == null || !ACTIVE.equalsIgnoreCase(value.getAstatus())) {
            throw new IllegalArgumentException("Aktiver Aufgabentyp fehlt: " + code);
        }
        return value;
    }

    private Benutzer findActiveUser(ObjectContext context, String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        Benutzer value = ObjectSelect.query(Benutzer.class)
                .where(Benutzer.USERNAME.eq(username)).selectFirst(context);
        if (value == null || !ACTIVE.equalsIgnoreCase(value.getAstatus())) {
            throw new IllegalArgumentException("Aktiver Benutzer fehlt: " + username);
        }
        return value;
    }

    private Organisationseinheit findActiveOrganisationseinheit(ObjectContext context, String kuerzel) {
        if (kuerzel == null || kuerzel.isBlank()) {
            return null;
        }
        Organisationseinheit value = ObjectSelect.query(Organisationseinheit.class)
                .where(Organisationseinheit.KUERZEL.eq(kuerzel)).selectFirst(context);
        if (value == null || !ACTIVE.equalsIgnoreCase(value.getAstatus())) {
            throw new IllegalArgumentException("Aktive Organisationseinheit fehlt: " + kuerzel);
        }
        return value;
    }

    private void requireOpenBusiness(Geschaeft business) {
        if (business == null) {
            throw new IllegalArgumentException("Unbekanntes Geschäft.");
        }
        if ("Abgeschlossen".equalsIgnoreCase(business.getLifecyclestatus())
                || "Archiviert".equalsIgnoreCase(business.getLifecyclestatus())
                || "Vernichtet".equalsIgnoreCase(business.getLifecyclestatus())) {
            throw new DomainRuleViolationException("Geschäft ist nicht mehr bearbeitbar.");
        }
    }

    private void record(Aufgabe value, EreignisTyp type, String remark, ObjectContext context) {
        journalService.record(context, new JournalCommand(
                EreignisObjektTyp.Aufgabe, value.getTIliTid().toString(), type, remark,
                currentActor.id(), Instant.now(clock)));
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

    private static String remark(String prefix, String comment) {
        return comment == null || comment.isBlank() ? prefix + "." : prefix + ": " + comment.trim();
    }
}
