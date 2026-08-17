package guru.interlis.mabillon.fachsystem;

import guru.interlis.mabillon.numbering.DossierNumber;

public record AddFachsystemReferenzToDossierCommand(
        DossierNumber dossierNumber,
        String systemCode,
        String objektTyp,
        String objektId,
        String mutationId,
        String link,
        String beschreibung) {

    public AddFachsystemReferenzToDossierCommand {
        require(systemCode, "systemCode");
        require(objektTyp, "objektTyp");
        require(objektId, "objektId");
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " darf nicht leer sein.");
        }
    }
}
