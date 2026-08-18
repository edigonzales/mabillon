package guru.interlis.mabillon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import guru.interlis.mabillon.interlis.ExportSelection;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class InterlisIntegrationTest extends MabillonIntegrationTestSupport {

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void phaseEightExportsValidatedCatalogWithStableTidAndBasket() throws IOException {
        Path exportDirectory = Files.createTempDirectory("mabillon-phase8-export-");
        Path target = exportDirectory.resolve("kataloge.xtf");

        Path exported = interlisExchangeService.exportCatalog(ExportSelection.all(target));
        String exportedContent = Files.readString(exported, StandardCharsets.UTF_8);

        assertThat(exported).isEqualTo(target);
        assertThat(exportedContent).contains("ili:bid=\"c4dbb2a2-9b06-525d-b2d9-e69b8d9e7013\"");
        assertThat(exportedContent).contains("ili:tid=\"d5410f91-14ed-50c7-9596-f8c227db72c1\"");
        assertThat(exportedContent).contains("<SO_AGI_GEVER_20260707:Kataloge");
        assertThat(exportedContent.split("ili:tid=", -1).length).isGreaterThan(40);
    }

    @Test
    void phaseEightExchangePageRendersForAdministrators() throws Exception {
        mockMvc.perform(get("/admin/interlis").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("INTERLIS-Datenaustausch")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Kataloge")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Importieren")));
    }
}
