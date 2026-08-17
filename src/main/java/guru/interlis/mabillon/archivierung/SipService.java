package guru.interlis.mabillon.archivierung;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.UUID;

import guru.interlis.mabillon.domain.DomainRuleViolationException;
import guru.interlis.mabillon.journal.EreignisObjektTyp;
import guru.interlis.mabillon.journal.EreignisTyp;
import guru.interlis.mabillon.journal.JournalCommand;
import guru.interlis.mabillon.journal.JournalService;
import guru.interlis.mabillon.numbering.ArchivAblieferungNumber;
import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Archivablieferung;
import guru.interlis.mabillon.persistence.cayenne.ArchivablieferungDossier;
import guru.interlis.mabillon.persistence.cayenne.Benutzer;
import guru.interlis.mabillon.persistence.cayenne.Dossier;
import guru.interlis.mabillon.persistence.cayenne.Sippaket;
import guru.interlis.mabillon.quality.DataQualityService;
import guru.interlis.mabillon.security.AuthorizationException;
import guru.interlis.mabillon.security.AuthorizationService;
import guru.interlis.mabillon.security.CurrentActor;
import guru.interlis.mabillon.security.Permission;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.stereotype.Service;

@Service
public final class SipService {

    private static final String READY = "Bereit";
    private static final String CORRECTION_REQUIRED = "Korrektur_erforderlich";
    private static final String SIP_CREATED = "SIP_Erstellt";
    private static final String VALIDATED = "Validiert";
    private static final String GENERATED = "Erzeugt";
    private static final String NOT_VALIDATED = "Nicht_validiert";

    private final CayenneUnitOfWork unitOfWork;
    private final SipGenerator generator;
    private final SipValidator validator;
    private final ArchivePathConfiguration paths;
    private final DataQualityService dataQualityService;
    private final AuthorizationService authorizationService;
    private final CurrentActor currentActor;
    private final JournalService journalService;
    private final Clock clock;

    public SipService(
            CayenneUnitOfWork unitOfWork,
            SipGenerator generator,
            SipValidator validator,
            ArchivePathConfiguration paths,
            DataQualityService dataQualityService,
            AuthorizationService authorizationService,
            CurrentActor currentActor,
            JournalService journalService,
            Clock clock) {
        this.unitOfWork = unitOfWork;
        this.generator = generator;
        this.validator = validator;
        this.paths = paths;
        this.dataQualityService = dataQualityService;
        this.authorizationService = authorizationService;
        this.currentActor = currentActor;
        this.journalService = journalService;
        this.clock = clock;
    }

    public SippaketView generate(ArchivAblieferungNumber deliveryNumber) {
        authorizationService.require(Permission.MANAGE_ARCHIVE_DELIVERY);
        DeliverySnapshot delivery = unitOfWork.read(context -> snapshot(context, deliveryNumber));
        if (!READY.equalsIgnoreCase(delivery.status()) && !CORRECTION_REQUIRED.equalsIgnoreCase(delivery.status())) {
            throw new DomainRuleViolationException("SIP kann nur aus einer bereiten oder zu korrigierenden Archivablieferung erzeugt werden.");
        }
        delivery.dossiers().forEach(dossier -> {
            if (!"Geschlossen".equalsIgnoreCase(dossier.getAstatus())) {
                throw new DomainRuleViolationException("Nur geschlossene Dossiers dürfen in ein SIP gelangen.");
            }
            if ("Archiviert".equalsIgnoreCase(dossier.getAstatus())
                    || "Vernichtet".equalsIgnoreCase(dossier.getAstatus())
                    || dossier.getArchivierungs().stream().anyMatch(archive ->
                            "Uebernommen".equalsIgnoreCase(archive.getAstatus())
                                    || "Vernichtet".equalsIgnoreCase(archive.getAstatus()))) {
                throw new DomainRuleViolationException("Dossier wurde bereits übernommen oder vernichtet.");
            }
            if (dataQualityService.checkDossier(DossierNumber.parse(dossier.getDossiernummer())).hasErrors()) {
                throw new DomainRuleViolationException("Dossier verletzt Datenqualitätsregeln: " + dossier.getDossiernummer());
            }
        });

        int attempt = delivery.nextAttempt();
        Path target = paths.sipRoot()
                .resolve("SIP_%s_%s_%06d".formatted(LocalDateTime.now(clock).toLocalDate(),
                        deliveryNumber.value(), attempt) + "_" + UUID.randomUUID()).toAbsolutePath().normalize();
        GeneratedSip generated;
        try {
            generated = generator.generate(new SipGenerationRequest(
                    deliveryNumber, SipProfile.ECH_0160_1_3_0, target));
        } catch (RuntimeException failure) {
            throw new SipGenerationException("SIP konnte nicht erzeugt werden: " + deliveryNumber.value(), failure);
        }
        return unitOfWork.write(context -> {
            Archivablieferung deliveryEntity = findDelivery(context, deliveryNumber);
            Sippaket packet = context.newObject(Sippaket.class);
            packet.setAstatus(GENERATED);
            packet.setValidierungsstatus(NOT_VALIDATED);
            packet.setErstelltam(LocalDateTime.now(clock));
            packet.setDateigroesse(generated.size());
            packet.setHashsha256(generated.sha256());
            packet.setStorageuri(generated.path().toUri().toString());
            packet.setLaufnummer(attempt);
            packet.setArchivablieferung(deliveryEntity);
            packet.setBenutzer(actor(context));
            packet.setTBasket(deliveryEntity.getTBasket());
            packet.setTIliTid(UUID.randomUUID());
            deliveryEntity.setAstatus(SIP_CREATED);
            record(context, EreignisObjektTyp.SipPaket, packet.getTIliTid().toString(), EreignisTyp.SIP_erzeugt,
                    "SIP erzeugt: " + deliveryNumber.value());
            return toView(packet);
        });
    }

