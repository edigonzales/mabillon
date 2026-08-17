package guru.interlis.mabillon.unterlage;

import java.util.UUID;

import guru.interlis.mabillon.numbering.GeschaeftNumber;

public record AssignUnterlageCommand(UUID tid, GeschaeftNumber geschaeftNumber) {

    public AssignUnterlageCommand {
        if (tid == null || geschaeftNumber == null) {
            throw new IllegalArgumentException("Unterlage und Geschäft sind erforderlich.");
        }
    }
}
