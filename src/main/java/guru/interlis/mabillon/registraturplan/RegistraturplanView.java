package guru.interlis.mabillon.registraturplan;

import java.time.LocalDate;

public record RegistraturplanView(
        String code,
        String name,
        LocalDate gueltigVon,
        LocalDate gueltigBis,
        String status,
        String organisationseinheit) {

    public boolean active() {
        return "Aktiv".equalsIgnoreCase(status) || "aktiv".equalsIgnoreCase(status);
    }
}
