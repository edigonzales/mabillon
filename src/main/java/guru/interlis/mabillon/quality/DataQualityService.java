package guru.interlis.mabillon.quality;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import guru.interlis.mabillon.numbering.ArchivAblieferungNumber;
import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.numbering.GeschaeftNumber;
import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Archivablieferung;
import guru.interlis.mabillon.persistence.cayenne.ArchivablieferungDossier;
import guru.interlis.mabillon.persistence.cayenne.Dossier;
import guru.interlis.mabillon.persistence.cayenne.Geschaeft;
import guru.interlis.mabillon.persistence.cayenne.Unterlage;
import guru.interlis.mabillon.storage.DocumentStorage;
import guru.interlis.mabillon.security.AuthorizationService;
import guru.interlis.mabillon.security.Permission;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.stereotype.Service;

@Service
public final class DataQualityService {

    private final CayenneUnitOfWork unitOfWork;
    private final DocumentStorage storage;
    private final AuthorizationService authorizationService;

    public DataQualityService(
            CayenneUnitOfWork unitOfWork,
            DocumentStorage storage,
            AuthorizationService authorizationService) {
        this.unitOfWork = unitOfWork;
        this.storage = storage;
        this.authorizationService = authorizationService;
    }

    public QualityReport checkDossier(DossierNumber number) {
        authorizationService.require(Permission.RUN_DATA_QUALITY);
        return unitOfWork.read(context -> {
            Dossier dossier = findDossier(context, number.value());
            if (dossier == null) {
                throw new IllegalArgumentException("Unbekanntes Dossier: " + number.value());
            }
            List<QualityFinding> findings = new ArrayList<>();
            checkDossierPosition(dossier, findings);
            dossier.getGeschaefts().forEach(business -> checkBusiness(business, findings));
            dossier.getUnterlages().forEach(document -> checkDocument(document, dossier, findings));
            ObjectSelect.query(Geschaeft.class).select(context).stream()
                    .filter(business -> business.getDossier() == null)
                    .forEach(business -> finding(findings, "DQ-002", QualitySeverity.WARNING,
                            "Geschaeft", business.getGeschaeftsnummer(), "Geschäft ist keinem Dossier zugeordnet."));
            ObjectSelect.query(Unterlage.class).select(context).stream()
                    .filter(document -> document.getDossier() == null)
                    .forEach(document -> finding(findings, "DQ-004", QualitySeverity.WARNING,
                            "Unterlage", id(document), "Unterlage ist keinem Dossier zugeordnet."));
            return new QualityReport("Dossier", number.value(), findings);
        });
    }

    public QualityReport checkGeschaeft(GeschaeftNumber number) {
        authorizationService.require(Permission.RUN_DATA_QUALITY);
        return unitOfWork.read(context -> {
            Geschaeft business = findBusiness(context, number.value());
            if (business == null) {
                throw new IllegalArgumentException("Unbekanntes Geschäft: " + number.value());
            }
            List<QualityFinding> findings = new ArrayList<>();
            checkBusiness(business, findings);
            business.getUnterlages().forEach(document -> checkDocument(document, business.getDossier(), findings));
            if (business.getDossier() != null) {
                checkDossierPosition(business.getDossier(), findings);
            }
            return new QualityReport("Geschaeft", number.value(), findings);
        });
    }

    public QualityReport checkArchiveDelivery(ArchivAblieferungNumber number) {
        authorizationService.require(Permission.RUN_DATA_QUALITY);
        return unitOfWork.read(context -> {
            Archivablieferung delivery = ObjectSelect.query(Archivablieferung.class)
                    .where(Archivablieferung.ABLIEFERUNGSNUMMER.eq(number.value()))
                    .selectFirst(context);
            if (delivery == null) {
                throw new IllegalArgumentException("Unbekannte Archivablieferung: " + number.value());
            }
            List<QualityFinding> findings = new ArrayList<>();
            for (ArchivablieferungDossier link : delivery.getArchivablieferungDossiers()) {
                Dossier dossier = link.getDossier();
                if (dossier == null || !"Geschlossen".equalsIgnoreCase(dossier.getAstatus())) {
                    finding(findings, "DQ-013", QualitySeverity.ERROR, "ArchivAblieferung", number.value(),
                            "Archivablieferung enthält ein nicht geschlossenes Dossier.");
                }
            }
            return new QualityReport("ArchivAblieferung", number.value(), findings);
        });
    }

    private void checkDossierPosition(Dossier dossier, List<QualityFinding> findings) {
        if (dossier.getOrdnungssystemposition() == null
                || dossier.getOrdnungssystemposition().getOrdnungssystem() == null
                || !active(dossier.getOrdnungssystemposition().getAstatus())
                || !active(dossier.getOrdnungssystemposition().getOrdnungssystem().getAstatus())) {
            finding(findings, "DQ-001", QualitySeverity.WARNING, "Dossier", dossier.getDossiernummer(),
                    "Dossier hat keine gültige Registraturplanposition.");
        }
    }

