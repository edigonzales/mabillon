package guru.interlis.mabillon.numbering;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

@Service
public final class NumberingService {

    private final NumberSequenceStore sequenceStore;

    public NumberingService(NumberSequenceStore sequenceStore) {
        this.sequenceStore = sequenceStore;
    }

    public DossierNumber nextDossierNumber(String organisationCode, LocalDate date) {
        return new DossierNumber(next(organisationCode, NumberObjectType.DOSSIER, date));
    }

    public GeschaeftNumber nextGeschaeftNumber(String organisationCode, LocalDate date) {
        return new GeschaeftNumber(next(organisationCode, NumberObjectType.GESCHAEFT, date));
    }

    public ArchivAblieferungNumber nextArchivAblieferungNumber(String organisationCode, LocalDate date) {
        return new ArchivAblieferungNumber(next(organisationCode, NumberObjectType.ARCHIVABLIEFERUNG, date));
    }

    private String next(String organisationCode, NumberObjectType type, LocalDate date) {
        if (organisationCode == null || organisationCode.isBlank()) {
            throw new IllegalArgumentException("Organisationscode darf nicht leer sein.");
        }
        if (date == null) {
            throw new IllegalArgumentException("Datum darf nicht leer sein.");
        }
        String normalized = organisationCode.trim().toUpperCase(java.util.Locale.ROOT);
        long value = sequenceStore.next(normalized, type, date.getYear());
        if (value > 999999) {
            throw new IllegalStateException("Nummernsequenz überschreitet sechs Stellen.");
        }
        return "%s-%s-%04d-%06d".formatted(normalized, type.code(), date.getYear(), value);
    }
}
