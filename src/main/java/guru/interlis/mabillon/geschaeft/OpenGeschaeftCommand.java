package guru.interlis.mabillon.geschaeft;

import java.time.LocalDate;

import guru.interlis.mabillon.numbering.DossierNumber;

public record OpenGeschaeftCommand(
        DossierNumber dossierNumber,
        String title,
        String shortDescription,
        String geschaeftsartCode,
        String federfuehrungKuerzel,
        String verantwortlicherUsername,
        LocalDate eingangsdatum,
        LocalDate eroeffnungsdatum,
        LocalDate dueDate,
        Integer priority) {

    public OpenGeschaeftCommand {
        if (dossierNumber == null) {
            throw new IllegalArgumentException("dossierNumber darf nicht null sein.");
        }
        requireText(title, "title");
        requireText(geschaeftsartCode, "geschaeftsartCode");
        requireText(federfuehrungKuerzel, "federfuehrungKuerzel");
        requireText(verantwortlicherUsername, "verantwortlicherUsername");
        if (priority != null && (priority < 0 || priority > 5)) {
            throw new IllegalArgumentException("priority muss zwischen 0 und 5 liegen.");
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " darf nicht leer sein.");
        }
    }
}
