package guru.interlis.mabillon.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(SecurityConfigurationTest.ProbeController.class)
@Import(SecurityConfiguration.class)
@ActiveProfiles("test")
class SecurityConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthAndStaticAssetsRemainPublic() throws Exception {
        for (String path : new String[] {
                "/actuator/health",
                "/mabillon.css",
                "/mabillon.js",
                "/htmx-2.0.10.min.js",
                "/favicon.ico"
        }) {
            assertAllowed(path, null);
        }
    }

    @Test
    void fachlicheRoutesRejectAnonymousUsers() throws Exception {
        for (String path : new String[] {
                "/",
                "/dossiers",
                "/geschaefte",
                "/aufgaben",
                "/beteiligte",
                "/suche",
                "/geschaeftskontrolle",
                "/datenqualitaet",
                "/unterlagen/123/download",
                "/archivierung"
        }) {
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void fachlicheRoutesRejectAuthenticatedUsersWithoutMabillonRole() throws Exception {
        mockMvc.perform(get("/dossiers").with(user("external-user")))
                .andExpect(status().isForbidden());
    }

    @Test
    void sachbearbeiterCanReadFachlicheRoutes() throws Exception {
        for (String path : new String[] {
                "/",
                "/dossiers",
                "/geschaefte",
                "/aufgaben",
                "/beteiligte",
                "/suche",
                "/geschaeftskontrolle",
                "/datenqualitaet",
                "/unterlagen/123/download",
                "/archivierung"
        }) {
            assertAllowed(path, httpBasic("sachbearbeiter", "sachbearbeiter"));
        }
    }

    @Test
    void adminAreaRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/admin").with(httpBasic("sachbearbeiter", "sachbearbeiter")))
                .andExpect(status().isForbidden());
        assertAllowed("/admin", httpBasic("admin", "admin"));
    }

    @Test
    void nonHealthActuatorEndpointsRequireAdminRole() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/metrics").with(httpBasic("sachbearbeiter", "sachbearbeiter")))
                .andExpect(status().isForbidden());
        assertAllowed("/actuator/metrics", httpBasic("admin", "admin"));
    }

    private void assertAllowed(String path, RequestPostProcessor authentication) throws Exception {
        var request = get(path);
        if (authentication != null) {
            request.with(authentication);
        }
        int responseStatus = mockMvc.perform(request).andReturn().getResponse().getStatus();
        assertThat(responseStatus).isNotIn(401, 403);
    }

    @RestController
    static class ProbeController {

        @GetMapping({
                "/",
                "/dossiers",
                "/geschaefte",
                "/aufgaben",
                "/beteiligte",
                "/suche",
                "/geschaeftskontrolle",
                "/datenqualitaet",
                "/unterlagen/123/download",
                "/archivierung",
                "/admin",
                "/actuator/health",
                "/actuator/metrics",
                "/mabillon.css",
                "/mabillon.js",
                "/htmx-2.0.10.min.js",
                "/favicon.ico"
        })
        String ok() {
            return "ok";
        }
    }
}
