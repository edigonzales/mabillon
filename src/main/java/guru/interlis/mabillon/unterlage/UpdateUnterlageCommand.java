package guru.interlis.mabillon.unterlage;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateUnterlageCommand(
        UUID tid,
        String title,
        String typCode,
        LocalDate documentDate,
        LocalDate incomingDate,
        LocalDate outgoingDate,
        String dateiformat,
        String bemerkungen) {

    public UpdateUnterlageCommand {
        if (tid == null) {
            throw new IllegalArgumentException("Unterlage ist erforderlich.");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Titel ist erforderlich.");
        }
        if (typCode == null || typCode.isBlank()) {
            throw new IllegalArgumentException("Unterlagentyp ist erforderlich.");
        }
    }
}
