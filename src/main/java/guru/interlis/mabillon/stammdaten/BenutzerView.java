package guru.interlis.mabillon.stammdaten;

public record BenutzerView(
        String username,
        String name,
        String email,
        String status,
        String organisationseinheit) {

    public boolean active() {
        return "aktiv".equalsIgnoreCase(status);
    }
}
