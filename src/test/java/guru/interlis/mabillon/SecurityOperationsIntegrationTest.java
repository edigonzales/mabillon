package guru.interlis.mabillon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class SecurityOperationsIntegrationTest extends MabillonIntegrationTestSupport {

    @Test
    @WithMockUser(username = "sachbearbeiter", roles = "MABILLON_SACHBEARBEITER")
    void springBootAndJteRenderTheLocalTemplate() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<h1>Mabillon</h1>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AGI-D-2026-000007")));
    }

    @Test
    void cayenneRuntimeStartsWithTheNewBuilderApiAndHasARealDatasource() throws Exception {
        assertThat(cayenneRuntime).isNotNull();
        try (var connection = cayenneRuntime.getDataSource().getConnection()) {
            assertThat(connection.isValid(2)).isTrue();
        }
    }

    @Test
    void unitOfWorkReadUsesShortLivedObjectContext() {
        String number = unitOfWork.read(context -> {
            assertThat(context).isNotNull();
            return "AGI-D-2026-000007";
        });
        assertThat(number).isEqualTo("AGI-D-2026-000007");
        assertThat(localPort).isPositive();
    }

    @Test
    void phaseTenExposesSafeHealthEndpointAndSecurityHeaders() throws Exception {
        mockMvc.perform(get("/").with(httpBasic("sachbearbeiter", "sachbearbeiter")))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy",
                        org.hamcrest.Matchers.containsString("default-src 'self'")));
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("UP")));
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/metrics").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk());
    }

    @Test
    void phaseTenProtectsMutationsWithCsrfAndRendersErrorPage() throws Exception {
        mockMvc.perform(post("/archivierung/ablieferungen")
                        .with(httpBasic("admin", "admin"))
                        .param("organisationCode", "AGI")
                        .param("title", "CSRF-Test")
                        .param("archivempfaenger", "Staatsarchiv"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/does-not-exist").with(httpBasic("sachbearbeiter", "sachbearbeiter")))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("HTTP 404")));
    }
}
