package guru.interlis.mabillon.journal;

import java.time.Instant;
import java.util.Objects;

import guru.interlis.mabillon.security.ActorId;

public record JournalCommand(
        EreignisObjektTyp objektTyp,
        String objektId,
        EreignisTyp typ,
        String bemerkung,
        ActorId actorId,
        Instant timestamp) {

    public JournalCommand {
        Objects.requireNonNull(objektTyp, "objektTyp");
        Objects.requireNonNull(typ, "typ");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(timestamp, "timestamp");
        if (objektId == null || objektId.isBlank()) {
            throw new IllegalArgumentException("objektId darf nicht leer sein.");
        }
    }
}
