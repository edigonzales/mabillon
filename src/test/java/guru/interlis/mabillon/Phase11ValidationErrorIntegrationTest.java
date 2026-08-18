package guru.interlis.mabillon;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import guru.interlis.mabillon.domain.ValidationException;
import guru.interlis.mabillon.dossier.OpenDossierCommand;
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
class Phase11ValidationErrorIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("sogis/postgis:16-3.5").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("mabillon")
            .withUsername("mabillon")
            .withPassword("mabillon");

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
    void commandValidationKeepsFieldCodeAndMessage() {
        assertThatThrownBy(() -> new OpenDossierCommand("", null, "", "", "", null))
                .isInstanceOfSatisfying(ValidationException.class, failure ->
                        org.assertj.core.api.Assertions.assertThat(failure.errors())
                                .extracting(error -> error.field() + ":" + error.code())
                                .containsExactly("title:required", "position:required", "federation:required", "responsible:required"));
    }

    @Test
    void invalidFormRendersStructuredBadRequest() throws Exception {
        mockMvc.perform(post("/dossiers")
                        .with(httpBasic("sachbearbeiter", "sachbearbeiter"))
                        .with(csrf())
                        .param("title", "")
                        .param("position", "4.3.2")
                        .param("federation", "AGI")
                        .param("responsible", "anna.mueller"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Eingaben prüfen")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("title")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("required")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Titel ist erforderlich.")));
    }

    @Test
    void htmxValidationRendersOnlyAlertFragment() throws Exception {
        mockMvc.perform(post("/dossiers")
                        .with(httpBasic("sachbearbeiter", "sachbearbeiter"))
                        .with(csrf())
                        .header("HX-Request", "true")
                        .param("title", "")
                        .param("position", "4.3.2")
                        .param("federation", "AGI")
                        .param("responsible", "anna.mueller"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("role=\"alert\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("app-header"))));
    }

    @Test
    void conversionAndMissingParametersAreFieldErrors() throws Exception {
        mockMvc.perform(get("/dossiers")
                        .with(httpBasic("sachbearbeiter", "sachbearbeiter"))
                        .param("openedFrom", "kein-datum"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("openedFrom")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("type")));

        mockMvc.perform(post("/dossiers")
                        .with(httpBasic("sachbearbeiter", "sachbearbeiter"))
                        .with(csrf())
                        .param("title", "Test"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("position")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("required")));
    }

    @Test
    void missingObjectAndDomainConflictUseDifferentStatuses() throws Exception {
        mockMvc.perform(get("/dossiers/AGI-D-2099-999999")
                        .with(httpBasic("sachbearbeiter", "sachbearbeiter")))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Unbekanntes Dossier")));

        mockMvc.perform(post("/dossiers/AGI-D-2026-000007/abschluss")
                        .with(httpBasic("admin", "admin"))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Dossier ist nicht offen.")));
    }
}
