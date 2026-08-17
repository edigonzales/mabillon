package guru.interlis.mabillon.dossier;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import guru.interlis.mabillon.domain.DomainRuleViolationException;
import guru.interlis.mabillon.journal.EreignisObjektTyp;
import guru.interlis.mabillon.journal.EreignisTyp;
import guru.interlis.mabillon.journal.JournalCommand;
import guru.interlis.mabillon.journal.JournalService;
import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.numbering.NumberingService;
import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Benutzer;
import guru.interlis.mabillon.persistence.cayenne.Dossier;
import guru.interlis.mabillon.persistence.cayenne.Geschaeft;
import guru.interlis.mabillon.persistence.cayenne.Ordnungssystemposition;
import guru.interlis.mabillon.persistence.cayenne.Organisationseinheit;
import guru.interlis.mabillon.persistence.cayenne.Unterlage;
import guru.interlis.mabillon.quality.DataQualityService;
import guru.interlis.mabillon.security.ActorId;
import guru.interlis.mabillon.security.AuthorizationService;
import guru.interlis.mabillon.security.CurrentActor;
import guru.interlis.mabillon.security.Permission;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.stereotype.Service;

@Service
public final class DossierService {

    private static final String OPEN = "Offen";
    private static final String CLOSED = "Geschlossen";

    private final CayenneUnitOfWork unitOfWork;
    private final NumberingService numberingService;
    private final JournalService journalService;
    private final AuthorizationService authorizationService;
    private final CurrentActor currentActor;
    private final Clock clock;
    private final DataQualityService dataQualityService;

    public DossierService(
            CayenneUnitOfWork unitOfWork,
            NumberingService numberingService,
            JournalService journalService,
            AuthorizationService authorizationService,
            CurrentActor currentActor,
            Clock clock,
            DataQualityService dataQualityService) {
        this.unitOfWork = unitOfWork;
        this.numberingService = numberingService;
        this.journalService = journalService;
        this.authorizationService = authorizationService;
        this.currentActor = currentActor;
        this.clock = clock;
        this.dataQualityService = dataQualityService;
    }

    public DossierView open(OpenDossierCommand command) {
        authorizationService.require(Permission.EDIT_DOSSIER);
        LocalDate openingDate = command.openingDate() == null ? LocalDate.now(clock) : command.openingDate();
        return unitOfWork.write(context -> {
            Ordnungssystemposition position = findPosition(context, command.registraturplanPositionCode());
            if (position == null || !isActive(position.getAstatus()) || position.getOrdnungssystem() == null
                    || !isActive(position.getOrdnungssystem().getAstatus()) || !isLeaf(position)) {
                throw new DomainRuleViolationException("Neue Dossiers benötigen eine aktive Blattposition.");
            }
            Organisationseinheit federation = findOrganisationseinheit(context, command.federfuehrungKuerzel());
            Benutzer responsible = findBenutzer(context, command.verantwortlicherUsername());
            requireActive(federation, "Federführung");
            requireActive(responsible, "Verantwortlicher");

            Dossier dossier = context.newObject(Dossier.class);
            DossierNumber number = numberingService.nextDossierNumber(command.federfuehrungKuerzel(), openingDate);
            dossier.setDossiernummer(number.value());
            dossier.setTitel(command.title());
            dossier.setBeschreibung(command.description());
            dossier.setOrdnungssystemposition(position);
            dossier.setOrganisationseinheit(federation);
            dossier.setBenutzer(responsible);
            dossier.setAstatus(OPEN);
            dossier.setEroeffnetam(openingDate);
            dossier.setTBasket(businessBasket(context));
            dossier.setTIliTid(UUID.randomUUID());
            record(number.value(), EreignisTyp.Erstellt, "Dossier eröffnet.", context);
            return toView(dossier);
        });
    }

    public DossierView update(UpdateDossierCommand command) {
        authorizationService.require(Permission.EDIT_DOSSIER);
        return unitOfWork.write(context -> {
            Dossier dossier = findDossier(context, command.number().value());
            if (dossier == null) {
                throw new IllegalArgumentException("Unbekanntes Dossier: " + command.number().value());
            }
            requireOpen(dossier);
            dossier.setTitel(command.title());
            dossier.setBeschreibung(command.description());
            dossier.setBemerkungen(command.remarks());
            if (command.verantwortlicherUsername() != null && !command.verantwortlicherUsername().isBlank()) {
                Benutzer responsible = findBenutzer(context, command.verantwortlicherUsername());
                requireActive(responsible, "Verantwortlicher");
                dossier.setBenutzer(responsible);
            }
            record(dossier.getDossiernummer(), EreignisTyp.Geaendert, "Dossier geändert.", context);
            return toView(dossier);
        });
    }

