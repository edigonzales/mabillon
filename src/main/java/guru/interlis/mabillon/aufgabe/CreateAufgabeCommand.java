package guru.interlis.mabillon.aufgabe;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import guru.interlis.mabillon.domain.FieldError;
import guru.interlis.mabillon.domain.ValidationException;
import guru.interlis.mabillon.numbering.GeschaeftNumber;

public record CreateAufgabeCommand(
        GeschaeftNumber geschaeftNumber,
        String title,
        String description,
        String typCode,
        LocalDate dueDate,
        Integer priority,
        String assignedUsername,
        String assignedOrganisationseinheit) {

    public CreateAufgabeCommand {
        List<FieldError> errors = new ArrayList<>();
        if (geschaeftNumber == null) errors.add(new FieldError("geschaeftNumber", "required", "Geschäft ist erforderlich."));
        if (title == null || title.isBlank()) errors.add(new FieldError("title", "required", "Titel ist erforderlich."));
        if (typCode == null || typCode.isBlank()) errors.add(new FieldError("typCode", "required", "Aufgabentyp ist erforderlich."));
        if (priority != null && (priority < 0 || priority > 5)) {
            errors.add(new FieldError("priority", "range", "Priorität muss zwischen 0 und 5 liegen."));
        }
        if (assignedUsername != null && !assignedUsername.isBlank()
                && assignedOrganisationseinheit != null && !assignedOrganisationseinheit.isBlank()) {
            String message = "Eine Aufgabe wird entweder einem Benutzer oder einer Organisationseinheit zugewiesen.";
            errors.add(new FieldError("assignedUsername", "exclusive", message));
            errors.add(new FieldError("assignedOrganisationseinheit", "exclusive", message));
        }
        if (!errors.isEmpty()) throw new ValidationException(errors);
    }
}
