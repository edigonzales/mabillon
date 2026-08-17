package guru.interlis.mabillon.catalog;

public enum CatalogType {
    GESCHAEFTSART("Geschäftsarten"),
    PROZESSSTATUS("Prozessstatus"),
    RESULTATSTATUS("Resultatstatus"),
    BETEILIGUNGSROLLE("Beteiligungsrollen"),
    UNTERLAGENTYP("Unterlagentypen"),
    AUFGABENTYP("Aufgabentypen");

    private final String label;

    CatalogType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
