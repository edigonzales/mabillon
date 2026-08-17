package guru.interlis.mabillon.geschaeft;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import guru.interlis.mabillon.domain.DomainRuleViolationException;
import guru.interlis.mabillon.journal.EreignisObjektTyp;
import guru.interlis.mabillon.journal.EreignisTyp;
import guru.interlis.mabillon.journal.JournalCommand;
import guru.interlis.mabillon.journal.JournalService;
import guru.interlis.mabillon.numbering.GeschaeftNumber;
import guru.interlis.mabillon.numbering.NumberingService;
import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Benutzer;
import guru.interlis.mabillon.persistence.cayenne.Dossier;
import guru.interlis.mabillon.persistence.cayenne.Geschaeft;
import guru.interlis.mabillon.persistence.cayenne.Geschaeftsart;
import guru.interlis.mabillon.persistence.cayenne.Organisationseinheit;
import guru.interlis.mabillon.persistence.cayenne.Prozessstatus;
import guru.interlis.mabillon.persistence.cayenne.Resultatstatus;
import guru.interlis.mabillon.security.AuthorizationService;
import guru.interlis.mabillon.security.CurrentActor;
import guru.interlis.mabillon.security.Permission;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.stereotype.Service;

@Service
public final class GeschaeftService {

    private static final String OPEN_DOSSIER = "Offen";
    private static final String CREATED = "Eroeffnet";
    private static final String IN_PROGRESS = "In_Bearbeitung";
    private static final String SUSPENDED = "Sistiert";
    private static final String CLOSED = "Abgeschlossen";

    private final CayenneUnitOfWork unitOfWork;
    private final NumberingService numberingService;
    private final JournalService journalService;
    private final AuthorizationService authorizationService;
    private final CurrentActor currentActor;
    private final Clock clock;

    public GeschaeftService(
            CayenneUnitOfWork unitOfWork,
            NumberingService numberingService,
            JournalService journalService,
            AuthorizationService authorizationService,
            CurrentActor currentActor,
            Clock clock) {
        this.unitOfWork = unitOfWork;
        this.numberingService = numberingService;
        this.journalService = journalService;
        this.authorizationService = authorizationService;
        this.currentActor = currentActor;
        this.clock = clock;
    }

    public GeschaeftView open(OpenGeschaeftCommand command) {
        authorizationService.require(Permission.EDIT_GESCHAEFT);
        LocalDate today = LocalDate.now(clock);
        LocalDate openingDate = command.eroeffnungsdatum() == null ? today : command.eroeffnungsdatum();
        return unitOfWork.write(context -> {
            Dossier dossier = findDossier(context, command.dossierNumber().value());
            if (dossier == null || !OPEN_DOSSIER.equalsIgnoreCase(dossier.getAstatus())) {
                throw new DomainRuleViolationException("Geschäft benötigt ein offenes Dossier.");
            }
            Geschaeftsart type = findGeschaeftsart(context, command.geschaeftsartCode());
            Organisationseinheit federation = findOrganisationseinheit(context, command.federfuehrungKuerzel());
            Benutzer responsible = findBenutzer(context, command.verantwortlicherUsername());
            requireActive(type, "Geschäftsart");
            requireActive(federation, "Federführung");
            requireActive(responsible, "Verantwortlicher");
            List<Prozessstatus> initialStatuses = ObjectSelect.query(Prozessstatus.class).select(context).stream()
                    .filter(status -> status.isAinitial()
                            && status.getGeschaeftsart() != null
                            && type.getAcode().equals(status.getGeschaeftsart().getAcode())
                            && isActive(status.getAstatus()))
                    .toList();
            if (initialStatuses.size() != 1) {
                throw new DomainRuleViolationException(
                        "Geschäftsart benötigt genau einen aktiven Initialstatus.");
            }
            Prozessstatus initial = initialStatuses.getFirst();

            Geschaeft business = context.newObject(Geschaeft.class);
            GeschaeftNumber number = numberingService.nextGeschaeftNumber(
                    command.federfuehrungKuerzel(), openingDate);
            business.setGeschaeftsnummer(number.value());
            business.setTitel(command.title());
            business.setKurzbeschreibung(command.shortDescription());
            business.setGeschaeftsart(type);
            business.setLifecyclestatus(CREATED);
            business.setProzessstatus(initial);
            business.setDossier(dossier);
            business.setOrganisationseinheit(federation);
            business.setBenutzer(responsible);
            business.setEingangsdatum(command.eingangsdatum());
            business.setEroeffnungsdatum(openingDate);
            business.setFaelligam(command.dueDate());
            business.setPrioritaet(command.priority() == null ? 0 : command.priority());
            business.setErstelltam(LocalDateTime.now(clock));
            business.setTBasket(businessBasket(context));
            business.setTIliTid(UUID.randomUUID());
            record(number.value(), EreignisTyp.Erstellt, "Geschäft eröffnet.", context);
            return toView(business);
        });
    }

