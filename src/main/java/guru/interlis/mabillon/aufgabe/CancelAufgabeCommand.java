package guru.interlis.mabillon.aufgabe;

import java.util.UUID;

public record CancelAufgabeCommand(UUID tid, String comment) {

    public CancelAufgabeCommand {
        if (tid == null) {
            throw new IllegalArgumentException("Aufgabe ist erforderlich.");
        }
    }
}
