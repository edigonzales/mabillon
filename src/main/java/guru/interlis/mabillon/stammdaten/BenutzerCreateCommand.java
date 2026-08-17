package guru.interlis.mabillon.stammdaten;

public record BenutzerCreateCommand(
        String username,
        String name,
        String email,
        String organisationseinheit) {

    public BenutzerCreateCommand {
        requireText(username, "username");
        requireText(name, "name");
        requireText(organisationseinheit, "organisationseinheit");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