    public GeschaeftView update(UpdateGeschaeftCommand command) {
        authorizationService.require(Permission.EDIT_GESCHAEFT);
        return unitOfWork.write(context -> {
            Geschaeft business = requireBusiness(context, command.number());
            requireEditable(business);
            business.setTitel(command.title());
            business.setKurzbeschreibung(command.shortDescription());
            business.setFaelligam(command.dueDate());
            if (command.priority() != null) {
                business.setPrioritaet(command.priority());
            }
            if (command.verantwortlicherUsername() != null && !command.verantwortlicherUsername().isBlank()) {
                Benutzer responsible = findBenutzer(context, command.verantwortlicherUsername());
                requireActive(responsible, "Verantwortlicher");
                business.setBenutzer(responsible);
            }
            touch(business);
            record(business.getGeschaeftsnummer(), EreignisTyp.Geaendert, "Geschäft geändert.", context);
            return toView(business);
        });
    }

    public GeschaeftView changeProcessStatus(ChangeProcessStatusCommand command) {
        authorizationService.require(Permission.EDIT_GESCHAEFT);
        return unitOfWork.write(context -> {
            Geschaeft business = requireBusiness(context, command.number());
            requireEditable(business);
            List<Prozessstatus> candidates = ObjectSelect.query(Prozessstatus.class).select(context).stream()
                    .filter(status -> command.processStatusCode().equals(status.getAcode()))
                    .toList();
            if (candidates.isEmpty()) {
                throw new DomainRuleViolationException("Unbekannter Prozessstatus.");
            }
            Prozessstatus target = candidates.stream()
                    .filter(status -> status.getGeschaeftsart() != null
                            && business.getGeschaeftsart() != null
                            && business.getGeschaeftsart().getAcode().equals(status.getGeschaeftsart().getAcode())
                            && isActive(status.getAstatus()))
                    .findFirst()
                    .orElseThrow(() -> new DomainRuleViolationException("Prozessstatus gehört nicht zur Geschäftsart."));
            if (!isActive(target.getAstatus()) || target.getGeschaeftsart() == null
                    || business.getGeschaeftsart() == null
                    || !business.getGeschaeftsart().getAcode().equals(target.getGeschaeftsart().getAcode())) {
                throw new DomainRuleViolationException("Prozessstatus gehört nicht zur Geschäftsart.");
            }
            business.setProzessstatus(target);
            if (CREATED.equalsIgnoreCase(business.getLifecyclestatus())) {
                business.setLifecyclestatus(IN_PROGRESS);
            }
            touch(business);
            record(business.getGeschaeftsnummer(), EreignisTyp.Status_geaendert,
                    command.comment() == null ? "Prozessstatus geändert." : command.comment(), context);
            return toView(business);
        });
    }

