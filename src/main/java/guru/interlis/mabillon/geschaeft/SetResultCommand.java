package guru.interlis.mabillon.geschaeft;

import guru.interlis.mabillon.numbering.GeschaeftNumber;

public record SetResultCommand(
        GeschaeftNumber number,
        String resultStatusCode,
        String comment) {

    public SetResultCommand {
        if (number == null || resultStatusCode == null || resultStatusCode.isBlank()) {
            throw new IllegalArgumentException("number und resultStatusCode dürfen nicht leer sein.");
        }
    }
}
