package guru.interlis.mabillon.geschaeft;

import java.time.LocalDate;

import guru.interlis.mabillon.numbering.GeschaeftNumber;

public record UpdateGeschaeftCommand(
        GeschaeftNumber number,
        String title,
        String shortDescription,
        String verantwortlicherUsername,
        LocalDate dueDate,
        Integer priority) {

    public UpdateGeschaeftCommand {
        if (number == null || title == null || title.isBlank()) {
            throw new IllegalArgumentException("number und title dürfen nicht leer sein.");
        }
        if (priority != null && (priority < 0 || priority > 5)) {
            throw new IllegalArgumentException("priority muss zwischen 0 und 5 liegen.");
        }
    }
}
