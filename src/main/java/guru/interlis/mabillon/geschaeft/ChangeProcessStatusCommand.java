package guru.interlis.mabillon.geschaeft;

import guru.interlis.mabillon.numbering.GeschaeftNumber;

public record ChangeProcessStatusCommand(
        GeschaeftNumber number,
        String processStatusCode,
        String comment) {

    public ChangeProcessStatusCommand {
        if (number == null || processStatusCode == null || processStatusCode.isBlank()) {
            throw new IllegalArgumentException("number und processStatusCode dürfen nicht leer sein.");
        }
    }
}
