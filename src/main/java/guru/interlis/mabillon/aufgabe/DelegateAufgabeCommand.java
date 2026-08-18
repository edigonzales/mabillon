package guru.interlis.mabillon.aufgabe;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import guru.interlis.mabillon.domain.FieldError;
import guru.interlis.mabillon.domain.ValidationException;

public record DelegateAufgabeCommand(UUID tid, String username, String organisationseinheit) {

    public DelegateAufgabeCommand {
        List<FieldError> errors = new ArrayList<>();
        if (tid == null) errors.add(new FieldError("tid", "required", "Aufgabe ist erforderlich."));
        boolean hasUser = username != null && !username.isBlank();
        boolean hasOrganisationseinheit = organisationseinheit != null && !organisationseinheit.isBlank();
        if (hasUser == hasOrganisationseinheit) {
            String message = "Eine Aufgabe wird genau einem Benutzer oder einer Organisationseinheit zugewiesen.";
            errors.add(new FieldError("username", "exclusive", message));
            errors.add(new FieldError("organisationseinheit", "exclusive", message));
        }
        if (!errors.isEmpty()) throw new ValidationException(errors);
    }
}
