package guru.interlis.mabillon.beteiligung;

import java.time.LocalDate;
import java.util.UUID;

public record BeteiligungView(
        UUID tid,
        String geschaeftsnummer,
        UUID beteiligterTid,
        String beteiligterName,
        String rollenCode,
        String rollenName,
        String rollenbezeichnung,
        LocalDate gueltigVon,
        LocalDate gueltigBis,
        String bemerkung) {

    public boolean active(LocalDate today) {
        return gueltigBis == null || !gueltigBis.isBefore(today);
    }
}
