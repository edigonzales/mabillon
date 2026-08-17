package guru.interlis.mabillon.interlis;

import java.nio.file.Path;
import java.util.List;

public record ExchangeResult(
        ImportScope scope,
        Path xtf,
        ValidationReport validation,
        Ili2pgResult importResult,
        boolean successful,
        List<String> messages) {

    public ExchangeResult {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }

    public static ExchangeResult rejected(ImportScope scope, Path xtf, ValidationReport validation) {
        return new ExchangeResult(scope, xtf, validation, null, false,
                List.of("XTF-Import abgebrochen: Die Datei ist nicht valide."));
    }

    public static ExchangeResult imported(
            ImportScope scope, Path xtf, ValidationReport validation, Ili2pgResult result) {
        String message = result.successful()
                ? scope.label() + " erfolgreich importiert."
                : scope.label() + " konnten nicht importiert werden. Prüfe Importreihenfolge und Referenzen.";
        return new ExchangeResult(scope, xtf, validation, result, result.successful(), List.of(message));
    }
}
