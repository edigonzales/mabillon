package guru.interlis.mabillon.search;

public record GlobalSearchCriteria(
        String text,
        String geschaeftsnummer,
        String dossiernummer,
        String titel,
        String beteiligterName,
        String organisation,
        String geschaeftsartCode,
        String processStatusCode,
        String unterlagentitel,
        String fachsystemId,
        int page,
        int size) {

    public GlobalSearchCriteria {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("Ungültige Seitendaten.");
        }
    }

    public GlobalSearchCriteria(String text, int page, int size) {
        this(text, null, null, null, null, null, null, null, null, null, page, size);
    }

    public static GlobalSearchCriteria empty() {
        return new GlobalSearchCriteria(null, 0, 20);
    }
}
