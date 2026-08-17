package guru.interlis.mabillon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import guru.interlis.mabillon.search.GlobalSearchCriteria;
import guru.interlis.mabillon.search.GlobalSearchHit;
import guru.interlis.mabillon.search.GlobalSearchResult;
import guru.interlis.mabillon.search.GlobalSearchService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
class GlobalSearchCorrectnessIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("sogis/postgis:16-3.5").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("mabillon")
            .withUsername("mabillon")
            .withPassword("mabillon");

    @Autowired
    private GlobalSearchService searchService;

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
    void structuredFiltersUseTheirDeclaredMeaningPerObjectType() {
        GlobalSearchResult dossierNumberAsBusinessNumber = searchService.search(criteria(
                null, "AGI-D-2026-000007", null, null, null, null, null, null, null, null));
        assertThat(dossierNumberAsBusinessNumber.items()).isEmpty();

        GlobalSearchResult realBusinessNumber = searchService.search(criteria(
                null, "AGI-G-2026-000421", null, null, null, null, null, null, null, null));
        assertThat(realBusinessNumber.items())
                .extracting(GlobalSearchHit::objectType)
                .contains("Dossier", "Geschaeft", "Unterlage", "FachsystemReferenz");

        GlobalSearchResult systemCodeIsNotAProcessStatus = searchService.search(criteria(
                null, null, null, null, null, null, null, "NOMENKLATUR", null, null));
        assertThat(systemCodeIsNotAProcessStatus.items())
                .noneMatch(hit -> "FachsystemReferenz".equals(hit.objectType()));

        GlobalSearchResult fachsystemIdIsNotATitle = searchService.search(criteria(
                null, null, null, "FLN-4500-000123", null, null, null, null, null, null));
        assertThat(fachsystemIdIsNotATitle.items())
                .noneMatch(hit -> "FachsystemReferenz".equals(hit.objectType()));

        GlobalSearchResult byFachsystemId = searchService.search(criteria(
                null, null, null, null, null, null, null, null, null, "FLN-4500-000123"));
        assertThat(byFachsystemId.items())
                .anyMatch(hit -> "FachsystemReferenz".equals(hit.objectType())
                        && "FLN-4500-000123".equals(hit.identifier()));
    }

    @Test
    void participantAndNestedDossierFiltersAreExplicitlyRelated() {
        GlobalSearchResult byParticipant = searchService.search(criteria(
                null, null, null, null, "Gemeinde Musterwil", null, null, null, null, null));
        assertThat(byParticipant.items())
                .extracting(GlobalSearchHit::objectType)
                .contains("Dossier", "Geschaeft", "Beteiligter");

        GlobalSearchResult byDossier = searchService.search(criteria(
                null, null, "AGI-D-2026-000007", null, null, null, null, null, null, null));
        assertThat(byDossier.items())
                .extracting(GlobalSearchHit::objectType)
                .contains("Dossier", "Geschaeft", "Unterlage", "FachsystemReferenz");
    }

    @Test
    void blankBrowserFieldsDoNotSuppressFreeTextResults() throws Exception {
        mockMvc.perform(get("/suche")
                        .with(httpBasic("sachbearbeiter", "sachbearbeiter"))
                        .param("q", "Bodenrain")
                        .param("geschaeftsnummer", "")
                        .param("dossiernummer", "")
                        .param("titel", "")
                        .param("beteiligterName", "")
                        .param("organisation", "")
                        .param("geschaeftsartCode", "")
                        .param("processStatusCode", "")
                        .param("unterlagentitel", "")
                        .param("fachsystemId", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AGI-D-2026-000007")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AGI-G-2026-000421")));
    }

    @Test
    void everyReturnedTargetUrlResolvesToAnExistingPage() throws Exception {
        GlobalSearchResult result = searchService.search(new GlobalSearchCriteria("Musterwil", 0, 100));
        assertThat(result.items()).isNotEmpty();

        for (String targetUrl : result.items().stream().map(GlobalSearchHit::targetUrl).distinct().toList()) {
            mockMvc.perform(get(targetUrl).with(httpBasic("sachbearbeiter", "sachbearbeiter")))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void criteriaNormalizeWhitespaceAndEmptyValues() {
        GlobalSearchCriteria criteria = new GlobalSearchCriteria(
                "  Bodenrain  ", " ", "", null, "  Gemeinde Musterwil ", null,
                null, null, null, null, 0, 20);

        assertThat(criteria.text()).isEqualTo("Bodenrain");
        assertThat(criteria.geschaeftsnummer()).isNull();
        assertThat(criteria.dossiernummer()).isNull();
        assertThat(criteria.beteiligterName()).isEqualTo("Gemeinde Musterwil");
    }

    private static GlobalSearchCriteria criteria(
            String text,
            String businessNumber,
            String dossierNumber,
            String title,
            String participant,
            String organisation,
            String businessType,
            String processStatus,
            String documentTitle,
            String referenceId) {
        return new GlobalSearchCriteria(
                text, businessNumber, dossierNumber, title, participant, organisation,
                businessType, processStatus, documentTitle, referenceId, 0, 100);
    }
}
