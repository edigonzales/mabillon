package guru.interlis.mabillon.unterlage;

import java.time.LocalDate;

import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.numbering.GeschaeftNumber;

public record RegisterUnterlageCommand(
        DossierNumber dossierNumber,
        GeschaeftNumber geschaeftNumber,
        String title,
        String typCode,
        LocalDate documentDate,
        LocalDate incomingDate,
        LocalDate outgoingDate,
        boolean aktenrelevant,
        String dateiformat,
        String bemerkungen) {

    public RegisterUnterlageCommand {
        if (dossierNumber == null || title == null || title.isBlank()
                || typCode == null || typCode.isBlank()) {
            throw new IllegalArgumentException("Dossier, Titel und Unterlagentyp sind erforderlich.");
        }
        if (incomingDate != null && outgoingDate != null && outgoingDate.isBefore(incomingDate)) {
            throw new IllegalArgumentException("Ausgangsdatum darf nicht vor dem Eingangsdatum liegen.");
        }
    }
}
