package guru.interlis.mabillon.unterlage;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import guru.interlis.mabillon.domain.DomainRuleViolationException;
import guru.interlis.mabillon.journal.EreignisObjektTyp;
import guru.interlis.mabillon.journal.EreignisTyp;
import guru.interlis.mabillon.journal.JournalCommand;
import guru.interlis.mabillon.journal.JournalService;
import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Benutzer;
import guru.interlis.mabillon.persistence.cayenne.Dossier;
import guru.interlis.mabillon.persistence.cayenne.Ereignis;
import guru.interlis.mabillon.persistence.cayenne.Geschaeft;
import guru.interlis.mabillon.persistence.cayenne.Unterlage;
import guru.interlis.mabillon.persistence.cayenne.Unterlagentyp;
import guru.interlis.mabillon.security.AuthorizationService;
import guru.interlis.mabillon.security.CurrentActor;
import guru.interlis.mabillon.security.Permission;
import guru.interlis.mabillon.storage.DocumentStorage;
import guru.interlis.mabillon.storage.DocumentUpload;
import guru.interlis.mabillon.storage.StoredDocument;
import guru.interlis.mabillon.storage.StagedDocument;
import guru.interlis.mabillon.storage.StorageTarget;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.stereotype.Service;

@Service
public final class UnterlageService {

    private static final String ACTIVE = "aktiv";
    private static final String REGISTERED = "Registriert";
    private static final String CANCELLED = "Storniert";

    private final CayenneUnitOfWork unitOfWork;
    private final DocumentStorage storage;
    private final JournalService journalService;
    private final AuthorizationService authorizationService;
    private final CurrentActor currentActor;
    private final Clock clock;

    public UnterlageService(
            CayenneUnitOfWork unitOfWork,
            DocumentStorage storage,
            JournalService journalService,
            AuthorizationService authorizationService,
            CurrentActor currentActor,
            Clock clock) {
        this.unitOfWork = unitOfWork;
        this.storage = storage;
        this.journalService = journalService;
        this.authorizationService = authorizationService;
        this.currentActor = currentActor;
        this.clock = clock;
    }

    public UnterlageView register(RegisterUnterlageCommand command, DocumentUpload upload) {
        authorizationService.require(Permission.EDIT_UNTERLAGE);
        StagedDocument staged = null;
        boolean databaseCommitted = false;
        UUID tid = UUID.randomUUID();
        try {
            if (upload != null) {
                staged = stage(upload);
            }
            StorageTarget target = new StorageTarget(command.dossierNumber().value());
            StoredDocument planned = staged == null ? null : describe(staged, target);
            StoredDocument plannedDocument = planned;
            UnterlageView result = unitOfWork.write(context -> {
                Dossier dossier = findDossier(context, command.dossierNumber().value());
                requireEditableDossier(dossier);
                Geschaeft business = command.geschaeftNumber() == null
                        ? null : findBusiness(context, command.geschaeftNumber().value());
                requireBusinessContext(dossier, business);
                Unterlagentyp type = findActiveType(context, command.typCode());
                Unterlage value = context.newObject(Unterlage.class);
                value.setTitel(command.title().trim());
                value.setUnterlagentyp(type);
                value.setUnterlagendatum(command.documentDate());
                value.setEingangsdatum(command.incomingDate());
                value.setAusgangsdatum(command.outgoingDate());
                value.setRegistriertam(LocalDateTime.now(clock));
                value.setBenutzer(findActor(context));
                value.setAktenrelevant(command.aktenrelevant());
                value.setAstatus(REGISTERED);
                value.setDateiname(plannedDocument == null ? null : plannedDocument.originalFilename());
                value.setMimetype(plannedDocument == null ? null : plannedDocument.mimeType());
                value.setDateigroesse(plannedDocument == null ? null : plannedDocument.size());
                value.setStorageuri(plannedDocument == null ? null : plannedDocument.storageUri());
                value.setHashsha256(plannedDocument == null ? null : plannedDocument.sha256());
                value.setDateiformat(command.dateiformat());
                value.setBemerkungen(command.bemerkungen());
                value.setDossier(dossier);
                value.setGeschaeft(business);
                value.setTBasket(dossier.getTBasket());
                value.setTIliTid(tid);
                journalService.record(context, new JournalCommand(
                        EreignisObjektTyp.Unterlage, tid.toString(),
                        EreignisTyp.Unterlage_registriert, "Unterlage registriert.",
                        currentActor.id(), Instant.now(clock)));
                return UnterlageQueryService.toView(value);
            });
            databaseCommitted = true;

            if (staged != null) {
                StoredDocument committed = commit(staged, target);
                if (!planned.equals(committed)) {
                    throw new IllegalStateException("Finale Ablage weicht von der geplanten Ablage ab.");
                }
            }
            return result;
        } catch (RuntimeException failure) {
            if (databaseCommitted) {
                compensateRegistration(tid, failure);
            }
            discardQuietly(staged, failure);
            throw failure;
        }
    }

    public UnterlageView assignToGeschaeft(AssignUnterlageCommand command) {
        authorizationService.require(Permission.EDIT_UNTERLAGE);
        return unitOfWork.write(context -> {
            Unterlage document = find(context, command.tid());
            if (document == null) {
                throw new IllegalArgumentException("Unbekannte Unterlage: " + command.tid());
            }
            Geschaeft business = findBusiness(context, command.geschaeftNumber().value());
            requireBusinessContext(document.getDossier(), business);
            if (CANCELLED.equalsIgnoreCase(document.getAstatus())) {
                throw new DomainRuleViolationException("Stornierte Unterlagen können nicht zugeordnet werden.");
            }
            document.setGeschaeft(business);
            journalService.record(context, new JournalCommand(
                    EreignisObjektTyp.Unterlage, document.getTIliTid().toString(), EreignisTyp.Geaendert,
                    "Unterlage einem Geschäft zugeordnet.", currentActor.id(), Instant.now(clock)));
            return UnterlageQueryService.toView(document);
        });
    }

