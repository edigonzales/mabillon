package guru.interlis.mabillon.stammdaten;

public record BenutzerUpdateCommand(
        String username,
        String name,
        String email,
        String organisationseinheit) {

    public BenutzerUpdateCommand {
        if (username == null || username.isBlank() || name == null || name.isBlank()
                || organisationseinheit == null || organisationseinheit.isBlank()) {
            throw new IllegalArgumentException("Username, Name und Organisationseinheit sind erforderlich.");
        }
    }
}
