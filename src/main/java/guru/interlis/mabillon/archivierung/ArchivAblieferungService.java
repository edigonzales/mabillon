package guru.interlis.mabillon.archivierung;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import guru.interlis.mabillon.domain.DomainRuleViolationException;
import guru.interlis.mabillon.journal.EreignisObjektTyp;
import guru.interlis.mabillon.journal.EreignisTyp;
import guru.interlis.mabillon.journal.JournalCommand;
import guru.interlis.mabillon.journal.JournalService;
import guru.interlis.mabillon.numbering.ArchivAblieferungNumber;
import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.numbering.NumberingService;
import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Archivablieferung;
import guru.interlis.mabillon.persistence.cayenne.ArchivablieferungDossier;
import guru.interlis.mabillon.persistence.cayenne.Archivierung;
import guru.interlis.mabillon.persistence.cayenne.Benutzer;
import guru.interlis.mabillon.persistence.cayenne.Dossier;
import guru.interlis.mabillon.persistence.cayenne.Geschaeft;
import guru.interlis.mabillon.persistence.cayenne.Sippaket;
import guru.interlis.mabillon.persistence.cayenne.Unterlage;
import guru.interlis.mabillon.quality.DataQualityService;
import guru.interlis.mabillon.security.AuthorizationService;
import guru.interlis.mabillon.security.CurrentActor;
import guru.interlis.mabillon.security.Permission;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.stereotype.Service;

@Service
public final class ArchivAblieferungService {

    private static final String DRAFT = "Entwurf";
    private static final String READY = "Bereit";
    private static final String SIP_CREATED = "SIP_Erstellt";
    private static final String VALIDATED = "Validiert";
    private static final String TRANSFERRED = "Uebergeben";
    private static final String ACCEPTED = "Uebernommen";
    private static final String REJECTED = "Abgelehnt";
    private static final String OFFERED = "Angeboten";
    private static final String ARCHIVED = "Archiviert";
    private static final String DESTROYED = "Vernichtet";

    private final CayenneUnitOfWork unitOfWork;
    private final NumberingService numberingService;
    private final JournalService journalService;
    private final AuthorizationService authorizationService;
    private final CurrentActor currentActor;
    private final Clock clock;
    private final DataQualityService dataQualityService;

