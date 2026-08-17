package guru.interlis.mabillon.geschaeft;

import java.time.LocalDate;

public record GeschaeftSearchCriteria(
        String number,
        String title,
        String geschaeftsartCode,
        String processStatusCode,
        String lifecycleStatus,
        String verantwortlicherUsername,
        String organisationseinheitKuerzel,
        LocalDate dueFrom,
        LocalDate dueTo) {

    public static GeschaeftSearchCriteria empty() {
        return new GeschaeftSearchCriteria(null, null, null, null, null, null, null, null, null);
    }
}
