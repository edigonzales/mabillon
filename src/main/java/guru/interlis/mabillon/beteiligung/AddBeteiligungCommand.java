package guru.interlis.mabillon.beteiligung;

import java.time.LocalDate;
import java.util.UUID;

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
        if (geschaeftNumber == null || beteiligterTid == null
                || rollenCode == null || rollenCode.isBlank()) {
            throw new IllegalArgumentException("Geschäft, Beteiligter und Rolle sind erforderlich.");
        }
        if (gueltigVon != null && gueltigBis != null && gueltigBis.isBefore(gueltigVon)) {
            throw new IllegalArgumentException("Gültig-bis darf nicht vor Gültig-von liegen.");
        }
    }
}
