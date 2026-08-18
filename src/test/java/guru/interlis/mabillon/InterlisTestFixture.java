package guru.interlis.mabillon;

import java.nio.file.Path;

import guru.interlis.mabillon.interlis.Ili2pgResult;
import guru.interlis.mabillon.interlis.ImportScope;
import guru.interlis.mabillon.interlis.ImportXtfRequest;
import guru.interlis.mabillon.interlis.InterlisToolDefaults;
import guru.interlis.mabillon.interlis.JavaApiIli2pgRunner;
import guru.interlis.mabillon.interlis.JavaApiInterlisModelValidator;
import guru.interlis.mabillon.interlis.JavaApiXtfValidator;
import guru.interlis.mabillon.interlis.SchemaImportRequest;
import guru.interlis.mabillon.interlis.ValidationResult;
import org.testcontainers.containers.PostgreSQLContainer;

final class InterlisTestFixture {

    private InterlisTestFixture() {
    }

    static void importGoldenPath(PostgreSQLContainer<?> postgres) {
        ValidationResult model = new JavaApiInterlisModelValidator().validate(InterlisToolDefaults.model());
        require(model.valid(), "Modellvalidierung", model.diagnostics());

        JavaApiIli2pgRunner runner = runner(postgres);
        require(runner.schemaImport(new SchemaImportRequest(InterlisToolDefaults.model(), true, true)), "Schemaimport");
        importXtf(runner, Path.of("model/testdata/01_SO_AGI_GEVER_20260707_testdaten_kataloge.xtf"), ImportScope.CATALOG);
        importXtf(runner, Path.of("model/testdata/02_SO_AGI_GEVER_20260707_testdaten_stammdaten.xtf"), ImportScope.MASTER_DATA);
        importXtf(runner, Path.of("model/testdata/03_SO_AGI_GEVER_20260707_testdaten_geschaeftsdaten.xtf"), ImportScope.BUSINESS_DATA);
    }

    static JavaApiIli2pgRunner runner(PostgreSQLContainer<?> postgres) {
        return new JavaApiIli2pgRunner(
                postgres.getHost(),
                Integer.toString(postgres.getMappedPort(5432)),
                postgres.getDatabaseName(),
                postgres.getUsername(),
                postgres.getPassword(),
                "mabillon");
    }

    private static void importXtf(JavaApiIli2pgRunner runner, Path path, ImportScope scope) {
        ValidationResult validation = new JavaApiXtfValidator().validate(path);
        require(validation.valid(), "XTF-Validierung " + path, validation.diagnostics());
        require(runner.importXtf(new ImportXtfRequest(path, scope, true, true)), "XTF-Import " + path);
    }

    private static void require(Ili2pgResult result, String operation) {
        require(result.successful(), operation, result.diagnostics());
    }

    private static void require(boolean successful, String operation, String diagnostics) {
        if (!successful) {
            throw new IllegalStateException(operation + " fehlgeschlagen: " + diagnostics);
        }
    }
}
