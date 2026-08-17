package guru.interlis.mabillon.geschaeft;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import guru.interlis.mabillon.domain.FieldError;
import guru.interlis.mabillon.domain.ValidationException;
import guru.interlis.mabillon.numbering.DossierNumber;

public record OpenGeschaeftCommand(
        DossierNumber dossierNumber,
        String title,
        String shortDescription,
        String geschaeftsartCode,
        String federfuehrungKuerzel,
        String verantwortlicherUsername,
        LocalDate eingangsdatum,
        LocalDate eroeffnungsdatum,
        LocalDate dueDate,
        Integer priority) {

    public OpenGeschaeftCommand {
        List<FieldError> errors = new ArrayList<>();
        if (dossierNumber == null) {
            errors.add(new FieldError("dossierNumber", "required", "Dossier ist erforderlich."));
        }
        requireText(errors, "title", title, "Titel ist erforderlich.");
        requireText(errors, "type", geschaeftsartCode, "Geschäftsart ist erforderlich.");
        requireText(errors, "federation", federfuehrungKuerzel, "Federführung ist erforderlich.");
        requireText(errors, "responsible", verantwortlicherUsername, "Verantwortliche Person ist erforderlich.");
        if (priority != null && (priority < 0 || priority > 5)) {
            errors.add(new FieldError("priority", "range", "Priorität muss zwischen 0 und 5 liegen."));
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private static void requireText(List<FieldError> errors, String field, String value, String message) {
        if (value == null || value.isBlank()) {
            errors.add(new FieldError(field, "required", message));
        }
    }
}
