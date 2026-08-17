package guru.interlis.mabillon.stammdaten;

public record OrganisationseinheitCreateCommand(
        String kuerzel,
        String name,
        String beschreibung,
        String uebergeordneteEinheit) {

    public OrganisationseinheitCreateCommand {
        requireText(kuerzel, "kuerzel");
        requireText(name, "name");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
