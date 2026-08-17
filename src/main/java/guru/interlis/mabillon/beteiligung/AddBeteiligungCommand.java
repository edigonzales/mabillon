package guru.interlis.mabillon.beteiligung;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import guru.interlis.mabillon.domain.FieldError;
import guru.interlis.mabillon.domain.ValidationException;
import guru.interlis.mabillon.numbering.GeschaeftNumber;

public record AddBeteiligungCommand(
        GeschaeftNumber geschaeftNumber,
        UUID beteiligterTid,
        String rollenCode,
        String rollenbezeichnung,
        LocalDate gueltigVon,
        LocalDate gueltigBis,
        String bemerkung) {

    public AddBeteiligungCommand {
        List<FieldError> errors = new ArrayList<>();
        if (geschaeftNumber == null) errors.add(new FieldError("geschaeftNumber", "required", "Geschäft ist erforderlich."));
        if (beteiligterTid == null) errors.add(new FieldError("beteiligterTid", "required", "Beteiligter ist erforderlich."));
        if (rollenCode == null || rollenCode.isBlank()) errors.add(new FieldError("rollenCode", "required", "Rolle ist erforderlich."));
        if (!errors.isEmpty()) throw new ValidationException(errors);
        if (gueltigVon != null && gueltigBis != null && gueltigBis.isBefore(gueltigVon)) {
            throw new IllegalArgumentException("Gültig-bis darf nicht vor Gültig-von liegen.");
        }
    }
}
