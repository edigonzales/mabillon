package guru.interlis.mabillon.geschaeft;

import java.time.LocalDate;

public record GeschaeftskontrolleCriteria(LocalDate today, int limit, int inactiveSinceDays) {

    public GeschaeftskontrolleCriteria {
        if (today == null || limit < 1 || inactiveSinceDays < 0) {
            throw new IllegalArgumentException("Datum, limit und Inaktivitätsdauer sind ungültig.");
        }
    }
}
