package guru.interlis.mabillon.unterlage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import guru.interlis.mabillon.domain.FieldError;
import guru.interlis.mabillon.domain.ValidationException;

public record UpdateUnterlageCommand(
        UUID tid,
        String title,
        String typCode,
        LocalDate documentDate,
        LocalDate incomingDate,
        LocalDate outgoingDate,
        String dateiformat,
        String bemerkungen) {

    public UpdateUnterlageCommand {
        List<FieldError> errors = new ArrayList<>();
        if (tid == null) errors.add(new FieldError("tid", "required", "Unterlage ist erforderlich."));
        if (title == null || title.isBlank()) errors.add(new FieldError("title", "required", "Titel ist erforderlich."));
        if (typCode == null || typCode.isBlank()) errors.add(new FieldError("typCode", "required", "Unterlagentyp ist erforderlich."));
        if (incomingDate != null && outgoingDate != null && outgoingDate.isBefore(incomingDate)) {
            errors.add(new FieldError("outgoingDate", "dateOrder", "Ausgangsdatum darf nicht vor dem Eingangsdatum liegen."));
        }
        if (!errors.isEmpty()) throw new ValidationException(errors);
    }
}
