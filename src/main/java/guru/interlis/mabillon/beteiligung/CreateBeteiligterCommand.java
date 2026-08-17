package guru.interlis.mabillon.beteiligung;

public record CreateBeteiligterCommand(
        String typ,
        String name,
        String vorname,
        String organisation,
        String email,
        String telefon,
        String adresse,
        String externeReferenz) {

    public CreateBeteiligterCommand {
        requireText(typ, "typ");
        requireText(name, "name");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " darf nicht leer sein.");
        }
    }
}
