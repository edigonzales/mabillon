package guru.interlis.mabillon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import guru.interlis.mabillon.domain.DomainRuleViolationException;
import guru.interlis.mabillon.dossier.DossierView;
import guru.interlis.mabillon.dossier.OpenDossierCommand;
import guru.interlis.mabillon.geschaeft.ChangeProcessStatusCommand;
import guru.interlis.mabillon.geschaeft.GeschaeftView;
import guru.interlis.mabillon.geschaeft.OpenGeschaeftCommand;
import guru.interlis.mabillon.geschaeft.SetResultCommand;
import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.numbering.GeschaeftNumber;
import guru.interlis.mabillon.quality.QualityReport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class DossierIntegrationTest extends MabillonIntegrationTestSupport {

    @Test
    void queryNomenklaturDossier() {
        DossierView dossier = dossierQueryService.findByNumber("AGI-D-2026-000007").orElseThrow();
        assertThat(dossier.title()).contains("Bodenrain");
        assertThat(dossier.geschaefte()).singleElement()
                .extracting(DossierView.GeschaeftSummary::number)
                .isEqualTo("AGI-G-2026-000421");
        assertThat(dossier.unterlagen()).hasSize(9);
    }

    @Test
    @WithMockUser(username = "sachbearbeiter", roles = "MABILLON_SACHBEARBEITER")
    void phaseThreeSearchListsAndCreationFormsRender() throws Exception {
        mockMvc.perform(get("/dossiers"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Dossiers")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AGI-D-2026-000007")));
        mockMvc.perform(get("/geschaefte").param("title", "Musterwil"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AGI-G-2026-000421")));
        mockMvc.perform(get("/dossiers/neu"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Neues Dossier")));
        mockMvc.perform(get("/geschaefte/neu"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Neues Geschäft")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void phaseSevenGoldenPathClosesBusinessAndDossierWithQualityAndAudit() throws Exception {
        DossierView dossier = dossierService.open(new OpenDossierCommand(
                "Phase 7 Golden-Path Dossier", "Datenqualitäts- und Abschlussprüfung.",
                "4.3.3", "AGI-NOM", "anna.mueller", LocalDate.of(2026, 8, 16)));
        GeschaeftView business = geschaeftService.open(new OpenGeschaeftCommand(
                DossierNumber.parse(dossier.number()), "Phase 7 Golden-Path Geschäft", null,
                "NOMENKLATURMUTATION", "AGI-NOM", "anna.mueller", null,
                LocalDate.of(2026, 8, 16), null, 1));

        assertThatThrownBy(() -> dossierService.close(DossierNumber.parse(dossier.number())))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("nicht abgeschlossen");

        geschaeftService.changeProcessStatus(new ChangeProcessStatusCommand(
                GeschaeftNumber.parse(business.number()), "ABGESCHLOSSEN", "Abschluss fachlich bereit."));
        geschaeftService.setResult(new SetResultCommand(
                GeschaeftNumber.parse(business.number()), "GENEHMIGT", "Golden-Path-Entscheid."));

        QualityReport businessQuality = dataQualityService.checkGeschaeft(GeschaeftNumber.parse(business.number()));
        mockMvc.perform(post("/geschaefte/{number}/abschluss", business.number())
                        .with(httpBasic("admin", "admin"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/geschaefte/" + business.number()));

        assertThat(businessQuality.hasErrors()).isFalse();
        assertThat(geschaeftQueryService.findByNumber(business.number()).orElseThrow().lifecycleStatus())
                .isEqualTo("Abgeschlossen");
        assertThat(journalQueryService.findForGeschaeft(GeschaeftNumber.parse(business.number()), 20))
                .extracting(entry -> entry.typ().name())
                .contains("Geschaeft_abgeschlossen");

        mockMvc.perform(get("/datenqualitaet/geschaefte/{number}", business.number())
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Keine Datenqualitätsbefunde")));
        mockMvc.perform(get("/geschaefte/{number}", business.number())
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Geschaeft_abgeschlossen")));

        mockMvc.perform(post("/dossiers/{number}/abschluss", dossier.number())
                        .with(httpBasic("admin", "admin"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dossiers/" + dossier.number()));
        assertThat(dossierQueryService.findByNumber(dossier.number()).orElseThrow().status())
                .isEqualTo("Geschlossen");
        assertThat(journalQueryService.findForDossier(DossierNumber.parse(dossier.number()), 20))
                .extracting(entry -> entry.typ().name())
                .contains("Dossier_abgeschlossen");
    }
}
