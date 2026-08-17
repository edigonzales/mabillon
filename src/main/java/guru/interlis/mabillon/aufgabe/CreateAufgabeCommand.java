package guru.interlis.mabillon.aufgabe;

import java.time.LocalDate;

import guru.interlis.mabillon.numbering.GeschaeftNumber;

public record CreateAufgabeCommand(
        GeschaeftNumber geschaeftNumber,
        String title,
        String description,
        String typCode,
        LocalDate dueDate,
        Integer priority,
        String assignedUsername,
        String assignedOrganisationseinheit) {

    public CreateAufgabeCommand {
        if (geschaeftNumber == null || title == null || title.isBlank()
                || typCode == null || typCode.isBlank()) {
            throw new IllegalArgumentException("Geschäft, Titel und Aufgabentyp sind erforderlich.");
        }
        if (priority != null && (priority < 0 || priority > 5)) {
            throw new IllegalArgumentException("Priorität muss zwischen 0 und 5 liegen.");
        }
        if (assignedUsername != null && !assignedUsername.isBlank()
                && assignedOrganisationseinheit != null && !assignedOrganisationseinheit.isBlank()) {
            throw new IllegalArgumentException("Eine Aufgabe wird entweder einem Benutzer oder einer Organisationseinheit zugewiesen.");
        }
    }
}
