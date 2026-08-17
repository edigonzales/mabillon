package guru.interlis.mabillon.fachsystem;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import guru.interlis.mabillon.domain.DomainRuleViolationException;
import guru.interlis.mabillon.journal.EreignisObjektTyp;
import guru.interlis.mabillon.journal.EreignisTyp;
import guru.interlis.mabillon.journal.JournalCommand;
import guru.interlis.mabillon.journal.JournalService;
import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Dossier;
import guru.interlis.mabillon.persistence.cayenne.Fachsystemreferenz;
import guru.interlis.mabillon.persistence.cayenne.Geschaeft;
import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.numbering.GeschaeftNumber;
import guru.interlis.mabillon.security.AuthorizationService;
import guru.interlis.mabillon.security.CurrentActor;
import guru.interlis.mabillon.security.AuthorizationException;
import guru.interlis.mabillon.security.Permission;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.stereotype.Service;

@Service
public final class FachsystemReferenzService {

    private final CayenneUnitOfWork unitOfWork;
    private final JournalService journalService;
    private final AuthorizationService authorizationService;
    private final CurrentActor currentActor;
    private final Clock clock;

    public FachsystemReferenzService(
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

    public FachsystemReferenzView addToGeschaeft(AddFachsystemReferenzCommand command) {
        authorizationService.require(Permission.EDIT_GESCHAEFT);
        return unitOfWork.write(context -> {
            Geschaeft business = findBusiness(context, command.geschaeftNumber());
            requireOpenBusiness(business);
            Fachsystemreferenz value = newReference(context, command.systemCode(), command.objektTyp(),
                    command.objektId(), command.mutationId(), command.link(), command.beschreibung());
            value.setGeschaeft(business);
            value.setTBasket(business.getTBasket());
            record(value, "Fachsystemreferenz einem Geschäft zugeordnet.", context);
            return toView(value);
        });
    }

    public FachsystemReferenzView addToDossier(AddFachsystemReferenzToDossierCommand command) {
        authorizationService.require(Permission.EDIT_DOSSIER);
        return unitOfWork.write(context -> {
            Dossier dossier = findDossier(context, command.dossierNumber());
            if (!"Offen".equalsIgnoreCase(dossier.getAstatus())) {
                throw new DomainRuleViolationException("Dossier ist nicht mehr bearbeitbar.");
            }
            Fachsystemreferenz value = newReference(context, command.systemCode(), command.objektTyp(),
                    command.objektId(), command.mutationId(), command.link(), command.beschreibung());
            value.setDossier(dossier);
            value.setTBasket(dossier.getTBasket());
            record(value, "Fachsystemreferenz einem Dossier zugeordnet.", context);
            return toView(value);
        });
    }

    public void remove(UUID referenceTid, String reason) {
        if (!authorizationService.has(Permission.EDIT_GESCHAEFT)
                && !authorizationService.has(Permission.EDIT_DOSSIER)) {
            throw new AuthorizationException("Berechtigung zum Entfernen der Fachsystemreferenz fehlt.");
        }
        unitOfWork.write(context -> {
            Fachsystemreferenz value = find(context, referenceTid);
            if (value == null) {
                throw new IllegalArgumentException("Unbekannte Fachsystemreferenz: " + referenceTid);
            }
            record(value, reason == null || reason.isBlank()
                    ? "Fachsystemreferenz entfernt." : "Fachsystemreferenz entfernt: " + reason, context);
            context.deleteObjects(value);
        });
    }

    public List<FachsystemReferenzView> forGeschaeft(GeschaeftNumber number) {
        return unitOfWork.read(context -> {
            Geschaeft business = findBusiness(context, number);
            return business.getFachsystemreferenzes().stream()
                    .sorted(Comparator.comparing(Fachsystemreferenz::getSystemcode)
                            .thenComparing(Fachsystemreferenz::getObjektid))
                    .map(this::toView)
                    .toList();
        });
    }

    public List<FachsystemReferenzView> forDossier(DossierNumber number) {
        return unitOfWork.read(context -> {
            Dossier dossier = findDossier(context, number);
            List<Fachsystemreferenz> references = new java.util.ArrayList<>(dossier.getFachsystemreferenzes());
            dossier.getGeschaefts().forEach(business -> references.addAll(business.getFachsystemreferenzes()));
            return references.stream()
                    .sorted(Comparator.comparing(Fachsystemreferenz::getSystemcode)
                            .thenComparing(Fachsystemreferenz::getObjektid))
                    .map(this::toView)
                    .toList();
        });
    }

    private Fachsystemreferenz newReference(
            ObjectContext context, String systemCode, String objectType, String objectId,
            String mutationId, String link, String description) {
        Fachsystemreferenz value = context.newObject(Fachsystemreferenz.class);
        value.setSystemcode(systemCode.trim());
        value.setObjekttyp(objectType.trim());
        value.setObjektid(objectId.trim());
        value.setMutationid(mutationId);
        value.setLink(link);
        value.setBeschreibung(description);
        value.setTIliTid(UUID.randomUUID());
        return value;
    }

    private void record(Fachsystemreferenz value, String remark, ObjectContext context) {
        journalService.record(context, new JournalCommand(
                EreignisObjektTyp.FachsystemReferenz, value.getTIliTid().toString(), EreignisTyp.Erstellt,
                remark, currentActor.id(), Instant.now(clock)));
    }

    private Geschaeft findBusiness(ObjectContext context, GeschaeftNumber number) {
        Geschaeft value = ObjectSelect.query(Geschaeft.class)
                .where(Geschaeft.GESCHAEFTSNUMMER.eq(number.value())).selectFirst(context);
        if (value == null) {
            throw new IllegalArgumentException("Unbekanntes Geschäft: " + number.value());
        }
        return value;
    }

    private Dossier findDossier(ObjectContext context, DossierNumber number) {
        Dossier value = ObjectSelect.query(Dossier.class)
                .where(Dossier.DOSSIERNUMMER.eq(number.value())).selectFirst(context);
        if (value == null) {
            throw new IllegalArgumentException("Unbekanntes Dossier: " + number.value());
        }
        return value;
    }

    private Fachsystemreferenz find(ObjectContext context, UUID tid) {
        return ObjectSelect.query(Fachsystemreferenz.class)
                .where(Fachsystemreferenz.T_ILI_TID.eq(tid)).selectFirst(context);
    }

    private void requireOpenBusiness(Geschaeft business) {
        if ("Abgeschlossen".equalsIgnoreCase(business.getLifecyclestatus())
                || "Archiviert".equalsIgnoreCase(business.getLifecyclestatus())
                || "Vernichtet".equalsIgnoreCase(business.getLifecyclestatus())) {
            throw new DomainRuleViolationException("Geschäft ist nicht mehr bearbeitbar.");
        }
    }

    private FachsystemReferenzView toView(Fachsystemreferenz value) {
        return new FachsystemReferenzView(value.getTIliTid(), value.getSystemcode(), value.getObjekttyp(),
                value.getObjektid(), value.getMutationid(), value.getLink(), value.getBeschreibung(),
                value.getDossier() == null ? null : value.getDossier().getDossiernummer(),
                value.getGeschaeft() == null ? null : value.getGeschaeft().getGeschaeftsnummer());
    }
}