    public ArchivAblieferungService(
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

    public ArchivAblieferungView create(CreateArchivAblieferungCommand command) {
        authorizationService.require(Permission.MANAGE_ARCHIVE_DELIVERY);
        LocalDate today = LocalDate.now(clock);
        return unitOfWork.write(context -> {
            ArchivAblieferungNumber number;
            do {
                number = numberingService.nextArchivAblieferungNumber(command.organisationCode(), today);
            } while (existingDelivery(context, number) != null);
            Archivablieferung delivery = context.newObject(Archivablieferung.class);
            delivery.setAblieferungsnummer(number.value());
            delivery.setTitel(command.title().trim());
            delivery.setArchivempfaenger(command.archivempfaenger().trim());
            delivery.setBemerkung(command.bemerkung());
            delivery.setAstatus(DRAFT);
            delivery.setErstelltam(LocalDateTime.now(clock));
            delivery.setBenutzer(requireActor(context));
            delivery.setTBasket(businessBasket(context));
            delivery.setTIliTid(UUID.randomUUID());
            record(context, EreignisObjektTyp.ArchivAblieferung, number.value(), EreignisTyp.Erstellt,
                    "Archivablieferung erstellt.");
            return toView(delivery);
        });
    }

    public ArchivAblieferungView addDossier(ArchivAblieferungNumber deliveryNumber, DossierNumber dossierNumber) {
        authorizationService.require(Permission.MANAGE_ARCHIVE_DELIVERY);
        return unitOfWork.write(context -> {
            Archivablieferung delivery = findDelivery(context, deliveryNumber);
            requireStatus(delivery, DRAFT);
            Dossier dossier = findDossier(context, dossierNumber);
            requireEligible(dossier, dossierNumber);
            if (delivery.getArchivablieferungDossiers().stream()
                    .anyMatch(link -> link.getDossier() != null
                            && dossierNumber.value().equals(link.getDossier().getDossiernummer()))) {
                throw new DomainRuleViolationException("Dossier ist bereits Bestandteil der Archivablieferung.");
            }
            ArchivablieferungDossier link = context.newObject(ArchivablieferungDossier.class);
            link.setArchivablieferung(delivery);
            link.setDossier(dossier);
            link.setTBasket(delivery.getTBasket());
            record(context, EreignisObjektTyp.ArchivAblieferung, delivery.getAblieferungsnummer(),
                    EreignisTyp.Geaendert, "Dossier der Archivablieferung hinzugefügt.");
            return toView(delivery);
        });
    }

    public ArchivAblieferungView removeDossier(ArchivAblieferungNumber deliveryNumber, DossierNumber dossierNumber) {
        authorizationService.require(Permission.MANAGE_ARCHIVE_DELIVERY);
        return unitOfWork.write(context -> {
            Archivablieferung delivery = findDelivery(context, deliveryNumber);
            requireStatus(delivery, DRAFT);
            ArchivablieferungDossier link = delivery.getArchivablieferungDossiers().stream()
                    .filter(candidate -> candidate.getDossier() != null
                            && dossierNumber.value().equals(candidate.getDossier().getDossiernummer()))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Dossier ist nicht Bestandteil der Archivablieferung."));
            context.deleteObject(link);
            record(context, EreignisObjektTyp.ArchivAblieferung, delivery.getAblieferungsnummer(),
                    EreignisTyp.Geaendert, "Dossier aus Archivablieferung entfernt.");
            return toView(delivery);
        });
    }

    public ArchivAblieferungView markReady(ArchivAblieferungNumber deliveryNumber) {
        authorizationService.require(Permission.MANAGE_ARCHIVE_DELIVERY);
        return unitOfWork.write(context -> {
            Archivablieferung delivery = findDelivery(context, deliveryNumber);
            requireStatus(delivery, DRAFT, REJECTED);
            if (delivery.getArchivablieferungDossiers().isEmpty()) {
                throw new DomainRuleViolationException("Eine Archivablieferung benötigt mindestens ein Dossier.");
            }
            delivery.getArchivablieferungDossiers().forEach(link -> requireEligible(link.getDossier(),
                    DossierNumber.parse(link.getDossier().getDossiernummer())));
            delivery.setAstatus(READY);
            for (ArchivablieferungDossier link : delivery.getArchivablieferungDossiers()) {
                Archivierung archive = archiveRecord(context, link.getDossier());
                archive.setAstatus(OFFERED);
                archive.setAngebotenam(LocalDate.now(clock));
                archive.setTBasket(link.getDossier().getTBasket());
                archive.setTIliTid(archive.getTIliTid() == null ? UUID.randomUUID() : archive.getTIliTid());
                record(context, EreignisObjektTyp.Dossier, link.getDossier().getDossiernummer(),
                        EreignisTyp.Archivierung_geaendert, "Dossier zur Archivierung angeboten.");
            }
            record(context, EreignisObjektTyp.ArchivAblieferung, delivery.getAblieferungsnummer(),
                    EreignisTyp.Status_geaendert, "Archivablieferung ist bereit.");
            return toView(delivery);
        });
    }

    public ArchivAblieferungView recordTransferred(ArchivAblieferungNumber deliveryNumber, String note) {
        authorizationService.require(Permission.MANAGE_ARCHIVE_DELIVERY);
        return unitOfWork.write(context -> {
            Archivablieferung delivery = findDelivery(context, deliveryNumber);
            requireStatus(delivery, VALIDATED);
            delivery.setAstatus(TRANSFERRED);
            delivery.setUebergebenam(LocalDateTime.now(clock));
            appendRemark(delivery, note);
            record(context, EreignisObjektTyp.ArchivAblieferung, delivery.getAblieferungsnummer(),
                    EreignisTyp.Archivablieferung_uebergeben, "Archivablieferung übergeben.");
            return toView(delivery);
        });
    }

    public ArchivAblieferungView recordAccepted(ArchivAblieferungNumber deliveryNumber, String archiveSignature,
            String note) {
        authorizationService.require(Permission.MANAGE_ARCHIVE_DELIVERY);
        if (archiveSignature == null || archiveSignature.isBlank()) {
            throw new IllegalArgumentException("Eine Archivsignatur ist erforderlich.");
        }
        return unitOfWork.write(context -> {
            Archivablieferung delivery = findDelivery(context, deliveryNumber);
            requireStatus(delivery, TRANSFERRED);
            delivery.setAstatus(ACCEPTED);
            delivery.setUebernommenam(LocalDateTime.now(clock));
            appendRemark(delivery, note);
            for (ArchivablieferungDossier link : delivery.getArchivablieferungDossiers()) {
                Archivierung archive = archiveRecord(context, link.getDossier());
                archive.setAstatus(ACCEPTED);
                archive.setEntscheidam(LocalDate.now(clock));
                archive.setArchivsignatur(archiveSignature.trim());
                archive.setBemerkung(note);
                link.getDossier().setAstatus(ARCHIVED);
                for (Geschaeft business : link.getDossier().getGeschaefts()) {
                    business.setLifecyclestatus(ARCHIVED);
                }
                record(context, EreignisObjektTyp.Dossier, link.getDossier().getDossiernummer(),
                        EreignisTyp.Archivierung_geaendert, "Dossier vom Archiv übernommen.");
            }
            record(context, EreignisObjektTyp.ArchivAblieferung, delivery.getAblieferungsnummer(),
                    EreignisTyp.Archivablieferung_uebernommen, "Archivablieferung vom Archiv übernommen.");
            return toView(delivery);
        });
    }

    public ArchivAblieferungView recordRejected(ArchivAblieferungNumber deliveryNumber, String note) {
        authorizationService.require(Permission.MANAGE_ARCHIVE_DELIVERY);
        return unitOfWork.write(context -> {
            Archivablieferung delivery = findDelivery(context, deliveryNumber);
            requireStatus(delivery, TRANSFERRED, VALIDATED);
            delivery.setAstatus(REJECTED);
            appendRemark(delivery, note);
            for (ArchivablieferungDossier link : delivery.getArchivablieferungDossiers()) {
                Archivierung archive = archiveRecord(context, link.getDossier());
                archive.setAstatus(REJECTED);
                archive.setEntscheidam(LocalDate.now(clock));
                archive.setBemerkung(note);
            }
            record(context, EreignisObjektTyp.ArchivAblieferung, delivery.getAblieferungsnummer(),
                    EreignisTyp.Archivablieferung_abgelehnt, "Archivablieferung vom Archiv abgelehnt.");
            return toView(delivery);
        });
    }

    public ArchivAblieferungView get(ArchivAblieferungNumber deliveryNumber) {
        authorizationService.require(Permission.MANAGE_ARCHIVE_DELIVERY);
        return unitOfWork.read(context -> toView(findDelivery(context, deliveryNumber)));
    }

    private void requireEligible(Dossier dossier, DossierNumber number) {
        if (dossier == null) {
            throw new IllegalArgumentException("Unbekanntes Dossier: " + number.value());
        }
        if (!"Geschlossen".equalsIgnoreCase(dossier.getAstatus())) {
            throw new DomainRuleViolationException("Nur geschlossene Dossiers können angeboten werden.");
        }
        if (ARCHIVED.equalsIgnoreCase(dossier.getAstatus()) || DESTROYED.equalsIgnoreCase(dossier.getAstatus())) {
            throw new DomainRuleViolationException("Dossier ist bereits archiviert oder vernichtet.");
        }
        if (dossier.getArchivierungs().stream().anyMatch(archive -> ACCEPTED.equalsIgnoreCase(archive.getAstatus())
                || DESTROYED.equalsIgnoreCase(archive.getAstatus()))) {
            throw new DomainRuleViolationException("Dossier wurde bereits übernommen oder vernichtet.");
        }
        if (dataQualityService.checkDossier(number).hasErrors()) {
            throw new DomainRuleViolationException("Dossier verletzt Datenqualitätsregeln.");
        }
    }

    private Archivierung archiveRecord(ObjectContext context, Dossier dossier) {
        return dossier.getArchivierungs().stream().findFirst().orElseGet(() -> {
            Archivierung archive = context.newObject(Archivierung.class);
            archive.setDossier(dossier);
            archive.setTBasket(dossier.getTBasket());
            archive.setTIliTid(UUID.randomUUID());
            return archive;
        });
    }

    private Archivablieferung findDelivery(ObjectContext context, ArchivAblieferungNumber number) {
        Archivablieferung delivery = existingDelivery(context, number);
        if (delivery == null) {
            throw new IllegalArgumentException("Unbekannte Archivablieferung: " + number.value());
        }
        return delivery;
    }

    private Archivablieferung existingDelivery(ObjectContext context, ArchivAblieferungNumber number) {
        return ObjectSelect.query(Archivablieferung.class)
                .where(Archivablieferung.ABLIEFERUNGSNUMMER.eq(number.value())).selectFirst(context);
    }

    private Dossier findDossier(ObjectContext context, DossierNumber number) {
        return ObjectSelect.query(Dossier.class).where(Dossier.DOSSIERNUMMER.eq(number.value())).selectFirst(context);
    }

    private void requireStatus(Archivablieferung delivery, String... allowed) {
        for (String status : allowed) {
            if (status.equalsIgnoreCase(delivery.getAstatus())) {
                return;
            }
        }
        throw new DomainRuleViolationException("Archivablieferung hat nicht den erwarteten Status: "
                + delivery.getAstatus());
    }

    private void appendRemark(Archivablieferung delivery, String note) {
        if (note != null && !note.isBlank()) {
            delivery.setBemerkung(delivery.getBemerkung() == null || delivery.getBemerkung().isBlank()
                    ? note.trim() : delivery.getBemerkung() + " / " + note.trim());
        }
    }

    private Benutzer requireActor(ObjectContext context) {
        String username = currentActor.id().value();
        Benutzer actor = ObjectSelect.query(Benutzer.class).where(Benutzer.USERNAME.eq(username)).selectFirst(context);
        if (actor == null) {
            throw new IllegalStateException("Archivakteur ist kein fachlicher Benutzer: " + username);
        }
        return actor;
    }

    private long businessBasket(ObjectContext context) {
        Dossier dossier = ObjectSelect.query(Dossier.class).selectFirst(context);
        if (dossier != null) {
            return dossier.getTBasket();
        }
        Geschaeft business = ObjectSelect.query(Geschaeft.class).selectFirst(context);
        if (business != null) {
            return business.getTBasket();
        }
        throw new IllegalStateException("Kein Geschäftsdaten-Basket vorhanden.");
    }

    private void record(ObjectContext context, EreignisObjektTyp objectType, String objectId, EreignisTyp type,
            String message) {
        journalService.record(context, new JournalCommand(objectType, objectId, type, message,
                currentActor.id(), Instant.now(clock)));
    }

    private ArchivAblieferungView toView(Archivablieferung delivery) {
        List<ArchivAblieferungView.DossierArchiveView> dossiers = delivery.getArchivablieferungDossiers().stream()
                .map(link -> {
                    Dossier dossier = link.getDossier();
                    Archivierung archive = dossier == null ? null : dossier.getArchivierungs().stream().findFirst().orElse(null);
                    return new ArchivAblieferungView.DossierArchiveView(
                            dossier == null ? "" : dossier.getDossiernummer(),
                            dossier == null ? "" : dossier.getTitel(),
                            dossier == null ? "" : dossier.getAstatus(),
                            archive == null ? "" : archive.getAstatus());
                }).sorted(Comparator.comparing(ArchivAblieferungView.DossierArchiveView::dossierNumber)).toList();
        List<SippaketView> packages = delivery.getSippakets().stream()
                .sorted(Comparator.comparingInt(Sippaket::getLaufnummer))
                .map(value -> new SippaketView(value.getLaufnummer(), value.getAstatus(), value.getValidierungsstatus(),
                        value.getDateigroesse() == null ? 0L : value.getDateigroesse(), value.getHashsha256(),
                        value.getStorageuri(), value.getValidierungsberichturi())).toList();
        return new ArchivAblieferungView(delivery.getAblieferungsnummer(), delivery.getTitel(), delivery.getAstatus(),
                delivery.getArchivempfaenger(), dossiers, packages);
    }
}
