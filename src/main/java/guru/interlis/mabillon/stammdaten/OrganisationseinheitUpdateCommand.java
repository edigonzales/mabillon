package guru.interlis.mabillon.stammdaten;

public record OrganisationseinheitUpdateCommand(
        String kuerzel,
        String name,
        String beschreibung,
        String uebergeordneteEinheit) {

    public OrganisationseinheitUpdateCommand {
        if (kuerzel == null || kuerzel.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Kürzel und Name sind erforderlich.");
        }
    }
}