    public DossierView close(DossierNumber number) {
        authorizationService.require(Permission.CLOSE_DOSSIER);
        return unitOfWork.write(context -> {
            Dossier dossier = findDossier(context, number.value());
            if (dossier == null) {
                throw new IllegalArgumentException("Unbekanntes Dossier: " + number.value());
            }
            requireOpen(dossier);
            if (dossier.getGeschaefts().stream()
                    .anyMatch(business -> !"Abgeschlossen".equalsIgnoreCase(business.getLifecyclestatus()))) {
                throw new DomainRuleViolationException("Dossier enthält ein nicht abgeschlossenes Geschäft.");
            }
            if (dossier.getUnterlages().stream()
                    .anyMatch(document -> document.isAktenrelevant()
                            && "In_Arbeit".equalsIgnoreCase(document.getAstatus()))) {
                throw new DomainRuleViolationException("Dossier enthält eine aktenrelevante Unterlage in Arbeit.");
            }
            var quality = dataQualityService.checkDossier(number);
            if (quality.hasErrors()) {
                throw new DomainRuleViolationException("Dossier verletzt Datenqualitätsregeln: "
                        + quality.findings().stream()
                                .filter(finding -> finding.severity().name().equals("ERROR"))
                                .map(finding -> finding.ruleCode() + " " + finding.message())
                                .findFirst().orElse("unbekannter Fehler"));
            }
            dossier.setAstatus(CLOSED);
            dossier.setGeschlossenam(LocalDate.now(clock));
            record(dossier.getDossiernummer(), EreignisTyp.Dossier_abgeschlossen,
                    "Dossier fachlich abgeschlossen.", context);
            return toView(dossier);
        });
    }

    public DossierView reopen(DossierNumber number, String reason) {
        authorizationService.require(Permission.CLOSE_DOSSIER);
        return unitOfWork.write(context -> {
            Dossier dossier = findDossier(context, number.value());
            if (dossier == null) {
                throw new IllegalArgumentException("Unbekanntes Dossier: " + number.value());
            }
            if (!CLOSED.equalsIgnoreCase(dossier.getAstatus())) {
                throw new DomainRuleViolationException("Nur geschlossene Dossiers können wieder geöffnet werden.");
            }
            dossier.setAstatus(OPEN);
            dossier.setGeschlossenam(null);
            record(dossier.getDossiernummer(), EreignisTyp.Geaendert,
                    reason == null || reason.isBlank() ? "Dossier wieder geöffnet." : reason, context);
            return toView(dossier);
        });
    }

    private void record(String objectId, EreignisTyp type, String remark, ObjectContext context) {
        ActorId actorId = currentActor.id();
        journalService.record(context, new JournalCommand(
                EreignisObjektTyp.Dossier, objectId, type, remark, actorId, Instant.now(clock)));
    }

    private Dossier findDossier(ObjectContext context, String number) {
        return ObjectSelect.query(Dossier.class)
                .where(Dossier.DOSSIERNUMMER.eq(number))
                .selectFirst(context);
    }

    private Ordnungssystemposition findPosition(ObjectContext context, String code) {
        return ObjectSelect.query(Ordnungssystemposition.class)
                .where(Ordnungssystemposition.ACODE.eq(code))
                .selectFirst(context);
    }

    private Organisationseinheit findOrganisationseinheit(ObjectContext context, String code) {
        return ObjectSelect.query(Organisationseinheit.class)
                .where(Organisationseinheit.KUERZEL.eq(code))
                .selectFirst(context);
    }

    private Benutzer findBenutzer(ObjectContext context, String username) {
        return ObjectSelect.query(Benutzer.class)
                .where(Benutzer.USERNAME.eq(username))
                .selectFirst(context);
    }

    private long businessBasket(ObjectContext context) {
        Dossier existing = ObjectSelect.query(Dossier.class).selectFirst(context);
        if (existing != null) {
            return existing.getTBasket();
        }
        Geschaeft business = ObjectSelect.query(Geschaeft.class).selectFirst(context);
        if (business != null) {
            return business.getTBasket();
        }
        throw new IllegalStateException("Kein Geschäftsdaten-Basket vorhanden.");
    }

    private boolean isLeaf(Ordnungssystemposition position) {
        return position.getOrdnungssystempositions().stream().noneMatch(child -> child != position);
    }

    private boolean isActive(String status) {
        return "aktiv".equalsIgnoreCase(status) || "Aktiv".equalsIgnoreCase(status);
    }

    private void requireActive(Object value, String label) {
        if (value == null) {
            throw new DomainRuleViolationException(label + " fehlt.");
        }
        String status = value instanceof Benutzer user ? user.getAstatus() : ((Organisationseinheit) value).getAstatus();
        if (!isActive(status)) {
            throw new DomainRuleViolationException(label + " ist nicht aktiv.");
        }
    }

    private void requireOpen(Dossier dossier) {
        if (!OPEN.equalsIgnoreCase(dossier.getAstatus())) {
            throw new DomainRuleViolationException("Dossier ist nicht offen.");
        }
    }

    private DossierView toView(Dossier dossier) {
        return new DossierView(
                dossier.getDossiernummer(),
                dossier.getTitel(),
                dossier.getBeschreibung(),
                dossier.getAstatus(),
                dossier.getGeschaefts().stream()
                        .map(business -> new DossierView.GeschaeftSummary(
                                business.getGeschaeftsnummer(), business.getTitel(), business.getLifecyclestatus()))
                        .toList(),
                dossier.getUnterlages().stream()
                        .map(document -> new DossierView.UnterlageSummary(
                                document.getTIliTid(), document.getTitel(), document.getDateiname(), document.getAstatus()))
                        .toList());
    }
}
