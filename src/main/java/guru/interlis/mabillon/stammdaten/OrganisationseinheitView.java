package guru.interlis.mabillon.stammdaten;

public record OrganisationseinheitView(
        String kuerzel,
        String name,
        String beschreibung,
        String status,
        String uebergeordneteEinheit) {

    public boolean active() {
        return "aktiv".equalsIgnoreCase(status);
    }
}