    public GeschaeftView setResult(SetResultCommand command) {
        authorizationService.require(Permission.EDIT_GESCHAEFT);
        return unitOfWork.write(context -> {
            Geschaeft business = requireBusiness(context, command.number());
            requireEditable(business);
            List<Resultatstatus> candidates = ObjectSelect.query(Resultatstatus.class).select(context).stream()
                    .filter(status -> command.resultStatusCode().equals(status.getAcode()))
                    .toList();
            if (candidates.isEmpty()) {
                throw new DomainRuleViolationException("Unbekannter Resultatstatus.");
            }
            Resultatstatus target = candidates.stream()
                    .filter(status -> status.getGeschaeftsart() != null
                            && business.getGeschaeftsart() != null
                            && business.getGeschaeftsart().getAcode().equals(status.getGeschaeftsart().getAcode())
                            && isActive(status.getAstatus()))
                    .findFirst()
                    .orElseThrow(() -> new DomainRuleViolationException("Resultatstatus gehört nicht zur Geschäftsart."));
            if (!isActive(target.getAstatus()) || target.getGeschaeftsart() == null
                    || business.getGeschaeftsart() == null
                    || !business.getGeschaeftsart().getAcode().equals(target.getGeschaeftsart().getAcode())) {
                throw new DomainRuleViolationException("Resultatstatus gehört nicht zur Geschäftsart.");
            }
            business.setResultatstatus(target);
            touch(business);
            record(business.getGeschaeftsnummer(), EreignisTyp.Entscheid_erfasst,
                    command.comment() == null ? "Geschäftsergebnis erfasst." : command.comment(), context);
            return toView(business);
        });
    }

    public GeschaeftView suspend(GeschaeftNumber number, String reason) {
        authorizationService.require(Permission.EDIT_GESCHAEFT);
        return transition(number, SUSPENDED, reason == null ? "Geschäft sistiert." : reason);
    }

    public GeschaeftView resume(GeschaeftNumber number, String comment) {
        authorizationService.require(Permission.EDIT_GESCHAEFT);
        return unitOfWork.write(context -> {
            Geschaeft business = requireBusiness(context, number);
            if (!SUSPENDED.equalsIgnoreCase(business.getLifecyclestatus())) {
                throw new DomainRuleViolationException("Nur sistierte Geschäfte können fortgesetzt werden.");
            }
            business.setLifecyclestatus(IN_PROGRESS);
            touch(business);
            record(business.getGeschaeftsnummer(), EreignisTyp.Status_geaendert,
                    comment == null ? "Geschäft fortgesetzt." : comment, context);
            return toView(business);
        });
    }

    public GeschaeftView close(GeschaeftNumber number) {
        authorizationService.require(Permission.EDIT_GESCHAEFT);
        return unitOfWork.write(context -> {
            Geschaeft business = requireBusiness(context, number);
            requireEditable(business);
            if (business.getAufgabes().stream()
                    .anyMatch(task -> !"Erledigt".equalsIgnoreCase(task.getAstatus())
                            && !"Abgebrochen".equalsIgnoreCase(task.getAstatus()))) {
                throw new DomainRuleViolationException("Geschäft enthält offene Aufgaben.");
            }
            if (business.getProzessstatus() != null && !business.getProzessstatus().isTerminal()) {
                throw new DomainRuleViolationException("Der Prozessstatus ist nicht terminal.");
            }
            if (business.getGeschaeftsart() != null && business.getGeschaeftsart().isResultaterforderlich()
                    && business.getResultatstatus() == null) {
                throw new DomainRuleViolationException("Für diese Geschäftsart ist ein Resultat erforderlich.");
            }
            if (business.getResultatstatus() != null && business.getGeschaeftsart() != null
                    && (business.getResultatstatus().getGeschaeftsart() == null
                    || !business.getGeschaeftsart().getAcode().equals(
                    business.getResultatstatus().getGeschaeftsart().getAcode()))) {
                throw new DomainRuleViolationException("Resultatstatus gehört nicht zur Geschäftsart.");
            }
            if (business.getUnterlages().stream()
                    .anyMatch(document -> document.isAktenrelevant()
                            && "In_Arbeit".equalsIgnoreCase(document.getAstatus()))) {
                throw new DomainRuleViolationException("Geschäft enthält eine aktenrelevante Unterlage in Arbeit.");
            }
            business.setLifecyclestatus(CLOSED);
            business.setAbgeschlossenam(LocalDate.now(clock));
            touch(business);
            record(business.getGeschaeftsnummer(), EreignisTyp.Geschaeft_abgeschlossen,
                    "Geschäft abgeschlossen.", context);
            return toView(business);
        });
    }

    private GeschaeftView transition(GeschaeftNumber number, String lifecycle, String remark) {
        return unitOfWork.write(context -> {
            Geschaeft business = requireBusiness(context, number);
            requireEditable(business);
            business.setLifecyclestatus(lifecycle);
            touch(business);
            record(business.getGeschaeftsnummer(), EreignisTyp.Status_geaendert, remark, context);
            return toView(business);
        });
    }

