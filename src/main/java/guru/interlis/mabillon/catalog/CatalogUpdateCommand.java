package guru.interlis.mabillon.catalog;

public record CatalogUpdateCommand(
        CatalogType type,
        String code,
        String name,
        String description,
        String geschaeftsartCode,
        Integer sortierung,
        boolean initial,
        boolean terminal,
        boolean resultatErforderlich) {

    public CatalogUpdateCommand {
        if (type == null || code == null || code.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Katalogtyp, Code und Name sind erforderlich.");
        }
    }
}
