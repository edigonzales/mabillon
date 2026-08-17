package guru.interlis.mabillon.beteiligung;

import java.util.UUID;

public record UpdateBeteiligterCommand(
        UUID tid,
        String typ,
        String name,
        String vorname,
        String organisation,
        String email,
        String telefon,
        String adresse,
        String externeReferenz) {

    public UpdateBeteiligterCommand {
        if (tid == null || name == null || name.isBlank() || typ == null || typ.isBlank()) {
            throw new IllegalArgumentException("tid, typ und name dürfen nicht leer sein.");
        }
    }
}
