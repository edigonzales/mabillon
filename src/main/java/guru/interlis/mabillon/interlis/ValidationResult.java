package guru.interlis.mabillon.interlis;

import java.util.Objects;

public record ValidationResult(boolean valid, int exitCode, String diagnostics) {

    public ValidationResult {
        diagnostics = diagnostics == null ? "" : diagnostics;
    }

    public static ValidationResult valid(String diagnostics) {
        return new ValidationResult(true, 0, diagnostics);
    }

    public static ValidationResult invalid(int exitCode, String diagnostics) {
        return new ValidationResult(false, exitCode, diagnostics);
    }
}
