package guru.interlis.mabillon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import guru.interlis.mabillon.catalog.CatalogType;
import guru.interlis.mabillon.registraturplan.UpdatePositionCommand;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class CatalogIntegrationTest extends MabillonIntegrationTestSupport {

    @Test
    void sachbearbeiterCannotChangeAdminCatalogs() throws Exception {
        mockMvc.perform(post("/admin/kataloge/geschaeftsart")
                        .with(httpBasic("sachbearbeiter", "sachbearbeiter"))
                        .with(csrf())
                        .param("code", "NICHT_ERLAUBT")
                        .param("name", "Nicht erlaubt"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPagesRenderWithAdminIdentity() throws Exception {
        mockMvc.perform(get("/admin").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Administration")));
        mockMvc.perform(get("/admin/kataloge").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Geschäftsarten")));
        mockMvc.perform(get("/admin/stammdaten").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Anna Müller")));
        mockMvc.perform(get("/admin/registraturplan").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("4.3.2")));
    }

    @Test
    void adminCanCreateAndDeactivateCatalogValueWithoutDeletingItsHistory() throws Exception {
        mockMvc.perform(post("/admin/kataloge/geschaeftsart")
                        .with(httpBasic("admin", "admin"))
                        .with(csrf())
                        .param("code", "PHASE2_TEST")
                        .param("name", "Phase 2 Testwert"))
                .andExpect(status().is3xxRedirection());

        assertThat(catalogService.get(CatalogType.GESCHAEFTSART, "PHASE2_TEST").active()).isTrue();

        mockMvc.perform(post("/admin/kataloge/geschaeftsart/PHASE2_TEST/deactivate")
                        .with(httpBasic("admin", "admin"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(catalogService.get(CatalogType.GESCHAEFTSART, "PHASE2_TEST").active()).isFalse();
        assertThat(catalogService.list(CatalogType.GESCHAEFTSART, true))
                .anyMatch(value -> value.code().equals("PHASE2_TEST"));
    }

    @Test
    void eachSeededBusinessTypeHasExactlyOneInitialProcessStatus() {
        assertThat(catalogService.initialProcessStatus("NOMENKLATURMUTATION").initial()).isTrue();
        assertThat(catalogService.processStatusesForGeschaeftsart("NOMENKLATURMUTATION")).hasSize(9);
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void usedCatalogValueCanBeDeactivatedAndRemainsReadable() {
        catalogService.deactivate(CatalogType.GESCHAEFTSART, "NOMENKLATURMUTATION");
        try {
            assertThat(catalogService.get(CatalogType.GESCHAEFTSART, "NOMENKLATURMUTATION").active()).isFalse();
            assertThat(geschaeftQueryService.findByNumber("AGI-G-2026-000421")).isPresent();
        } finally {
            catalogService.activate(CatalogType.GESCHAEFTSART, "NOMENKLATURMUTATION");
        }
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void registraturplanCycleIsRejected() {
        assertThatThrownBy(() -> registraturplanAdminService.movePosition("4.3", "4.3.2"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Zyklus");
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void inactivePositionIsNotSelectableForNewDossiersButHistoricalDossierRemainsReadable() {
        registraturplanAdminService.deactivatePosition("4.3.2");
        try {
            assertThat(registraturplanQueryService.activeLeafPositions())
                    .noneMatch(position -> position.code().equals("4.3.2"));
            assertThat(dossierQueryService.findByNumber("AGI-D-2026-000007")).isPresent();
        } finally {
            registraturplanAdminService.updatePosition(new UpdatePositionCommand(
                    "4.3.2", "Einzelgeschäfte Flur- und Ortsnamen", null, "aktiv"));
        }
    }
}