    private void checkBusiness(Geschaeft business, List<QualityFinding> findings) {
        if (business.getDossier() == null) {
            finding(findings, "DQ-002", QualitySeverity.WARNING, "Geschaeft", business.getGeschaeftsnummer(),
                    "Geschäft ist keinem Dossier zugeordnet.");
        }
        if (business.getGeschaeftsart() == null || !active(business.getGeschaeftsart().getAstatus())) {
            finding(findings, "DQ-003", QualitySeverity.WARNING, "Geschaeft", business.getGeschaeftsnummer(),
                    "Geschäft hat keine gültige Geschäftsart.");
        }
        if (isClosed(business) && business.getAufgabes().stream().anyMatch(task -> !isTaskClosed(task.getAstatus()))) {
            finding(findings, "DQ-006", QualitySeverity.ERROR, "Geschaeft", business.getGeschaeftsnummer(),
                    "Abgeschlossenes Geschäft enthält offene Aufgaben.");
        }
        if (business.getProzessstatus() == null
                || business.getGeschaeftsart() == null
                || business.getProzessstatus().getGeschaeftsart() == null
                || !business.getGeschaeftsart().getAcode()
                        .equals(business.getProzessstatus().getGeschaeftsart().getAcode())) {
            finding(findings, "DQ-008", QualitySeverity.ERROR, "Geschaeft", business.getGeschaeftsnummer(),
                    "Prozessstatus passt nicht zur Geschäftsart.");
        }
        if (business.getResultatstatus() != null && (business.getGeschaeftsart() == null
                || business.getResultatstatus().getGeschaeftsart() == null
                || !business.getGeschaeftsart().getAcode()
                        .equals(business.getResultatstatus().getGeschaeftsart().getAcode()))) {
            finding(findings, "DQ-009", QualitySeverity.ERROR, "Geschaeft", business.getGeschaeftsnummer(),
                    "Resultatstatus passt nicht zur Geschäftsart.");
        }
        if (isClosed(business) && business.getGeschaeftsart() != null
                && business.getGeschaeftsart().isResultaterforderlich()
                && business.getResultatstatus() == null) {
            finding(findings, "DQ-010", QualitySeverity.ERROR, "Geschaeft", business.getGeschaeftsnummer(),
                    "Resultatpflichtiges abgeschlossenes Geschäft hat kein Resultat.");
        }
    }

    private void checkDocument(Unterlage document, Dossier dossier, List<QualityFinding> findings) {
        if (document.getDossier() == null) {
            finding(findings, "DQ-004", QualitySeverity.WARNING, "Unterlage", id(document),
                    "Unterlage ist keinem Dossier zugeordnet.");
        }
        if (document.getGeschaeft() != null && (dossier == null || document.getGeschaeft().getDossier() == null
                || !dossier.getDossiernummer().equals(document.getGeschaeft().getDossier().getDossiernummer()))) {
            finding(findings, "DQ-005", QualitySeverity.ERROR, "Unterlage", id(document),
                    "Unterlage verweist auf ein Geschäft eines anderen Dossiers.");
        }
        if (document.isAktenrelevant() && !"Storniert".equalsIgnoreCase(document.getAstatus())
                && (document.getStorageuri() == null || !storage.exists(document.getStorageuri()))) {
            finding(findings, "DQ-011", QualitySeverity.ERROR, "Unterlage", id(document),
                    "Aktenrelevante registrierte Unterlage hat keine vorhandene Datei.");
        } else if (document.getStorageuri() != null && document.getHashsha256() != null
                && document.getStorageuri().startsWith("mabillon:objects/") && storage.exists(document.getStorageuri())
                && !document.getHashsha256().equalsIgnoreCase(hash(document.getStorageuri()))) {
            finding(findings, "DQ-012", QualitySeverity.WARNING, "Unterlage", id(document),
                    "Datei-Hash stimmt nicht mit dem Storage-Inhalt überein.");
        }
    }

    private String hash(String uri) {
        try (InputStream input = storage.open(uri)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException failure) {
            throw new IllegalStateException("Datei-Hash konnte nicht geprüft werden.", failure);
        }
    }

    private static boolean active(String status) {
        return status != null && "aktiv".equalsIgnoreCase(status);
    }

    private static boolean isClosed(Geschaeft business) {
        return "Abgeschlossen".equalsIgnoreCase(business.getLifecyclestatus())
                || "Archiviert".equalsIgnoreCase(business.getLifecyclestatus())
                || "Vernichtet".equalsIgnoreCase(business.getLifecyclestatus());
    }

    private static boolean isTaskClosed(String status) {
        return "Erledigt".equalsIgnoreCase(status) || "Abgebrochen".equalsIgnoreCase(status);
    }

    private static String id(Unterlage document) {
        return document.getTIliTid() == null ? String.valueOf(document.getObjectId()) : document.getTIliTid().toString();
    }

    private static void finding(List<QualityFinding> findings, String code, QualitySeverity severity,
            String objectType, String objectId, String message) {
        findings.add(new QualityFinding(code, severity, objectType, objectId, message));
    }

    private static Dossier findDossier(ObjectContext context, String number) {
        return ObjectSelect.query(Dossier.class).where(Dossier.DOSSIERNUMMER.eq(number)).selectFirst(context);
    }

    private static Geschaeft findBusiness(ObjectContext context, String number) {
        return ObjectSelect.query(Geschaeft.class).where(Geschaeft.GESCHAEFTSNUMMER.eq(number)).selectFirst(context);
    }
}
