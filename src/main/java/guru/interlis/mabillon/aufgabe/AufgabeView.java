package guru.interlis.mabillon.aufgabe;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record AufgabeView(
        UUID tid,
        String geschaeftsnummer,
        String title,
        String description,
        String typCode,
        String typName,
        String status,
        LocalDate dueDate,
        Integer priority,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        String assignedUsername,
        String assignedOrganisationseinheit) {

    public boolean open() {
        return !"Erledigt".equalsIgnoreCase(status) && !"Abgebrochen".equalsIgnoreCase(status);
    }
}
