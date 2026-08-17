package guru.interlis.mabillon.unterlage;

import java.time.LocalDate;

import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.numbering.GeschaeftNumber;

public record EmailRegistrationCommand(
        DossierNumber dossierNumber,
        GeschaeftNumber geschaeftNumber,
        String title,
        LocalDate date,
        boolean aktenrelevant,
        String bemerkungen) {

    public EmailRegistrationCommand {
        if (dossierNumber == null || title == null || title.isBlank() || date == null) {
            throw new IllegalArgumentException("Dossier, Titel und E-Mail-Datum sind erforderlich.");
        }
    }
}
