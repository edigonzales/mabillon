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
        text = normalize(text);
        geschaeftsnummer = normalize(geschaeftsnummer);
        dossiernummer = normalize(dossiernummer);
        titel = normalize(titel);
        beteiligterName = normalize(beteiligterName);
        organisation = normalize(organisation);
        geschaeftsartCode = normalize(geschaeftsartCode);
        processStatusCode = normalize(processStatusCode);
        unterlagentitel = normalize(unterlagentitel);
        fachsystemId = normalize(fachsystemId);
    }

    public GlobalSearchCriteria(String text, int page, int size) {
        this(text, null, null, null, null, null, null, null, null, null, page, size);
    }

    public static GlobalSearchCriteria empty() {
        return new GlobalSearchCriteria(null, 0, 20);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
