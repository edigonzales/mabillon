package guru.interlis.mabillon.interlis;

import java.util.List;

public record ValidationReport(boolean valid, int exitCode, String diagnostics) {

    public ValidationReport {
        diagnostics = diagnostics == null ? "" : diagnostics;
    }

    public static ValidationReport from(ValidationResult result) {
        return new ValidationReport(result.valid(), result.exitCode(), result.diagnostics());
    }

    public List<String> messages() {
        return diagnostics.isBlank() ? List.of() : List.of(diagnostics);
    }
}
