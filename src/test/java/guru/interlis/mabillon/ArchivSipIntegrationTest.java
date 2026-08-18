package guru.interlis.mabillon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import guru.interlis.mabillon.archivierung.ArchivAblieferungView;
import guru.interlis.mabillon.archivierung.CreateArchivAblieferungCommand;
import guru.interlis.mabillon.archivierung.SippaketView;
import guru.interlis.mabillon.domain.DomainRuleViolationException;
import guru.interlis.mabillon.dossier.DossierView;
import guru.interlis.mabillon.dossier.OpenDossierCommand;
import guru.interlis.mabillon.geschaeft.ChangeProcessStatusCommand;
import guru.interlis.mabillon.geschaeft.GeschaeftView;
import guru.interlis.mabillon.geschaeft.OpenGeschaeftCommand;
import guru.interlis.mabillon.geschaeft.SetResultCommand;
import guru.interlis.mabillon.numbering.ArchivAblieferungNumber;
import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.numbering.GeschaeftNumber;
import guru.interlis.mabillon.storage.DocumentUpload;
import guru.interlis.mabillon.unterlage.RegisterUnterlageCommand;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ArchivSipIntegrationTest extends MabillonIntegrationTestSupport {

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void phaseNineGeneratesValidatesCorrectsAndAcceptsStructuredSip() throws Exception {
        DossierView dossier = dossierService.open(new OpenDossierCommand(
                "Phase 9 Archivdossier", "Archivierungs-Golden-Path.", "4.3.3", "AGI-NOM", "anna.mueller",
                LocalDate.of(2026, 8, 16)));
        GeschaeftView business = geschaeftService.open(new OpenGeschaeftCommand(
                DossierNumber.parse(dossier.number()), "Phase 9 Archivgeschäft", null, "NOMENKLATURMUTATION",
                "AGI-NOM", "anna.mueller", null, LocalDate.of(2026, 8, 16), null, 1));
        unterlageService.register(new RegisterUnterlageCommand(
                DossierNumber.parse(dossier.number()), GeschaeftNumber.parse(business.number()),
                "Archivfähige Unterlage", "AKTENNOTIZ", LocalDate.of(2026, 8, 16), LocalDate.of(2026, 8, 16),
                null, true, "TXT", null),
                new DocumentUpload("archiv-faehig.txt", "text/plain",
                        new ByteArrayInputStream("Archivinhalt Phase 9".getBytes(StandardCharsets.UTF_8))));
        geschaeftService.changeProcessStatus(new ChangeProcessStatusCommand(
                GeschaeftNumber.parse(business.number()), "ABGESCHLOSSEN", "Archivierung vorbereitet."));
        geschaeftService.setResult(new SetResultCommand(
                GeschaeftNumber.parse(business.number()), "GENEHMIGT", "Archivierungsentscheid."));
        geschaeftService.close(GeschaeftNumber.parse(business.number()));
        dossierService.close(DossierNumber.parse(dossier.number()));

        assertThat(aussonderungQueryService.eligible(0, 100).items())
                .anyMatch(item -> item.dossierNumber().equals(dossier.number()));
        ArchivAblieferungView delivery = archivAblieferungService.create(new CreateArchivAblieferungCommand(
                "AGI", "Phase 9 SIP", "Staatsarchiv", "Golden-Path-Prüfung."));
        delivery = archivAblieferungService.addDossier(
                ArchivAblieferungNumber.parse(delivery.deliveryNumber()), DossierNumber.parse(dossier.number()));
        delivery = archivAblieferungService.markReady(ArchivAblieferungNumber.parse(delivery.deliveryNumber()));
        assertThat(delivery.status()).isEqualTo("Bereit");
        assertThat(dataQualityService.checkArchiveDelivery(
                ArchivAblieferungNumber.parse(delivery.deliveryNumber())).hasErrors()).isFalse();

        SippaketView first = sipService.generate(ArchivAblieferungNumber.parse(delivery.deliveryNumber()));
        Path firstPackage = Path.of(java.net.URI.create(first.storageUri()));
        assertThat(firstPackage.resolve("header/metadata.xml")).isRegularFile();
        assertThat(firstPackage.resolve("header/xsd/arelda.xsd")).isRegularFile();
        assertThat(firstPackage.resolve("content/dossier_" + dossier.number() + "/p000001.txt")).isRegularFile();

        Files.writeString(firstPackage.resolve("header/metadata.xml"), "<unlesbar/>", StandardCharsets.UTF_8);
        SippaketView invalid = sipService.validate(ArchivAblieferungNumber.parse(delivery.deliveryNumber()), 1);
        assertThat(invalid.validationStatus()).isEqualTo("Ungueltig");
        assertThat(archivAblieferungService.get(ArchivAblieferungNumber.parse(delivery.deliveryNumber())).status())
                .isEqualTo("Korrektur_erforderlich");

        SippaketView second = sipService.generate(ArchivAblieferungNumber.parse(delivery.deliveryNumber()));
        assertThat(second.attempt()).isEqualTo(2);
        SippaketView valid = sipService.validateLatest(ArchivAblieferungNumber.parse(delivery.deliveryNumber()));
        assertThat(valid.validationStatus()).isEqualTo("Gueltig");

        archivAblieferungService.recordTransferred(
                ArchivAblieferungNumber.parse(delivery.deliveryNumber()), "Übernahme vorbereitet.");
        ArchivAblieferungView accepted = archivAblieferungService.recordAccepted(
                ArchivAblieferungNumber.parse(delivery.deliveryNumber()), "BAR-2026-0009", "Übernahme bestätigt.");
        assertThat(accepted.status()).isEqualTo("Uebernommen");
        assertThat(dossierQueryService.findByNumber(dossier.number()).orElseThrow().status()).isEqualTo("Archiviert");
    }

    @Test
    void phaseNineArchivePageRendersForAdministrators() throws Exception {
        mockMvc.perform(get("/archivierung").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Archivierung")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Neue Archivablieferung")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void phaseNineRejectsOpenDossierFromArchiveDelivery() {
        DossierView open = dossierService.open(new OpenDossierCommand(
                "Noch offenes Archivdossier", null, "4.3.3", "AGI-NOM", "anna.mueller",
                LocalDate.of(2026, 8, 16)));
        ArchivAblieferungView delivery = archivAblieferungService.create(new CreateArchivAblieferungCommand(
                "AGI", "Ungültige Archivablieferung", "Staatsarchiv", null));
        ArchivAblieferungNumber number = ArchivAblieferungNumber.parse(delivery.deliveryNumber());
        assertThatThrownBy(() -> archivAblieferungService.addDossier(number, DossierNumber.parse(open.number())))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("geschlossene");
        assertThat(archivAblieferungService.get(number).dossiers()).isEmpty();
    }
}
