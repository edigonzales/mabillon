package guru.interlis.mabillon.domain;

import java.util.List;
import java.util.Objects;

public final class ValidationException extends MabillonException {

    private final List<FieldError> errors;

    public ValidationException(List<FieldError> errors) {
        super("Bitte prüfen Sie die markierten Eingaben.");
        Objects.requireNonNull(errors, "errors");
        if (errors.isEmpty()) {
            throw new IllegalArgumentException("Mindestens ein Validierungsfehler ist erforderlich.");
        }
        this.errors = List.copyOf(errors);
    }

    public ValidationException(FieldError error) {
        this(List.of(error));
    }

    public List<FieldError> errors() {
        return errors;
    }
}
