package guru.interlis.mabillon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import guru.interlis.mabillon.geschaeft.GeschaeftView;
import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.numbering.GeschaeftNumber;
import guru.interlis.mabillon.security.AuthorizationException;
import guru.interlis.mabillon.storage.DocumentUpload;
import guru.interlis.mabillon.unterlage.EmailRegistrationCommand;
import guru.interlis.mabillon.unterlage.EmailRegistrationService;
import guru.interlis.mabillon.unterlage.UnterlageView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class EmailRegistrationIntegrationTest extends MabillonIntegrationTestSupport {

    @Autowired
    private EmailRegistrationService emailRegistrationService;

    @Test
    @WithMockUser(username = "sachbearbeiter", roles = "MABILLON_SACHBEARBEITER")
    void registersIncomingAndOutgoingEmailWithCorrectDatesAndStorage() throws Exception {
        GeschaeftView business = newBusiness("E-Mail-Registrierung");
        DossierNumber dossierNumber = DossierNumber.parse(business.dossierNumber());
        GeschaeftNumber geschaeftNumber = GeschaeftNumber.parse(business.number());

        byte[] incomingBytes = "From: gemeinde@example.test\nSubject: Eingang\n".getBytes(StandardCharsets.UTF_8);
        UnterlageView incoming = emailRegistrationService.registerIncomingEmail(
                new EmailRegistrationCommand(dossierNumber, geschaeftNumber, "Eingegangene E-Mail",
                        LocalDate.of(2026, 8, 18), true, "UC-016"),
                new DocumentUpload("eingang.eml", "message/rfc822", new ByteArrayInputStream(incomingBytes)));

        assertThat(incoming.typCode()).isEqualTo("EMAIL_EINGANG");
        assertThat(incoming.documentDate()).isEqualTo(LocalDate.of(2026, 8, 18));
        assertThat(incoming.incomingDate()).isEqualTo(LocalDate.of(2026, 8, 18));
        assertThat(incoming.outgoingDate()).isNull();
        assertThat(incoming.dateiformat()).isEqualTo("EML");
        assertThat(documentStorage.exists(incoming.storageUri())).isTrue();
        try (var opened = unterlageContentService.open(incoming.tid())) {
            assertThat(opened.content().readAllBytes()).isEqualTo(incomingBytes);
        }

        byte[] outgoingBytes = "To: gemeinde@example.test\nSubject: Ausgang\n".getBytes(StandardCharsets.UTF_8);
        UnterlageView outgoing = emailRegistrationService.registerOutgoingEmail(
                new EmailRegistrationCommand(dossierNumber, geschaeftNumber, "Ausgehende E-Mail",
                        LocalDate.of(2026, 8, 19), true, "UC-017"),
                new DocumentUpload("ausgang.eml", "message/rfc822", new ByteArrayInputStream(outgoingBytes)));

        assertThat(outgoing.typCode()).isEqualTo("EMAIL_AUSGANG");
        assertThat(outgoing.documentDate()).isEqualTo(LocalDate.of(2026, 8, 19));
        assertThat(outgoing.incomingDate()).isNull();
        assertThat(outgoing.outgoingDate()).isEqualTo(LocalDate.of(2026, 8, 19));
        assertThat(outgoing.dateiformat()).isEqualTo("EML");
        assertThat(unterlageQueryService.forGeschaeft(geschaeftNumber))
                .extracting(UnterlageView::tid)
                .contains(incoming.tid(), outgoing.tid());
    }

    @Test
    @WithMockUser(username = "external-user")
    void emailRegistrationRequiresUnterlageEditPermission() {
        GeschaeftView business = geschaeftQueryService.findByNumber("AGI-G-2026-000421").orElseThrow();
        EmailRegistrationCommand command = new EmailRegistrationCommand(
                DossierNumber.parse(business.dossierNumber()), GeschaeftNumber.parse(business.number()),
                "Nicht erlaubt", LocalDate.of(2026, 8, 18), true, null);

        assertThatThrownBy(() -> emailRegistrationService.registerIncomingEmail(
                command,
                new DocumentUpload("denied.eml", "message/rfc822",
                        new ByteArrayInputStream("denied".getBytes(StandardCharsets.UTF_8)))))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("EDIT_UNTERLAGE");
    }
}
