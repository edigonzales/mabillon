package guru.interlis.mabillon.geschaeft;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import guru.interlis.mabillon.domain.FieldError;
import guru.interlis.mabillon.domain.ValidationException;
import guru.interlis.mabillon.numbering.GeschaeftNumber;

public record UpdateGeschaeftCommand(
        GeschaeftNumber number,
        String title,
        String shortDescription,
        String verantwortlicherUsername,
        LocalDate dueDate,
        Integer priority) {

    public UpdateGeschaeftCommand {
        List<FieldError> errors = new ArrayList<>();
        if (number == null) errors.add(new FieldError("number", "required", "Geschäft ist erforderlich."));
        if (title == null || title.isBlank()) errors.add(new FieldError("title", "required", "Titel ist erforderlich."));
        if (priority != null && (priority < 0 || priority > 5)) {
            errors.add(new FieldError("priority", "range", "Priorität muss zwischen 0 und 5 liegen."));
        }
        if (!errors.isEmpty()) throw new ValidationException(errors);
    }
}
