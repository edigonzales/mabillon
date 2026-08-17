package guru.interlis.mabillon.dossier;

import java.time.LocalDate;

public record OpenDossierCommand(
        String title,
        String description,
        String registraturplanPositionCode,
        String federfuehrungKuerzel,
        String verantwortlicherUsername,
        LocalDate openingDate) {

    public OpenDossierCommand {
        requireText(title, "title");
        requireText(registraturplanPositionCode, "registraturplanPositionCode");
        requireText(federfuehrungKuerzel, "federfuehrungKuerzel");
        requireText(verantwortlicherUsername, "verantwortlicherUsername");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " darf nicht leer sein.");
        }
    }
}
