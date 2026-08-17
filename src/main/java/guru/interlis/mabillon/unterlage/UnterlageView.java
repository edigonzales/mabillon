package guru.interlis.mabillon.unterlage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record UnterlageView(
        UUID tid,
        String title,
        String typCode,
        String typName,
        String status,
        String dossierNumber,
        String geschaeftsnummer,
        LocalDate documentDate,
        LocalDate incomingDate,
        LocalDate outgoingDate,
        LocalDateTime registeredAt,
        boolean aktenrelevant,
        String filename,
        String mimeType,
        Long size,
        String storageUri,
        String sha256,
        String dateiformat,
        String bemerkungen) {
}
