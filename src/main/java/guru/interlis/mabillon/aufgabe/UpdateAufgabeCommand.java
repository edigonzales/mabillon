package guru.interlis.mabillon.aufgabe;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateAufgabeCommand(
        UUID tid,
        String title,
        String description,
        LocalDate dueDate,
        Integer priority) {

    public UpdateAufgabeCommand {
        if (tid == null || title == null || title.isBlank()) {
            throw new IllegalArgumentException("tid und Titel dürfen nicht leer sein.");
        }
        if (priority != null && (priority < 0 || priority > 5)) {
            throw new IllegalArgumentException("Priorität muss zwischen 0 und 5 liegen.");
        }
    }
}
