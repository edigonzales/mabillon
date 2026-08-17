package guru.interlis.mabillon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import guru.interlis.mabillon.beteiligung.AddBeteiligungCommand;
import guru.interlis.mabillon.beteiligung.BeteiligterService;
import guru.interlis.mabillon.beteiligung.BeteiligterView;
import guru.interlis.mabillon.beteiligung.BeteiligungService;
import guru.interlis.mabillon.beteiligung.BeteiligungView;
import guru.interlis.mabillon.beteiligung.CreateBeteiligterCommand;
import guru.interlis.mabillon.beteiligung.EndBeteiligungCommand;
import guru.interlis.mabillon.beteiligung.UpdateBeteiligungCommand;
import guru.interlis.mabillon.dossier.DossierService;
import guru.interlis.mabillon.dossier.DossierView;
import guru.interlis.mabillon.dossier.OpenDossierCommand;
import guru.interlis.mabillon.geschaeft.GeschaeftService;
import guru.interlis.mabillon.geschaeft.GeschaeftView;
import guru.interlis.mabillon.geschaeft.OpenGeschaeftCommand;
import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.numbering.GeschaeftNumber;
import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Dossier;
import guru.interlis.mabillon.persistence.cayenne.Geschaeft;
import guru.interlis.mabillon.quality.DataQualityService;
import guru.interlis.mabillon.quality.QualitySeverity;
import org.apache.cayenne.query.ObjectSelect;
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
class Phase11BusinessRulesIntegrationTest {

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
    private BeteiligterService beteiligterService;

    @Autowired
    private BeteiligungService beteiligungService;

    @Autowired
    private DataQualityService dataQualityService;

    @Autowired
    private CayenneUnitOfWork unitOfWork;

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
    void phase118BusinessRulesAreEnforced() throws Exception {
        verifyDq007();
        verifyParticipationRules();
        verifyDuplicateWarning();
    }

    private void verifyDq007() {
        GeschaeftView business = openBusiness("DQ007");
        DossierNumber dossierNumber = DossierNumber.parse(business.dossierNumber());

        assertThat(dataQualityService.checkDossier(dossierNumber).findings())
                .noneMatch(finding -> "DQ-007".equals(finding.ruleCode()));

        unitOfWork.write(context -> {
            Dossier dossier = ObjectSelect.query(Dossier.class)
                    .where(Dossier.DOSSIERNUMMER.eq(dossierNumber.value())).selectFirst(context);
            dossier.setAstatus("Geschlossen");
        });

        assertThat(dataQualityService.checkDossier(dossierNumber).findings())
                .anySatisfy(finding -> {
                    assertThat(finding.ruleCode()).isEqualTo("DQ-007");
                    assertThat(finding.severity()).isEqualTo(QualitySeverity.ERROR);
                    assertThat(finding.objectId()).isEqualTo(dossierNumber.value());
                });
        assertThat(dataQualityService.checkGeschaeft(GeschaeftNumber.parse(business.number())).findings())
                .anyMatch(finding -> "DQ-007".equals(finding.ruleCode()));
    }

    private void verifyParticipationRules() {
        GeschaeftView business = openBusiness("Beteiligung");
        BeteiligterView party = beteiligterService.create(new CreateBeteiligterCommand(
                "Person", "Regel", "Rita", null, "rita.regel@example.test", null, null, "P11-8-RITA"));

        assertThatThrownBy(() -> new AddBeteiligungCommand(
                GeschaeftNumber.parse(business.number()), party.tid(), "ANTRAGSTELLERIN", null,
                LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 19), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Gültig-bis");

        BeteiligungView participation = beteiligungService.add(new AddBeteiligungCommand(
                GeschaeftNumber.parse(business.number()), party.tid(), "ANTRAGSTELLERIN", "Gesuchstellerin",
                LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 31), "Phase 11.8"));
        assertThat(participation.gueltigVon()).isEqualTo(LocalDate.of(2026, 8, 17));
        assertThat(participation.gueltigBis()).isEqualTo(LocalDate.of(2026, 8, 31));

        unitOfWork.write(context -> {
            Geschaeft value = ObjectSelect.query(Geschaeft.class)
                    .where(Geschaeft.GESCHAEFTSNUMMER.eq(business.number())).selectFirst(context);
            value.setLifecyclestatus("Abgeschlossen");
        });

        assertThatThrownBy(() -> beteiligungService.update(new UpdateBeteiligungCommand(
                participation.tid(), "Nicht mehr erlaubt", LocalDate.of(2026, 8, 17), null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nicht mehr bearbeitbar");
        assertThatThrownBy(() -> beteiligungService.end(new EndBeteiligungCommand(
                participation.tid(), LocalDate.of(2026, 8, 25))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nicht mehr bearbeitbar");
    }

    private void verifyDuplicateWarning() throws Exception {
        CreateBeteiligterCommand original = new CreateBeteiligterCommand(
                "Person", "Duplikat", "Dora", null, "dora.duplicate@example.test", null, null, "P11-8-DORA");
        BeteiligterView existing = beteiligterService.create(original);

        CreateBeteiligterCommand candidate = new CreateBeteiligterCommand(
                "Person", "Anderer Name", "Andere", null, "dora.duplicate@example.test", null, null, null);
        assertThat(beteiligterService.findPotentialDuplicates(candidate))
                .extracting(BeteiligterView::tid)
                .contains(existing.tid());

        mockMvc.perform(post("/beteiligte")
                        .with(httpBasic("admin", "admin"))
                        .with(csrf())
                        .param("typ", candidate.typ())
                        .param("name", candidate.name())
                        .param("vorname", candidate.vorname())
                        .param("email", candidate.email()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mögliches Duplikat")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Trotzdem erfassen")));

        mockMvc.perform(post("/beteiligte")
                        .with(httpBasic("admin", "admin"))
                        .with(csrf())
                        .param("duplicateConfirmed", "true")
                        .param("typ", candidate.typ())
                        .param("name", candidate.name())
                        .param("vorname", candidate.vorname())
                        .param("email", candidate.email()))
                .andExpect(status().is3xxRedirection());

        assertThat(beteiligterService.findPotentialDuplicates(candidate)).hasSizeGreaterThanOrEqualTo(2);
    }

    private GeschaeftView openBusiness(String suffix) {
        DossierView dossier = dossierService.open(new OpenDossierCommand(
                "Phase 11.8 Dossier " + suffix,
                "Business rules",
                "4.3.3",
                "AGI-NOM",
                "anna.mueller",
                LocalDate.of(2026, 8, 17)));
        return geschaeftService.open(new OpenGeschaeftCommand(
                DossierNumber.parse(dossier.number()),
                "Phase 11.8 Geschäft " + suffix,
                "Business rules",
                "NOMENKLATURMUTATION",
                "AGI-NOM",
                "anna.mueller",
                LocalDate.of(2026, 8, 17),
                LocalDate.of(2026, 8, 17),
                LocalDate.of(2026, 8, 31),
                2));
    }
}
