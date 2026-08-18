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
import guru.interlis.mabillon.dossier.DossierService;
import guru.interlis.mabillon.dossier.DossierView;
import guru.interlis.mabillon.dossier.OpenDossierCommand;
import guru.interlis.mabillon.geschaeft.GeschaeftService;
import guru.interlis.mabillon.geschaeft.GeschaeftView;
import guru.interlis.mabillon.geschaeft.OpenGeschaeftCommand;
import guru.interlis.mabillon.journal.EreignisObjektTyp;
import guru.interlis.mabillon.journal.EreignisTyp;
import guru.interlis.mabillon.journal.JournalQueryService;
import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.numbering.GeschaeftNumber;
import guru.interlis.mabillon.unterlage.AssignUnterlageCommand;
import guru.interlis.mabillon.unterlage.RegisterUnterlageCommand;
import guru.interlis.mabillon.unterlage.UnterlageQueryService;
import guru.interlis.mabillon.unterlage.UnterlageService;
import guru.interlis.mabillon.unterlage.UnterlageView;
import guru.interlis.mabillon.unterlage.UpdateUnterlageCommand;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class UnterlageLifecycleIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("sogis/postgis:16-3.5").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("mabillon")
            .withUsername("mabillon")
            .withPassword("mabillon");

    @Autowired
    private DossierService dossierService;

    @Autowired
    private GeschaeftService geschaeftService;

    @Autowired
    private UnterlageService unterlageService;

    @Autowired
    private UnterlageQueryService unterlageQueryService;

    @Autowired
    private JournalQueryService journalQueryService;

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("mabillon.cayenne.url", POSTGRES::getJdbcUrl);
        registry.add("mabillon.cayenne.username", POSTGRES::getUsername);
        registry.add("mabillon.cayenne.password", POSTGRES::getPassword);
    }

    @BeforeAll
    static void importFixtures() {
        InterlisTestFixture.importGoldenPath(POSTGRES);
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void lifecyclePersistsValidTransitionsAndRejectsInvalidOnes() {
        GeschaeftView business = openBusiness("Service");
        UnterlageView document = registerDraft(business.dossierNumber(), "Lifecycle-Unterlage");

        assertThat(document.status()).isEqualTo("In_Arbeit");
        assertThat(document.aktenrelevant()).isFalse();

        UnterlageView updated = unterlageService.updateMetadata(new UpdateUnterlageCommand(
                document.tid(), "Lifecycle-Unterlage geändert", "AKTENNOTIZ",
                LocalDate.of(2026, 8, 17), null, null, "TXT", "Metadaten geändert."));
        assertThat(updated.title()).isEqualTo("Lifecycle-Unterlage geändert");

        UnterlageView assigned = unterlageService.assignToGeschaeft(
                new AssignUnterlageCommand(document.tid(), GeschaeftNumber.parse(business.number())));
        assertThat(assigned.geschaeftsnummer()).isEqualTo(business.number());
        assertThat(unterlageService.unassignFromGeschaeft(document.tid()).geschaeftsnummer()).isNull();

        UnterlageView finalized = unterlageService.finalizeUnterlage(document.tid());
        assertThat(finalized.status()).isEqualTo("Final");
        assertThatThrownBy(() -> unterlageService.finalizeUnterlage(document.tid()))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("in Arbeit");

        UnterlageView registered = unterlageService.registerAktenrelevant(document.tid());
        assertThat(registered.status()).isEqualTo("Registriert");
        assertThat(registered.aktenrelevant()).isTrue();
        assertThatThrownBy(() -> unterlageService.registerAktenrelevant(document.tid()))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("finale Unterlagen");

        UnterlageView cancelled = unterlageService.cancel(document.tid(), "Fehlerhafte Unterlage");
        assertThat(cancelled.status()).isEqualTo("Storniert");
        assertThatThrownBy(() -> unterlageService.updateMetadata(new UpdateUnterlageCommand(
                document.tid(), "Nicht mehr erlaubt", "AKTENNOTIZ", null, null, null, null, null)))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("Stornierte Unterlagen");

        assertThat(unterlageQueryService.get(document.tid()).status()).isEqualTo("Storniert");
        assertThat(journalQueryService.findForObject(EreignisObjektTyp.Unterlage, document.tid().toString(), 20))
                .extracting(entry -> entry.typ())
                .contains(EreignisTyp.Unterlage_registriert, EreignisTyp.Geaendert, EreignisTyp.Status_geaendert);
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void detailPageExposesOnlyValidLifecycleActions() throws Exception {
        GeschaeftView business = openBusiness("Web");
        UnterlageView document = registerDraft(business.dossierNumber(), "Lifecycle-Web-Unterlage");
        String path = "/unterlagen/" + document.tid();

        mockMvc.perform(get(path).with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lifecycle-Web-Unterlage")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Unterlage finalisieren")));

        mockMvc.perform(post(path + "/finalisieren").with(httpBasic("admin", "admin")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(path));
        mockMvc.perform(get(path).with(httpBasic("admin", "admin")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Aktenrelevant registrieren")));

        mockMvc.perform(post(path + "/aktenrelevant-registrieren").with(httpBasic("admin", "admin")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(path));

        mockMvc.perform(post(path + "/stornieren").with(httpBasic("admin", "admin")).with(csrf())
                        .param("reason", "Web-Test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(path));
        mockMvc.perform(get(path).with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "storniert und kann nicht mehr geändert werden")));
    }

    private UnterlageView registerDraft(String dossierNumber, String title) {
        return unterlageService.register(new RegisterUnterlageCommand(
                DossierNumber.parse(dossierNumber), null, title, "AKTENNOTIZ",
                LocalDate.of(2026, 8, 17), null, null, false, "TXT", null), null);
    }

    private GeschaeftView openBusiness(String suffix) {
        DossierView dossier = dossierService.open(new OpenDossierCommand(
                "Phase 11.7 Dossier " + suffix,
                "Unterlagen-Lifecycle",
                "4.3.3",
                "AGI-NOM",
                "anna.mueller",
                LocalDate.of(2026, 8, 17)));
        return geschaeftService.open(new OpenGeschaeftCommand(
                DossierNumber.parse(dossier.number()),
                "Phase 11.7 Geschäft " + suffix,
                "Unterlagen-Lifecycle",
                "NOMENKLATURMUTATION",
                "AGI-NOM",
                "anna.mueller",
                LocalDate.of(2026, 8, 17),
                LocalDate.of(2026, 8, 17),
                LocalDate.of(2026, 8, 31),
                2));
    }
}
