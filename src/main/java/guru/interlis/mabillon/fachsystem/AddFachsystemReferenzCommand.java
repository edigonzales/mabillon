package guru.interlis.mabillon.fachsystem;

import guru.interlis.mabillon.numbering.GeschaeftNumber;

public record AddFachsystemReferenzCommand(
        GeschaeftNumber geschaeftNumber,
        String systemCode,
        String objektTyp,
        String objektId,
        String mutationId,
        String link,
        String beschreibung) {

    public AddFachsystemReferenzCommand {
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
