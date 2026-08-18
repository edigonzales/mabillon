package guru.interlis.mabillon.aufgabe;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import guru.interlis.mabillon.domain.FieldError;
import guru.interlis.mabillon.domain.ValidationException;

public record UpdateAufgabeCommand(
        UUID tid,
        String title,
        String description,
        LocalDate dueDate,
        Integer priority) {

    public UpdateAufgabeCommand {
        List<FieldError> errors = new ArrayList<>();
        if (tid == null) errors.add(new FieldError("tid", "required", "Aufgabe ist erforderlich."));
        if (title == null || title.isBlank()) errors.add(new FieldError("title", "required", "Titel ist erforderlich."));
        if (priority != null && (priority < 0 || priority > 5)) {
            errors.add(new FieldError("priority", "range", "Priorität muss zwischen 0 und 5 liegen."));
        }
        if (!errors.isEmpty()) throw new ValidationException(errors);
    }
}
