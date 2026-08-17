package guru.interlis.mabillon.interlis;

public record Ili2pgResult(boolean successful, int exitCode, String diagnostics) {

    public Ili2pgResult {
        diagnostics = diagnostics == null ? "" : diagnostics;
    }
}
