package guru.interlis.mabillon.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(ProductionSecurityConfigurationTest.ProbeController.class)
@Import(SecurityConfiguration.class)
@ActiveProfiles("prod")
class ProductionSecurityConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    void productionDoesNotAcceptDevelopmentCredentials() throws Exception {
        mockMvc.perform(get("/dossiers").with(httpBasic("admin", "admin")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/dossiers").with(httpBasic("sachbearbeiter", "sachbearbeiter")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void productionHasNoLocalUserDirectory() {
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("admin"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("disabled outside dev/test");
    }

    @RestController
    static class ProbeController {

        @GetMapping("/dossiers")
        String dossiers() {
            return "ok";
        }
    }
}
