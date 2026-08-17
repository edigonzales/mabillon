package guru.interlis.mabillon.beteiligung;

import java.time.LocalDate;
import java.util.UUID;

public record EndBeteiligungCommand(UUID tid, LocalDate endDate) {

    public EndBeteiligungCommand {
        if (tid == null || endDate == null) {
            throw new IllegalArgumentException("Beteiligung und Enddatum sind erforderlich.");
        }
    }
}
