package guru.interlis.mabillon.interlis;

import java.nio.file.Path;

/** Thin command-line entry point for repository scripts; uses the same in-process libraries as Mabillon. */
public final class InterlisToolCli {

    private InterlisToolCli() {
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            fail("Usage: InterlisToolCli <validate-model|validate-xtf|schema-import|import-xtf> [file]");
        }
        switch (args[0]) {
            case "validate-model" -> validateModel(args.length > 1 ? Path.of(args[1]) : InterlisToolDefaults.model());
            case "validate-xtf" -> requireArgs(args, 2, () -> validateXtf(Path.of(args[1])));
            case "schema-import" -> schemaImport();
            case "import-xtf" -> requireArgs(args, 2, () -> importXtf(Path.of(args[1])));
            default -> fail("Unbekannte INTERLIS-Operation: " + args[0]);
        }
    }

    private static void validateModel(Path model) {
        ValidationResult result = new JavaApiInterlisModelValidator().validate(model);
        if (!result.valid()) {
            fail(result.diagnostics());
        }
        System.out.println(result.diagnostics());
    }

    private static void validateXtf(Path xtf) {
        ValidationResult result = new JavaApiXtfValidator().validate(xtf);
        if (!result.valid()) {
            fail(result.diagnostics());
        }
        System.out.println(result.diagnostics());
    }

    private static void schemaImport() {
        validateModel(InterlisToolDefaults.model());
        Ili2pgResult result = runner().schemaImport(new SchemaImportRequest(InterlisToolDefaults.model(), true, true));
        require(result);
    }

    private static void importXtf(Path xtf) {
        validateXtf(xtf);
        Ili2pgResult result = runner().importXtf(new ImportXtfRequest(xtf, ImportScope.CATALOG, true, true));
        require(result);
    }

    private static JavaApiIli2pgRunner runner() {
        return new JavaApiIli2pgRunner(
                env("PGHOST", "localhost"),
                env("PGPORT", "5432"),
                env("PGDATABASE", "mabillon"),
                env("PGUSER", "mabillon"),
                env("PGPASSWORD", "mabillon"),
                "mabillon");
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void require(Ili2pgResult result) {
        if (!result.successful()) {
            fail(result.diagnostics());
        }
        System.out.println(result.diagnostics());
    }

    private static void requireArgs(String[] args, int count, Runnable action) {
        if (args.length != count) {
            fail("Falsche Anzahl Argumente.");
        }
        action.run();
    }

    private static void fail(String message) {
        throw new IllegalStateException(message);
    }
}
