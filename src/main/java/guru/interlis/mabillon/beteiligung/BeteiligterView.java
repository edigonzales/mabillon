package guru.interlis.mabillon.beteiligung;

import java.util.UUID;

public record BeteiligterView(
        UUID tid,
        String typ,
        String name,
        String vorname,
        String organisation,
        String email,
        String telefon,
        String adresse,
        String externeReferenz) {
}
