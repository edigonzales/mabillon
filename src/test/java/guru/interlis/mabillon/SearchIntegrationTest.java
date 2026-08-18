package guru.interlis.mabillon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import guru.interlis.mabillon.fachsystem.AddFachsystemReferenzCommand;
import guru.interlis.mabillon.fachsystem.AddFachsystemReferenzToDossierCommand;
import guru.interlis.mabillon.fachsystem.FachsystemReferenzView;
import guru.interlis.mabillon.geschaeft.GeschaeftView;
import guru.interlis.mabillon.journal.EreignisObjektTyp;
import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.numbering.GeschaeftNumber;
import guru.interlis.mabillon.search.GlobalSearchCriteria;
import guru.interlis.mabillon.search.GlobalSearchResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class SearchIntegrationTest extends MabillonIntegrationTestSupport {

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void phaseSixManagesFachsystemReferencesForDossierAndGeschaeft() {
        GeschaeftView business = newBusiness("Fachsystem");
        FachsystemReferenzView businessReference = fachsystemReferenzService.addToGeschaeft(
                new AddFachsystemReferenzCommand(GeschaeftNumber.parse(business.number()), "NOMENKLATUR",
                        "Flurname", "FLN-PHASE6-000001", "MUT-PHASE6-000001",
                        "https://nomenklatur.example/FLN-PHASE6-000001", "Phase-6-Referenz"));
        FachsystemReferenzView dossierReference = fachsystemReferenzService.addToDossier(
                new AddFachsystemReferenzToDossierCommand(DossierNumber.parse(business.dossierNumber()),
                        "AV", "Grundstück", "GS-PHASE6-000001", null, null, "Dossierweite Referenz"));

        assertThat(fachsystemReferenzService.forGeschaeft(GeschaeftNumber.parse(business.number())))
                .extracting(FachsystemReferenzView::objektId).containsExactly("FLN-PHASE6-000001");
        assertThat(fachsystemReferenzService.forDossier(DossierNumber.parse(business.dossierNumber())))
                .extracting(FachsystemReferenzView::tid)
                .contains(businessReference.tid(), dossierReference.tid());

        fachsystemReferenzService.remove(businessReference.tid(), "Referenz korrigiert.");
        assertThat(fachsystemReferenzService.forGeschaeft(GeschaeftNumber.parse(business.number()))).isEmpty();
        assertThat(journalQueryService.findForObject(EreignisObjektTyp.FachsystemReferenz,
                businessReference.tid().toString(), 10)).isNotEmpty();
    }

    @Test
    void phaseSixGlobalSearchFindsGoldenPathAndFachsystemIdentifiersWithPagination() {
        GlobalSearchResult byName = globalSearchService.search(new GlobalSearchCriteria("Bodenrain", 0, 50));
        assertThat(byName.items()).anyMatch(item -> item.objectType().equals("Dossier"));
        assertThat(byName.items()).anyMatch(item -> item.objectType().equals("Geschaeft"));
        assertThat(byName.items()).anyMatch(item -> item.objectType().equals("Unterlage"));

        GlobalSearchResult byReference = globalSearchService.search(new GlobalSearchCriteria(
                null, null, null, null, null, null, null, null, null, "FLN-4500-000123", 0, 20));
        assertThat(byReference.items()).anyMatch(item -> item.objectType().equals("FachsystemReferenz"));

        GlobalSearchResult firstPage = globalSearchService.search(new GlobalSearchCriteria(null, 0, 2));
        assertThat(firstPage.items()).hasSize(2);
        assertThat(firstPage.totalElements()).isGreaterThan(2);
        assertThat(firstPage.hasNext()).isTrue();
    }

    @Test
    @WithMockUser(username = "sachbearbeiter", roles = "MABILLON_SACHBEARBEITER")
    void phaseSixSearchAndControlPagesRenderThroughHttp() throws Exception {
        mockMvc.perform(get("/suche").param("q", "Bodenrain"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Systemweite Suche")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AGI-D-2026-000007")));
        mockMvc.perform(get("/geschaeftskontrolle"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Geschäftskontrolle")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Offene Geschäfte")));
    }
}
