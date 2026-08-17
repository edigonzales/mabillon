package guru.interlis.mabillon.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(SecurityConfigurationTest.ProbeController.class)
@Import(SecurityConfiguration.class)
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
            mockMvc.perform(get(path)).andExpect(status().isNotFound());
        }
    }

    @Test
    void fachlicheRoutesRejectAnonymousUsers() throws Exception {
        for (String path : new String[] {
                "/",
                "/dossiers",
                "/geschaefte",
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
                "/suche",
                "/geschaeftskontrolle",
                "/datenqualitaet",
                "/unterlagen/123/download",
                "/archivierung"
        }) {
            mockMvc.perform(get(path).with(httpBasic("sachbearbeiter", "sachbearbeiter")))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void adminAreaRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/admin").with(httpBasic("sachbearbeiter", "sachbearbeiter")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin").with(httpBasic("admin", "admin")))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonHealthActuatorEndpointsRequireAdminRole() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/metrics").with(httpBasic("sachbearbeiter", "sachbearbeiter")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/metrics").with(httpBasic("admin", "admin")))
                .andExpect(status().isNotFound());
    }

    @RestController
    static class ProbeController {

        @GetMapping({
                "/",
                "/dossiers",
                "/geschaefte",
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
