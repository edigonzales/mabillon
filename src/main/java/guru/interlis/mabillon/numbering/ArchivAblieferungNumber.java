package guru.interlis.mabillon.numbering;

import java.util.Objects;
import java.util.regex.Pattern;

public record ArchivAblieferungNumber(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*-A-\\d{4}-\\d{6}$");

    public ArchivAblieferungNumber {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Ungültige Ablieferungsnummer: " + value);
        }
    }

    public static ArchivAblieferungNumber parse(String value) {
        return new ArchivAblieferungNumber(value);
    }
}
