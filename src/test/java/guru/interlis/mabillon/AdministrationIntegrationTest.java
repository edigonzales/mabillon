package guru.interlis.mabillon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import guru.interlis.mabillon.catalog.CatalogType;
import guru.interlis.mabillon.registraturplan.RegistraturplanQueryService;
import guru.interlis.mabillon.stammdaten.BenutzerService;
import guru.interlis.mabillon.stammdaten.OrganisationseinheitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AdministrationIntegrationTest extends MabillonIntegrationTestSupport {

    @Autowired
    private OrganisationseinheitService organisationseinheitService;

    @Autowired
    private BenutzerService benutzerService;

    @Autowired
    private RegistraturplanQueryService registraturplanQueryService;

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void configuresBusinessTypeAndProcessStatusWithAllSpecializedFields() throws Exception {
        mockMvc.perform(post("/admin/kataloge/geschaeftsart")
                        .with(csrf())
                        .param("code", "P11_FINAL_TYPE")
                        .param("name", "Phase 11 Geschäftsart")
                        .param("description", "Final verification")
                        .param("resultatErforderlich", "true"))
                .andExpect(status().is3xxRedirection());

        assertThat(catalogService.get(CatalogType.GESCHAEFTSART, "P11_FINAL_TYPE").resultatErforderlich()).isTrue();

        mockMvc.perform(post("/admin/kataloge/geschaeftsart/P11_FINAL_TYPE")
                        .with(csrf())
                        .param("name", "Phase 11 Geschäftsart aktualisiert")
                        .param("description", "Aktualisierte Konfiguration")
                        .param("resultatErforderlich", "false"))
                .andExpect(status().is3xxRedirection());

        var type = catalogService.get(CatalogType.GESCHAEFTSART, "P11_FINAL_TYPE");
        assertThat(type.name()).isEqualTo("Phase 11 Geschäftsart aktualisiert");
        assertThat(type.resultatErforderlich()).isFalse();

        mockMvc.perform(post("/admin/kataloge/prozessstatus")
                        .with(csrf())
                        .param("code", "P11_FINAL_INITIAL")
                        .param("name", "Initial")
                        .param("geschaeftsartCode", "P11_FINAL_TYPE")
                        .param("sortierung", "10")
                        .param("initial", "true"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/admin/kataloge/prozessstatus/P11_FINAL_INITIAL")
                        .with(csrf())
                        .param("name", "Initial aktualisiert")
                        .param("geschaeftsartCode", "P11_FINAL_TYPE")
                        .param("sortierung", "20")
                        .param("initial", "true")
                        .param("terminal", "true"))
                .andExpect(status().is3xxRedirection());

        var processStatus = catalogService.get(CatalogType.PROZESSSTATUS, "P11_FINAL_INITIAL");
        assertThat(processStatus.geschaeftsartCode()).isEqualTo("P11_FINAL_TYPE");
        assertThat(processStatus.sortierung()).isEqualTo(20);
        assertThat(processStatus.initial()).isTrue();
        assertThat(processStatus.terminal()).isTrue();

        assertThatThrownBy(() -> catalogService.deactivate(CatalogType.PROZESSSTATUS, "P11_FINAL_INITIAL"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Initialstatus");

        mockMvc.perform(post("/admin/kataloge/geschaeftsart/P11_FINAL_TYPE/deactivate").with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(catalogService.get(CatalogType.GESCHAEFTSART, "P11_FINAL_TYPE").active()).isFalse();
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void maintainsOrganisationHierarchyAndDomainUsers() throws Exception {
        mockMvc.perform(post("/admin/stammdaten/organisationseinheiten")
                        .with(csrf())
                        .param("kuerzel", "P11-ROOT")
                        .param("name", "Phase 11 Root"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/admin/stammdaten/organisationseinheiten")
                        .with(csrf())
                        .param("kuerzel", "P11-CHILD")
                        .param("name", "Phase 11 Child")
                        .param("uebergeordneteEinheit", "P11-ROOT"))
                .andExpect(status().is3xxRedirection());

        assertThat(organisationseinheitService.list(true))
                .filteredOn(value -> value.kuerzel().equals("P11-CHILD"))
                .singleElement()
                .satisfies(value -> assertThat(value.uebergeordneteEinheit()).isEqualTo("P11-ROOT"));

        mockMvc.perform(post("/admin/stammdaten/organisationseinheiten/P11-CHILD")
                        .with(csrf())
                        .param("name", "Phase 11 Child aktualisiert")
                        .param("beschreibung", "UC-030")
                        .param("uebergeordneteEinheit", "P11-ROOT"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/admin/stammdaten/benutzer")
                        .with(csrf())
                        .param("username", "p11.user")
                        .param("name", "Phase 11 User")
                        .param("email", "p11.user@example.test")
                        .param("organisationseinheit", "P11-CHILD"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/admin/stammdaten/benutzer/p11.user")
                        .with(csrf())
                        .param("name", "Phase 11 User aktualisiert")
                        .param("email", "updated@example.test")
                        .param("organisationseinheit", "P11-ROOT"))
                .andExpect(status().is3xxRedirection());

        assertThat(benutzerService.list(true))
                .filteredOn(value -> value.username().equals("p11.user"))
                .singleElement()
                .satisfies(value -> {
                    assertThat(value.name()).isEqualTo("Phase 11 User aktualisiert");
                    assertThat(value.email()).isEqualTo("updated@example.test");
                    assertThat(value.organisationseinheit()).isEqualTo("P11-ROOT");
                });

        mockMvc.perform(post("/admin/stammdaten/benutzer/p11.user/deactivate").with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(benutzerService.list(true))
                .filteredOn(value -> value.username().equals("p11.user"))
                .singleElement()
                .satisfies(value -> assertThat(value.active()).isFalse());
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void maintainsRegistraturplanAndPositionTree() throws Exception {
        String organisation = organisationseinheitService.list(false).getFirst().kuerzel();

        mockMvc.perform(post("/admin/registraturplan/plaene")
                        .with(csrf())
                        .param("code", "P11-FINAL")
                        .param("name", "Phase 11 Finalplan")
                        .param("gueltigVon", "2026-08-18")
                        .param("organisationseinheit", organisation))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/registraturplan?plan=P11-FINAL"));

        mockMvc.perform(post("/admin/registraturplan/positionen")
                        .with(csrf())
                        .param("planCode", "P11-FINAL")
                        .param("code", "P11-1")
                        .param("titel", "Root")
                        .param("federfuehrendeEinheit", organisation))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/admin/registraturplan/positionen")
                        .with(csrf())
                        .param("planCode", "P11-FINAL")
                        .param("code", "P11-1.1")
                        .param("titel", "Child")
                        .param("oberposition", "P11-1")
                        .param("federfuehrendeEinheit", organisation))
                .andExpect(status().is3xxRedirection());

        assertThat(registraturplanQueryService.getPosition("P11-1.1").parentCode()).isEqualTo("P11-1");

        mockMvc.perform(post("/admin/registraturplan/positionen/P11-1.1")
                        .with(csrf())
                        .param("titel", "Child aktualisiert")
                        .param("beschreibung", "UC-033")
                        .param("status", "aktiv"))
                .andExpect(status().is3xxRedirection());
        assertThat(registraturplanQueryService.getPosition("P11-1.1").titel()).isEqualTo("Child aktualisiert");

        mockMvc.perform(post("/admin/registraturplan/positionen/P11-1.1/verschieben").with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(registraturplanQueryService.getPosition("P11-1.1").parentCode()).isNull();

        mockMvc.perform(post("/admin/registraturplan/plaene/P11-FINAL/replace")
                        .with(csrf())
                        .param("gueltigBis", "2026-12-31"))
                .andExpect(status().is3xxRedirection());

        assertThat(registraturplanQueryService.listPlans(true))
                .filteredOn(value -> value.code().equals("P11-FINAL"))
                .singleElement()
                .satisfies(value -> {
                    assertThat(value.gueltigBis()).isEqualTo(LocalDate.of(2026, 12, 31));
                    assertThat(value.status()).isEqualTo("Ersetzt");
                });
    }
}
