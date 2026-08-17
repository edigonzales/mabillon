package guru.interlis.mabillon.fachsystem;

import java.util.UUID;

public record FachsystemReferenzView(
        UUID tid,
        String systemCode,
        String objektTyp,
        String objektId,
        String mutationId,
        String link,
        String beschreibung,
        String dossierNumber,
        String geschaeftsnummer) {
}
