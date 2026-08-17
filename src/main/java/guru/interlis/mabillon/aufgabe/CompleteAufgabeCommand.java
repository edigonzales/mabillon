package guru.interlis.mabillon.aufgabe;

import java.util.UUID;

public record CompleteAufgabeCommand(UUID tid, String comment) {

    public CompleteAufgabeCommand {
        if (tid == null) {
            throw new IllegalArgumentException("Aufgabe ist erforderlich.");
        }
    }
}
