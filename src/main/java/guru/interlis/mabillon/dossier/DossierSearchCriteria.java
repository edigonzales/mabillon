package guru.interlis.mabillon.dossier;

import java.time.LocalDate;

public record DossierSearchCriteria(
        String number,
        String title,
        String registraturplanPositionCode,
        String status,
        String federfuehrungKuerzel,
        LocalDate openedFrom,
        LocalDate openedTo,
        LocalDate closedFrom,
        LocalDate closedTo) {

    public DossierSearchCriteria(
            String number,
            String title,
            String registraturplanPositionCode,
            String status,
            String federfuehrungKuerzel,
            LocalDate openedFrom,
            LocalDate openedTo) {
        this(number, title, registraturplanPositionCode, status, federfuehrungKuerzel,
                openedFrom, openedTo, null, null);
    }

    public static DossierSearchCriteria empty() {
        return new DossierSearchCriteria(null, null, null, null, null, null, null, null, null);
    }
}
