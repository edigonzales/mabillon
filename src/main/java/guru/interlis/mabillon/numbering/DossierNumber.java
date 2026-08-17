package guru.interlis.mabillon.numbering;

import java.util.Objects;
import java.util.regex.Pattern;

public record DossierNumber(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*-D-\\d{4}-\\d{6}$");

    public DossierNumber {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Ungültige Dossiernummer: " + value);
        }
    }

    public static DossierNumber parse(String value) {
        return new DossierNumber(value);
    }
}
