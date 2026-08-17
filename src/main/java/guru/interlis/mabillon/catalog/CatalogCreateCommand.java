package guru.interlis.mabillon.catalog;

import java.util.Objects;

public record CatalogCreateCommand(
        CatalogType type,
        String code,
        String name,
        String description,
        String geschaeftsartCode,
        Integer sortierung,
        boolean initial,
        boolean terminal,
        boolean resultatErforderlich) {

    public CatalogCreateCommand {
        Objects.requireNonNull(type, "type");
        requireText(code, "code");
        requireText(name, "name");
        if (type == CatalogType.PROZESSSTATUS || type == CatalogType.RESULTATSTATUS) {
            requireText(geschaeftsartCode, "geschaeftsartCode");
            Objects.requireNonNull(sortierung, "sortierung");
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
