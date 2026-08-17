package guru.interlis.mabillon.catalog;

public record CatalogEntryView(
        CatalogType type,
        String code,
        String name,
        String description,
        String status,
        String geschaeftsartCode,
        Integer sortierung,
        boolean initial,
        boolean terminal,
        boolean resultatErforderlich) {

    public boolean active() {
        return "aktiv".equalsIgnoreCase(status);
    }
}
