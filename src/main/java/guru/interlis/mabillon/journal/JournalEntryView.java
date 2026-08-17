package guru.interlis.mabillon.journal;

import java.time.LocalDateTime;

public record JournalEntryView(
        EreignisObjektTyp objektTyp,
        String objektId,
        EreignisTyp typ,
        LocalDateTime zeitpunkt,
        String username,
        String bemerkung) {
}
