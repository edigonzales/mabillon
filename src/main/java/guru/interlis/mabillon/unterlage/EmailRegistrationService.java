package guru.interlis.mabillon.unterlage;

import guru.interlis.mabillon.storage.DocumentUpload;

import org.springframework.stereotype.Service;

@Service
public final class EmailRegistrationService {

    private final UnterlageService unterlageService;

    public EmailRegistrationService(UnterlageService unterlageService) {
        this.unterlageService = unterlageService;
    }

    public UnterlageView registerIncomingEmail(EmailRegistrationCommand command, DocumentUpload upload) {
        return unterlageService.register(new RegisterUnterlageCommand(
                command.dossierNumber(), command.geschaeftNumber(), command.title(), "EMAIL_EINGANG",
                command.date(), command.date(), null, command.aktenrelevant(), "EML", command.bemerkungen()), upload);
    }

    public UnterlageView registerOutgoingEmail(EmailRegistrationCommand command, DocumentUpload upload) {
        return unterlageService.register(new RegisterUnterlageCommand(
                command.dossierNumber(), command.geschaeftNumber(), command.title(), "EMAIL_AUSGANG",
                command.date(), null, command.date(), command.aktenrelevant(), "EML", command.bemerkungen()), upload);
    }
}
