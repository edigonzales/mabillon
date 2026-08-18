package guru.interlis.mabillon.unterlage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import guru.interlis.mabillon.domain.FieldError;
import guru.interlis.mabillon.domain.ValidationException;
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
        List<FieldError> errors = new ArrayList<>();
        if (dossierNumber == null) errors.add(new FieldError("dossierNumber", "required", "Dossier ist erforderlich."));
        if (title == null || title.isBlank()) errors.add(new FieldError("title", "required", "Titel ist erforderlich."));
        if (typCode == null || typCode.isBlank()) errors.add(new FieldError("typCode", "required", "Unterlagentyp ist erforderlich."));
        if (incomingDate != null && outgoingDate != null && outgoingDate.isBefore(incomingDate)) {
            errors.add(new FieldError("outgoingDate", "dateOrder", "Ausgangsdatum darf nicht vor dem Eingangsdatum liegen."));
        }
        if (!errors.isEmpty()) throw new ValidationException(errors);
    }
}
