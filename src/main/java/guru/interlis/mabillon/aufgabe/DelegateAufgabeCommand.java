package guru.interlis.mabillon.aufgabe;

import java.util.UUID;

public record DelegateAufgabeCommand(UUID tid, String username, String organisationseinheit) {

    public DelegateAufgabeCommand {
        if (tid == null) {
            throw new IllegalArgumentException("Aufgabe ist erforderlich.");
        }
        boolean hasUser = username != null && !username.isBlank();
        boolean hasOrganisationseinheit = organisationseinheit != null && !organisationseinheit.isBlank();
        if (hasUser == hasOrganisationseinheit) {
            throw new IllegalArgumentException("Eine Aufgabe wird genau einem Benutzer oder einer Organisationseinheit zugewiesen.");
        }
    }
}