    private Geschaeft requireBusiness(ObjectContext context, GeschaeftNumber number) {
        Geschaeft business = ObjectSelect.query(Geschaeft.class)
                .where(Geschaeft.GESCHAEFTSNUMMER.eq(number.value()))
                .selectFirst(context);
        if (business == null) {
            throw new IllegalArgumentException("Unbekanntes Geschäft: " + number.value());
        }
        return business;
    }

    private Dossier findDossier(ObjectContext context, String number) {
        return ObjectSelect.query(Dossier.class).where(Dossier.DOSSIERNUMMER.eq(number)).selectFirst(context);
    }

    private Geschaeftsart findGeschaeftsart(ObjectContext context, String code) {
        return ObjectSelect.query(Geschaeftsart.class).where(Geschaeftsart.ACODE.eq(code)).selectFirst(context);
    }

    private Organisationseinheit findOrganisationseinheit(ObjectContext context, String code) {
        return ObjectSelect.query(Organisationseinheit.class).where(Organisationseinheit.KUERZEL.eq(code)).selectFirst(context);
    }

    private Benutzer findBenutzer(ObjectContext context, String username) {
        return ObjectSelect.query(Benutzer.class).where(Benutzer.USERNAME.eq(username)).selectFirst(context);
    }

    private long businessBasket(ObjectContext context) {
        Geschaeft existing = ObjectSelect.query(Geschaeft.class).selectFirst(context);
        if (existing != null) {
            return existing.getTBasket();
        }
        Dossier dossier = ObjectSelect.query(Dossier.class).selectFirst(context);
        if (dossier != null) {
            return dossier.getTBasket();
        }
        throw new IllegalStateException("Kein Geschäftsdaten-Basket vorhanden.");
    }

    private void requireActive(Object value, String label) {
        if (value == null) {
            throw new DomainRuleViolationException(label + " fehlt.");
        }
        String status;
        if (value instanceof Geschaeftsart type) {
            status = type.getAstatus();
        } else if (value instanceof Organisationseinheit organisationseinheit) {
            status = organisationseinheit.getAstatus();
        } else {
            status = ((Benutzer) value).getAstatus();
        }
        if (!isActive(status)) {
            throw new DomainRuleViolationException(label + " ist nicht aktiv.");
        }
    }

    private boolean isActive(String status) {
        return "aktiv".equalsIgnoreCase(status);
    }

    private void requireEditable(Geschaeft business) {
        if (CLOSED.equalsIgnoreCase(business.getLifecyclestatus())
                || "Archiviert".equalsIgnoreCase(business.getLifecyclestatus())
                || "Vernichtet".equalsIgnoreCase(business.getLifecyclestatus())) {
            throw new DomainRuleViolationException("Geschäft ist nicht mehr bearbeitbar.");
        }
    }

    private void touch(Geschaeft business) {
        business.setGeaendertam(LocalDateTime.now(clock));
    }

    private void record(String objectId, EreignisTyp type, String remark, ObjectContext context) {
        journalService.record(context, new JournalCommand(
                EreignisObjektTyp.Geschaeft,
                objectId,
                type,
                remark,
                currentActor.id(),
                Instant.now(clock)));
    }

    private GeschaeftView toView(Geschaeft business) {
        return new GeschaeftView(
                business.getGeschaeftsnummer(),
                business.getTitel(),
                business.getKurzbeschreibung(),
                business.getLifecyclestatus(),
                business.getDossier() == null ? null : business.getDossier().getDossiernummer(),
                business.getUnterlages().stream()
                        .map(document -> new GeschaeftView.UnterlageSummary(
                                document.getTIliTid(), document.getTitel(), document.getDateiname(), document.getAstatus()))
                        .toList(),
                business.getGeschaeftsart() == null ? null : business.getGeschaeftsart().getAcode(),
                business.getProzessstatus() == null ? null : business.getProzessstatus().getAcode(),
                business.getProzessstatus() == null ? null : business.getProzessstatus().getAname(),
                business.getResultatstatus() == null ? null : business.getResultatstatus().getAcode());
    }
}
