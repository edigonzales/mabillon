package guru.interlis.mabillon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import guru.interlis.mabillon.archivierung.CreateArchivAblieferungCommand;
import guru.interlis.mabillon.geschaeft.GeschaeftView;
import guru.interlis.mabillon.journal.EreignisTyp;
import guru.interlis.mabillon.numbering.GeschaeftNumber;
import guru.interlis.mabillon.persistence.cayenne.Archivablieferung;
import guru.interlis.mabillon.persistence.cayenne.Geschaeft;
import guru.interlis.mabillon.security.AuthorizationException;
import org.apache.cayenne.query.ObjectSelect;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class GeschaeftAuditIntegrationTest extends MabillonIntegrationTestSupport {

    @Test
    @WithMockUser(username = "sachbearbeiter", roles = "MABILLON_SACHBEARBEITER")
    void updatesBusinessWithoutChangingIdentityAndAttributesJournalToMappedActor() throws Exception {
        GeschaeftView before = newBusiness("UC-009 audit update");

        mockMvc.perform(post("/geschaefte/{number}", before.number())
                        .with(csrf())
                        .param("title", "Musterwil – fachlich aktualisiert")
                        .param("shortDescription", "UC-009 Final Verification")
                        .param("dueDate", "2026-09-30")
                        .param("priority", "4")
                        .param("responsible", "a.keller"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/geschaefte/" + before.number()));

        var after = geschaeftQueryService.findByNumber(before.number()).orElseThrow();
        assertThat(after.number()).isEqualTo(before.number());
        assertThat(after.geschaeftsartCode()).isEqualTo(before.geschaeftsartCode());
        assertThat(after.title()).isEqualTo("Musterwil – fachlich aktualisiert");

        Geschaeft persisted = unitOfWork.read(context -> ObjectSelect.query(Geschaeft.class)
                .where(Geschaeft.GESCHAEFTSNUMMER.eq(before.number())).selectFirst(context));
        assertThat(persisted.getFaelligam()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(persisted.getPrioritaet()).isEqualTo(4);
        assertThat(persisted.getGeschaeftsnummer()).isEqualTo(before.number());
        assertThat(persisted.getGeschaeftsart().getAcode()).isEqualTo(before.geschaeftsartCode());

        assertThat(journalQueryService.findForGeschaeft(GeschaeftNumber.parse(before.number()), 20))
                .filteredOn(entry -> entry.typ() == EreignisTyp.Geaendert
                        && "Geschäft geändert.".equals(entry.bemerkung()))
                .singleElement()
                .satisfies(entry -> assertThat(entry.username()).isEqualTo("a.keller"));
    }

    @Test
    @WithMockUser(username = "external-user")
    void businessUpdateRequiresEditPermission() {
        var business = geschaeftQueryService.findByNumber("AGI-G-2026-000421").orElseThrow();

        assertThatThrownBy(() -> geschaeftService.update(new guru.interlis.mabillon.geschaeft.UpdateGeschaeftCommand(
                GeschaeftNumber.parse(business.number()), "Nicht erlaubt", null, null, null, null)))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("EDIT_GESCHAEFT");
    }

    @Test
    @WithMockUser(username = "unmapped-archive-admin", roles = "MABILLON_ADMIN")
    void archiveCreationFailsClosedInsteadOfUsingAnotherDomainUserAsFallback() {
        long before = unitOfWork.read(context -> (long) ObjectSelect.query(Archivablieferung.class).select(context).size());

        assertThatThrownBy(() -> archivAblieferungService.create(new CreateArchivAblieferungCommand(
                "AGI", "Unmapped actor", "Staatsarchiv", null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("kein fachlicher Benutzer");

        long after = unitOfWork.read(context -> (long) ObjectSelect.query(Archivablieferung.class).select(context).size());
        assertThat(after).isEqualTo(before);
    }
}
