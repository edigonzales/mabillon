package guru.interlis.mabillon;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import guru.interlis.mabillon.archivierung.CreateArchivAblieferungCommand;
import guru.interlis.mabillon.aufgabe.CreateAufgabeCommand;
import guru.interlis.mabillon.geschaeft.GeschaeftView;
import guru.interlis.mabillon.numbering.ArchivAblieferungNumber;
import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.numbering.GeschaeftNumber;
import guru.interlis.mabillon.persistence.cayenne.Dossier;
import guru.interlis.mabillon.persistence.cayenne.Geschaeft;
import guru.interlis.mabillon.persistence.cayenne.Prozessstatus;
import guru.interlis.mabillon.persistence.cayenne.Resultatstatus;
import guru.interlis.mabillon.persistence.cayenne.Unterlage;
import guru.interlis.mabillon.storage.DocumentUpload;
import guru.interlis.mabillon.unterlage.RegisterUnterlageCommand;
import guru.interlis.mabillon.unterlage.UnterlageView;
import org.apache.cayenne.query.ObjectSelect;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DataQualityRulesIntegrationTest extends MabillonIntegrationTestSupport {

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void detectsInvalidRegistraturPositionAndBusinessType() {
        GeschaeftView business = newBusiness("DQ-001-003");
        DossierNumber dossierNumber = DossierNumber.parse(business.dossierNumber());
        GeschaeftNumber businessNumber = GeschaeftNumber.parse(business.number());

        unitOfWork.write(context -> {
            Dossier dossier = ObjectSelect.query(Dossier.class)
                    .where(Dossier.DOSSIERNUMMER.eq(dossierNumber.value())).selectFirst(context);
            Geschaeft entity = ObjectSelect.query(Geschaeft.class)
                    .where(Geschaeft.GESCHAEFTSNUMMER.eq(businessNumber.value())).selectFirst(context);
            dossier.getOrdnungssystemposition().setAstatus("inaktiv");
            entity.getGeschaeftsart().setAstatus("inaktiv");
        });

        assertRule(dataQualityService.checkDossier(dossierNumber), "DQ-001");
        assertRule(dataQualityService.checkGeschaeft(businessNumber), "DQ-003");
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void detectsDocumentPointingToBusinessOfAnotherDossier() {
        GeschaeftView first = newBusiness("DQ-005 first");
        GeschaeftView second = newBusiness("DQ-005 second");
        UnterlageView document = registerDocument(first, "dq005.txt", "DQ-005");

        unitOfWork.write(context -> {
            Unterlage entity = ObjectSelect.query(Unterlage.class)
                    .where(Unterlage.T_ILI_TID.eq(document.tid())).selectFirst(context);
            Geschaeft other = ObjectSelect.query(Geschaeft.class)
                    .where(Geschaeft.GESCHAEFTSNUMMER.eq(second.number())).selectFirst(context);
            entity.setGeschaeft(other);
        });

        assertRule(dataQualityService.checkDossier(DossierNumber.parse(first.dossierNumber())), "DQ-005");
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void detectsClosedBusinessWithOpenTaskAndClosedDossierWithOpenBusiness() {
        GeschaeftView taskBusiness = newBusiness("DQ-006");
        aufgabeService.create(new CreateAufgabeCommand(
                GeschaeftNumber.parse(taskBusiness.number()), "Offene DQ-Aufgabe", null,
                "RUECKFRAGE", null, 1, "anna.mueller", null));
        unitOfWork.write(context -> {
            ObjectSelect.query(Geschaeft.class)
                    .where(Geschaeft.GESCHAEFTSNUMMER.eq(taskBusiness.number())).selectFirst(context)
                    .setLifecyclestatus("Abgeschlossen");
        });
        assertRule(dataQualityService.checkGeschaeft(GeschaeftNumber.parse(taskBusiness.number())), "DQ-006");

        GeschaeftView openBusiness = newBusiness("DQ-007");
        unitOfWork.write(context -> {
            ObjectSelect.query(Dossier.class)
                    .where(Dossier.DOSSIERNUMMER.eq(openBusiness.dossierNumber())).selectFirst(context)
                    .setAstatus("Geschlossen");
        });
        assertRule(dataQualityService.checkDossier(DossierNumber.parse(openBusiness.dossierNumber())), "DQ-007");
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void detectsMismatchingStatusesAndMissingRequiredResult() {
        GeschaeftView mismatch = newBusiness("DQ-008-009");
        unitOfWork.write(context -> {
            Geschaeft entity = ObjectSelect.query(Geschaeft.class)
                    .where(Geschaeft.GESCHAEFTSNUMMER.eq(mismatch.number())).selectFirst(context);
            String ownType = entity.getGeschaeftsart().getAcode();
            Prozessstatus foreignProcess = ObjectSelect.query(Prozessstatus.class).select(context).stream()
                    .filter(value -> value.getGeschaeftsart() != null
                            && !ownType.equals(value.getGeschaeftsart().getAcode()))
                    .findFirst().orElseThrow();
            Resultatstatus foreignResult = ObjectSelect.query(Resultatstatus.class).select(context).stream()
                    .filter(value -> value.getGeschaeftsart() != null
                            && !ownType.equals(value.getGeschaeftsart().getAcode()))
                    .findFirst().orElseThrow();
            entity.setProzessstatus(foreignProcess);
            entity.setResultatstatus(foreignResult);
        });
        var mismatchReport = dataQualityService.checkGeschaeft(GeschaeftNumber.parse(mismatch.number()));
        assertRule(mismatchReport, "DQ-008");
        assertRule(mismatchReport, "DQ-009");

        GeschaeftView missingResult = newBusiness("DQ-010");
        unitOfWork.write(context -> {
            Geschaeft entity = ObjectSelect.query(Geschaeft.class)
                    .where(Geschaeft.GESCHAEFTSNUMMER.eq(missingResult.number())).selectFirst(context);
            entity.getGeschaeftsart().setResultaterforderlich(true);
            entity.setLifecyclestatus("Abgeschlossen");
            entity.setResultatstatus(null);
        });
        assertRule(dataQualityService.checkGeschaeft(GeschaeftNumber.parse(missingResult.number())), "DQ-010");
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void detectsMissingFileAndHashMismatch() throws Exception {
        GeschaeftView business = newBusiness("DQ-011-012");
        UnterlageView missing = registerDocument(business, "dq011.txt", "original-11");
        UnterlageView corrupt = registerDocument(business, "dq012.txt", "original-12");

        Files.delete(pathForStorageUri(missing.storageUri()));
        Files.writeString(pathForStorageUri(corrupt.storageUri()), "changed", StandardCharsets.UTF_8);

        var report = dataQualityService.checkGeschaeft(GeschaeftNumber.parse(business.number()));
        assertRule(report, "DQ-011");
        assertRule(report, "DQ-012");
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void detectsArchiveDeliveryContainingDossierThatIsNoLongerClosed() {
        GeschaeftView business = closeEligibleBusiness("DQ-013");
        DossierNumber dossierNumber = DossierNumber.parse(business.dossierNumber());
        var delivery = archivAblieferungService.create(new CreateArchivAblieferungCommand(
                "AGI", "DQ-013 Ablieferung", "Staatsarchiv", null));
        ArchivAblieferungNumber deliveryNumber = ArchivAblieferungNumber.parse(delivery.deliveryNumber());
        archivAblieferungService.addDossier(deliveryNumber, dossierNumber);

        unitOfWork.write(context -> {
            ObjectSelect.query(Dossier.class)
                    .where(Dossier.DOSSIERNUMMER.eq(dossierNumber.value())).selectFirst(context)
                    .setAstatus("Offen");
        });

        assertRule(dataQualityService.checkArchiveDelivery(deliveryNumber), "DQ-013");
    }

    private UnterlageView registerDocument(GeschaeftView business, String filename, String contents) {
        byte[] bytes = contents.getBytes(StandardCharsets.UTF_8);
        return unterlageService.register(new RegisterUnterlageCommand(
                DossierNumber.parse(business.dossierNumber()), GeschaeftNumber.parse(business.number()),
                filename, "AKTENNOTIZ", LocalDate.of(2026, 8, 18), LocalDate.of(2026, 8, 18),
                null, true, "TXT", null),
                new DocumentUpload(filename, "text/plain", new ByteArrayInputStream(bytes)));
    }

    private GeschaeftView closeEligibleBusiness(String label) {
        GeschaeftView business = newBusiness(label);
        GeschaeftNumber number = GeschaeftNumber.parse(business.number());
        geschaeftService.changeProcessStatus(new guru.interlis.mabillon.geschaeft.ChangeProcessStatusCommand(
                number, "ABGESCHLOSSEN", "DQ archive preparation"));
        geschaeftService.setResult(new guru.interlis.mabillon.geschaeft.SetResultCommand(
                number, "GENEHMIGT", "DQ archive result"));
        geschaeftService.close(number);
        dossierService.close(DossierNumber.parse(business.dossierNumber()));
        return business;
    }

    private static void assertRule(guru.interlis.mabillon.quality.QualityReport report, String code) {
        assertThat(report.findings()).anyMatch(finding -> code.equals(finding.ruleCode()));
    }

    private static Path pathForStorageUri(String storageUri) {
        String prefix = "mabillon:objects/";
        if (storageUri == null || !storageUri.startsWith(prefix)) {
            throw new IllegalArgumentException("Unexpected storage URI: " + storageUri);
        }
        return STORAGE_ROOT.resolve("objects").resolve(storageUri.substring(prefix.length())).normalize();
    }
}
