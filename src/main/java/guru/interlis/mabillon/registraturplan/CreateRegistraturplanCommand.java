package guru.interlis.mabillon.registraturplan;

import java.time.LocalDate;

public record CreateRegistraturplanCommand(
        String code,
        String name,
        LocalDate gueltigVon,
        String organisationseinheit) {

    public CreateRegistraturplanCommand {
        requireText(code, "code");
        requireText(name, "name");
        if (gueltigVon == null) {
            throw new IllegalArgumentException("gueltigVon must not be null");
        }
        requireText(organisationseinheit, "organisationseinheit");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