    public UnterlageView cancel(UUID tid, String reason) {
        authorizationService.require(Permission.EDIT_UNTERLAGE);
        return unitOfWork.write(context -> {
            Unterlage document = find(context, tid);
            if (document == null) {
                throw new IllegalArgumentException("Unbekannte Unterlage: " + tid);
            }
            document.setAstatus(CANCELLED);
            journalService.record(context, new JournalCommand(
                    EreignisObjektTyp.Unterlage, document.getTIliTid().toString(), EreignisTyp.Geaendert,
                    reason == null || reason.isBlank() ? "Unterlage storniert." : "Unterlage storniert: " + reason,
                    currentActor.id(), Instant.now(clock)));
            return UnterlageQueryService.toView(document);
        });
    }

    private StagedDocument stage(DocumentUpload upload) {
        try {
            return storage.stage(upload);
        } catch (IOException failure) {
            throw new IllegalStateException("Datei konnte nicht zwischengespeichert werden.", failure);
        }
    }

    private StoredDocument describe(StagedDocument staged, StorageTarget target) {
        try {
            return storage.describe(staged, target);
        } catch (IOException failure) {
            throw new IllegalStateException("Finale Dateiablage konnte nicht geplant werden.", failure);
        }
    }

    private StoredDocument commit(StagedDocument staged, StorageTarget target) {
        try {
            return storage.commit(staged, target);
        } catch (IOException failure) {
            throw new IllegalStateException("Datei konnte nicht endgültig abgelegt werden.", failure);
        }
    }

    private void compensateRegistration(UUID tid, RuntimeException original) {
        try {
            unitOfWork.write(context -> {
                ObjectSelect.query(Ereignis.class)
                        .where(Ereignis.OBJEKTID.eq(tid.toString()))
                        .select(context).stream()
                        .filter(event -> EreignisObjektTyp.Unterlage.name().equals(event.getObjekttyp()))
                        .forEach(context::deleteObject);
                Unterlage document = find(context, tid);
                if (document != null) {
                    context.deleteObject(document);
                }
            });
        } catch (RuntimeException compensationFailure) {
            original.addSuppressed(compensationFailure);
        }
    }

    private void discardQuietly(StagedDocument staged, RuntimeException original) {
        if (staged == null) {
            return;
        }
        try {
            storage.discard(staged);
        } catch (IOException cleanupFailure) {
            original.addSuppressed(cleanupFailure);
        }
    }

    private Dossier findDossier(ObjectContext context, String number) {
        Dossier dossier = ObjectSelect.query(Dossier.class)
                .where(Dossier.DOSSIERNUMMER.eq(number)).selectFirst(context);
        if (dossier == null) {
            throw new IllegalArgumentException("Unbekanntes Dossier: " + number);
        }
        return dossier;
    }

    private Geschaeft findBusiness(ObjectContext context, String number) {
        Geschaeft business = ObjectSelect.query(Geschaeft.class)
                .where(Geschaeft.GESCHAEFTSNUMMER.eq(number)).selectFirst(context);
        if (business == null) {
            throw new IllegalArgumentException("Unbekanntes Geschäft: " + number);
        }
        return business;
    }

    private Unterlage find(ObjectContext context, UUID tid) {
        return ObjectSelect.query(Unterlage.class).where(Unterlage.T_ILI_TID.eq(tid)).selectFirst(context);
    }

    private Unterlagentyp findActiveType(ObjectContext context, String code) {
        Unterlagentyp type = ObjectSelect.query(Unterlagentyp.class)
                .where(Unterlagentyp.ACODE.eq(code)).selectFirst(context);
        if (type == null || !ACTIVE.equalsIgnoreCase(type.getAstatus())) {
            throw new IllegalArgumentException("Aktiver Unterlagentyp fehlt: " + code);
        }
        return type;
    }

    private Benutzer findActor(ObjectContext context) {
        Benutzer actor = ObjectSelect.query(Benutzer.class)
                .where(Benutzer.USERNAME.eq(currentActor.username())).selectFirst(context);
        if (actor == null) {
            throw new IllegalStateException("Registrierender ist kein fachlicher Benutzer: " + currentActor.username());
        }
        return actor;
    }

    private void requireEditableDossier(Dossier dossier) {
        if ("Geschlossen".equalsIgnoreCase(dossier.getAstatus())
                || "Archiviert".equalsIgnoreCase(dossier.getAstatus())
                || "Vernichtet".equalsIgnoreCase(dossier.getAstatus())) {
            throw new DomainRuleViolationException("Dossier ist nicht mehr bearbeitbar.");
        }
    }

    private void requireBusinessContext(Dossier dossier, Geschaeft business) {
        if (business == null) {
            if (dossier == null) {
                throw new IllegalArgumentException("Dossier ist erforderlich.");
            }
            return;
        }
        if (business.getDossier() == null || dossier == null
                || !business.getDossier().getDossiernummer().equals(dossier.getDossiernummer())) {
            throw new DomainRuleViolationException("Unterlage und Geschäft müssen demselben Dossier angehören.");
        }
        if ("Abgeschlossen".equalsIgnoreCase(business.getLifecyclestatus())
                || "Archiviert".equalsIgnoreCase(business.getLifecyclestatus())
                || "Vernichtet".equalsIgnoreCase(business.getLifecyclestatus())) {
            throw new DomainRuleViolationException("Geschäft ist nicht mehr bearbeitbar.");
        }
    }
}
