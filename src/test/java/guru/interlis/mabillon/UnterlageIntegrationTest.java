package guru.interlis.mabillon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;

import guru.interlis.mabillon.domain.DomainRuleViolationException;
import guru.interlis.mabillon.geschaeft.GeschaeftView;
import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.numbering.GeschaeftNumber;
import guru.interlis.mabillon.storage.DocumentUpload;
import guru.interlis.mabillon.unterlage.AssignUnterlageCommand;
import guru.interlis.mabillon.unterlage.OpenedDocument;
import guru.interlis.mabillon.unterlage.RegisterUnterlageCommand;
import guru.interlis.mabillon.unterlage.UnterlageView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class UnterlageIntegrationTest extends MabillonIntegrationTestSupport {

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void phaseFiveStoresRegistersAndDownloadsDocumentWithoutUsingFilenameAsPath() throws Exception {
        GeschaeftView business = newBusiness("Unterlage");
        byte[] bytes = "Mabillon Phase 5\n".getBytes(StandardCharsets.UTF_8);
        UnterlageView document = unterlageService.register(new RegisterUnterlageCommand(
                DossierNumber.parse(business.dossierNumber()), GeschaeftNumber.parse(business.number()),
                "Phase 5 Textunterlage", "AKTENNOTIZ", LocalDate.of(2026, 8, 16),
                LocalDate.of(2026, 8, 16), null, true, "TXT", "Ablagetest."),
                new DocumentUpload("../../ausserhalb.txt", "text/plain", new ByteArrayInputStream(bytes)));

        assertThat(document.filename()).isEqualTo("../../ausserhalb.txt");
        assertThat(document.storageUri()).doesNotContain("..").startsWith("mabillon:objects/");
        assertThat(documentStorage.exists(document.storageUri())).isTrue();
        try (OpenedDocument opened = unterlageContentService.open(document.tid())) {
            assertThat(opened.content().readAllBytes()).isEqualTo(bytes);
        }
        mockMvc.perform(get("/unterlagen/{tid}/download", document.tid()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain"))
                .andExpect(content().bytes(bytes));
        assertThat(unterlageQueryService.forGeschaeft(GeschaeftNumber.parse(business.number())))
                .extracting(UnterlageView::tid).contains(document.tid());
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void phaseFiveEnforcesDossierConsistencyAndCleansStagingAfterRegistrationFailure() {
        GeschaeftView first = newBusiness("Unterlage eins");
        GeschaeftView second = newBusiness("Unterlage zwei");
        byte[] bytes = "Konsistenztest".getBytes(StandardCharsets.UTF_8);
        UnterlageView document = unterlageService.register(new RegisterUnterlageCommand(
                DossierNumber.parse(first.dossierNumber()), null, "Dossierweite Unterlage", "AKTENNOTIZ",
                null, null, null, true, "TXT", null),
                new DocumentUpload("dossierweite.txt", "text/plain", new ByteArrayInputStream(bytes)));

        assertThatThrownBy(() -> unterlageService.assignToGeschaeft(new AssignUnterlageCommand(
                document.tid(), GeschaeftNumber.parse(second.number()))))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("demselben Dossier");
        assertThatThrownBy(() -> unterlageService.register(new RegisterUnterlageCommand(
                DossierNumber.parse(first.dossierNumber()), null, "Fehlerhafte Unterlage", "NICHT_VORHANDEN",
                null, null, null, true, "TXT", null),
                new DocumentUpload("failure.txt", "text/plain", new ByteArrayInputStream(bytes))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unterlagentyp");
        try (var files = Files.walk(STORAGE_ROOT.resolve("staging"))) {
            assertThat(files.filter(Files::isRegularFile).count()).isZero();
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
