package guru.interlis.mabillon.beteiligung;

import java.util.ArrayList;
import java.util.List;

import guru.interlis.mabillon.domain.FieldError;
import guru.interlis.mabillon.domain.ValidationException;

public record CreateBeteiligterCommand(
        String typ,
        String name,
        String vorname,
        String organisation,
        String email,
        String telefon,
        String adresse,
        String externeReferenz) {

    public CreateBeteiligterCommand {
        List<FieldError> errors = new ArrayList<>();
        requireText(errors, "typ", typ, "Typ ist erforderlich.");
        requireText(errors, "name", name, "Name ist erforderlich.");
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
