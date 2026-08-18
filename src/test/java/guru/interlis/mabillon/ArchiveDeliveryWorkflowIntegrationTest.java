package guru.interlis.mabillon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import guru.interlis.mabillon.archivierung.ArchivAblieferungView;
import guru.interlis.mabillon.archivierung.CreateArchivAblieferungCommand;
import guru.interlis.mabillon.geschaeft.ChangeProcessStatusCommand;
import guru.interlis.mabillon.geschaeft.GeschaeftView;
import guru.interlis.mabillon.geschaeft.SetResultCommand;
import guru.interlis.mabillon.journal.EreignisObjektTyp;
import guru.interlis.mabillon.numbering.ArchivAblieferungNumber;
import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.numbering.GeschaeftNumber;
import guru.interlis.mabillon.persistence.cayenne.Archivablieferung;
import guru.interlis.mabillon.security.AuthorizationException;
import org.apache.cayenne.query.ObjectSelect;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ArchiveDeliveryWorkflowIntegrationTest extends MabillonIntegrationTestSupport {

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void draftDeliveryCanAddAndRemoveEligibleDossierThroughUiAndJournalsBothChanges() throws Exception {
        GeschaeftView business = closeEligibleBusiness("Archive remove");
        DossierNumber dossierNumber = DossierNumber.parse(business.dossierNumber());
        ArchivAblieferungView delivery = archivAblieferungService.create(new CreateArchivAblieferungCommand(
                "AGI", "Phase 11.15 Ablieferung", "Staatsarchiv", null));
        ArchivAblieferungNumber deliveryNumber = ArchivAblieferungNumber.parse(delivery.deliveryNumber());
        archivAblieferungService.addDossier(deliveryNumber, dossierNumber);

        mockMvc.perform(get("/archivierung/{number}", delivery.deliveryNumber()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Entfernen")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bereit markieren")));

        mockMvc.perform(post("/archivierung/{number}/dossiers/{dossierNumber}/entfernen",
                        delivery.deliveryNumber(), dossierNumber.value()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/archivierung/" + delivery.deliveryNumber()));

        boolean empty = unitOfWork.read(context -> {
            Archivablieferung persisted = ObjectSelect.query(Archivablieferung.class)
                    .where(Archivablieferung.ABLIEFERUNGSNUMMER.eq(deliveryNumber.value()))
                    .selectFirst(context);
            return persisted != null && persisted.getArchivablieferungDossiers().isEmpty();
        });
        assertThat(empty).isTrue();
        assertThat(journalQueryService.findForObject(
                EreignisObjektTyp.ArchivAblieferung, delivery.deliveryNumber(), 20))
                .extracting(entry -> entry.bemerkung())
                .contains("Dossier der Archivablieferung hinzugefügt.", "Dossier aus Archivablieferung entfernt.");
    }

    @Test
    @WithMockUser(username = "sachbearbeiter", roles = "MABILLON_SACHBEARBEITER")
    void archiveSelectionRequiresArchiveDeliveryPermission() {
        assertThatThrownBy(() -> aussonderungQueryService.eligible(0, 20))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("MANAGE_ARCHIVE_DELIVERY");
    }

    private GeschaeftView closeEligibleBusiness(String label) {
        GeschaeftView business = newBusiness(label);
        GeschaeftNumber businessNumber = GeschaeftNumber.parse(business.number());
        geschaeftService.changeProcessStatus(new ChangeProcessStatusCommand(
                businessNumber, "ABGESCHLOSSEN", "Archivierung vorbereitet."));
        geschaeftService.setResult(new SetResultCommand(businessNumber, "GENEHMIGT", "Archivierungsentscheid."));
        geschaeftService.close(businessNumber);
        dossierService.close(DossierNumber.parse(business.dossierNumber()));
        return business;
    }
}
