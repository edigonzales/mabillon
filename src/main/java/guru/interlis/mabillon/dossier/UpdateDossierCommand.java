package guru.interlis.mabillon.dossier;

import guru.interlis.mabillon.numbering.DossierNumber;

public record UpdateDossierCommand(
        DossierNumber number,
        String title,
        String description,
        String verantwortlicherUsername,
        String remarks) {

    public UpdateDossierCommand {
        if (number == null || title == null || title.isBlank()) {
            throw new IllegalArgumentException("number und title dürfen nicht leer sein.");
        }
    }
}
