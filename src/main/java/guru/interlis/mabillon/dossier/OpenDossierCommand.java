package guru.interlis.mabillon.dossier;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import guru.interlis.mabillon.domain.FieldError;
import guru.interlis.mabillon.domain.ValidationException;

public record OpenDossierCommand(
        String title,
        String description,
        String registraturplanPositionCode,
        String federfuehrungKuerzel,
        String verantwortlicherUsername,
        LocalDate openingDate) {

    public OpenDossierCommand {
        List<FieldError> errors = new ArrayList<>();
        requireText(errors, "title", title, "Titel ist erforderlich.");
        requireText(errors, "position", registraturplanPositionCode, "Registraturplanposition ist erforderlich.");
        requireText(errors, "federation", federfuehrungKuerzel, "Federführung ist erforderlich.");
        requireText(errors, "responsible", verantwortlicherUsername, "Verantwortliche Person ist erforderlich.");
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