    public SippaketView validate(ArchivAblieferungNumber deliveryNumber, int attempt) {
        authorizationService.require(Permission.MANAGE_ARCHIVE_DELIVERY);
        SippaketSnapshot packet = unitOfWork.read(context -> packetSnapshot(context, deliveryNumber, attempt));
        Path packagePath = toPath(packet.storageUri());
        SipValidationResult result = validator.validate(packagePath);
        return unitOfWork.write(context -> {
            Archivablieferung delivery = findDelivery(context, deliveryNumber);
            Sippaket entity = findPacket(delivery, attempt);
            entity.setValidierungsstatus(result.status().name());
            entity.setValidiertam(LocalDateTime.now(clock));
            entity.setValidierungsberichturi(result.reportPath() == null ? null : result.reportPath().toUri().toString());
            entity.setBemerkung(result.messages().stream().findFirst().map(SipValidationMessage::message).orElse(null));
            if (result.valid()) {
                delivery.setAstatus(VALIDATED);
            } else {
                delivery.setAstatus(CORRECTION_REQUIRED);
            }
            record(context, EreignisObjektTyp.SipPaket, entity.getTIliTid().toString(), EreignisTyp.SIP_validiert,
                    "SIP validiert: " + result.status());
            return toView(entity);
        });
    }

    public SippaketView validateLatest(ArchivAblieferungNumber deliveryNumber) {
        authorizationService.require(Permission.MANAGE_ARCHIVE_DELIVERY);
        int attempt = unitOfWork.read(context -> snapshot(context, deliveryNumber).nextAttempt() - 1);
        if (attempt < 1) {
            throw new IllegalArgumentException("Archivablieferung enthält kein SIP.");
        }
        return validate(deliveryNumber, attempt);
    }

    private DeliverySnapshot snapshot(ObjectContext context, ArchivAblieferungNumber number) {
        Archivablieferung delivery = findDelivery(context, number);
        return new DeliverySnapshot(delivery.getAstatus(), delivery.getArchivablieferungDossiers().stream()
                .map(ArchivablieferungDossier::getDossier).toList(), delivery.getSippakets().stream()
                .map(Sippaket::getLaufnummer).max(Comparator.naturalOrder()).orElse(0) + 1);
    }

    private SippaketSnapshot packetSnapshot(ObjectContext context, ArchivAblieferungNumber number, int attempt) {
        Sippaket packet = findPacket(findDelivery(context, number), attempt);
        return new SippaketSnapshot(packet.getStorageuri());
    }

    private Archivablieferung findDelivery(ObjectContext context, ArchivAblieferungNumber number) {
        Archivablieferung delivery = ObjectSelect.query(Archivablieferung.class)
                .where(Archivablieferung.ABLIEFERUNGSNUMMER.eq(number.value())).selectFirst(context);
        if (delivery == null) {
            throw new IllegalArgumentException("Unbekannte Archivablieferung: " + number.value());
        }
        return delivery;
    }

    private Sippaket findPacket(Archivablieferung delivery, int attempt) {
        return delivery.getSippakets().stream().filter(packet -> packet.getLaufnummer() == attempt).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unbekannter SIP-Versuch: " + attempt));
    }

    private Path toPath(String storageUri) {
        if (storageUri == null || !storageUri.startsWith("file:")) {
            throw new IllegalStateException("SIP hat keine lokale Ablage-URI.");
        }
        Path path = Path.of(java.net.URI.create(storageUri)).toAbsolutePath().normalize();
        if (!path.startsWith(paths.sipRoot())) {
            throw new IllegalStateException("SIP liegt ausserhalb des konfigurierten SIP-Speichers.");
        }
        return path;
    }

    private Benutzer actor(ObjectContext context) {
        String username = currentActor.username();
        Benutzer actor = ObjectSelect.query(Benutzer.class).where(Benutzer.USERNAME.eq(username)).selectFirst(context);
        if (actor == null) {
            throw new AuthorizationException("Kein fachlicher Benutzer für Identität: " + username);
        }
        return actor;
    }

    private void record(ObjectContext context, EreignisObjektTyp objectType, String objectId, EreignisTyp type,
            String message) {
        journalService.record(context, new JournalCommand(objectType, objectId, type, message,
                currentActor.id(), Instant.now(clock)));
    }

    private SippaketView toView(Sippaket packet) {
        return new SippaketView(packet.getLaufnummer(), packet.getAstatus(), packet.getValidierungsstatus(),
                packet.getDateigroesse() == null ? 0L : packet.getDateigroesse(), packet.getHashsha256(),
                packet.getStorageuri(), packet.getValidierungsberichturi());
    }

    private record DeliverySnapshot(String status, java.util.List<Dossier> dossiers, int nextAttempt) {
    }

    private record SippaketSnapshot(String storageUri) {
    }
}
