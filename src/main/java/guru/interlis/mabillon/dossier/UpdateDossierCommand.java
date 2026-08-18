package guru.interlis.mabillon.dossier;

import java.util.ArrayList;
import java.util.List;

import guru.interlis.mabillon.domain.FieldError;
import guru.interlis.mabillon.domain.ValidationException;
import guru.interlis.mabillon.numbering.DossierNumber;

public record UpdateDossierCommand(
        DossierNumber number,
        String title,
        String description,
        String verantwortlicherUsername,
        String remarks) {

    public UpdateDossierCommand {
        List<FieldError> errors = new ArrayList<>();
        if (number == null) errors.add(new FieldError("number", "required", "Dossier ist erforderlich."));
        if (title == null || title.isBlank()) errors.add(new FieldError("title", "required", "Titel ist erforderlich."));
        if (!errors.isEmpty()) throw new ValidationException(errors);
    }
}
