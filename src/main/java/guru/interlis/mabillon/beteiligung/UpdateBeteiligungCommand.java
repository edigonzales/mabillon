package guru.interlis.mabillon.beteiligung;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateBeteiligungCommand(
        UUID tid,
        String rollenbezeichnung,
        LocalDate gueltigVon,
        LocalDate gueltigBis,
        String bemerkung) {

    public UpdateBeteiligungCommand {
        if (tid == null) {
            throw new IllegalArgumentException("Beteiligung ist erforderlich.");
        }
        if (gueltigVon != null && gueltigBis != null && gueltigBis.isBefore(gueltigVon)) {
            throw new IllegalArgumentException("Gültig-bis darf nicht vor Gültig-von liegen.");
        }
    }
}
