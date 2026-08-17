package guru.interlis.mabillon.numbering;

import java.util.Objects;
import java.util.regex.Pattern;

public record GeschaeftNumber(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*-G-\\d{4}-\\d{6}$");

    public GeschaeftNumber {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Ungültige Geschäftsnummer: " + value);
        }
    }

    public static GeschaeftNumber parse(String value) {
        return new GeschaeftNumber(value);
    }
}
