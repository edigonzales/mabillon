package guru.interlis.mabillon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Restores the imported Golden-Path state before every thematic integration test. */
public final class MabillonIntegrationDatabaseIsolationExtension implements BeforeEachCallback {

    private static final String SNAPSHOT = "/tmp/mabillon-integration-baseline.sql";
    private static final Path ARCHIVE_ROOT = Path.of(System.getProperty("java.io.tmpdir"), "mabillon-sips")
            .toAbsolutePath().normalize();
    private static final AtomicBoolean SNAPSHOT_CREATED = new AtomicBoolean();

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();
        if (!MabillonIntegrationTestSupport.class.isAssignableFrom(testClass)) {
            return;
        }

        MabillonDatabaseBaseline baseline = new MabillonDatabaseBaseline(
                MabillonIntegrationTestSupport.POSTGRES, SNAPSHOT);
        if (SNAPSHOT_CREATED.compareAndSet(false, true)) {
            baseline.createSnapshot();
        }
        baseline.restore();
        prepareFixture(context.getRequiredTestMethod().getName(), baseline);
        clearDirectory(MabillonIntegrationTestSupport.STORAGE_ROOT);
        clearDirectory(ARCHIVE_ROOT);
    }

    private static void prepareFixture(String testMethod, MabillonDatabaseBaseline baseline) throws Exception {
        if ("springBootAndJteRenderTheLocalTemplate".equals(testMethod)) {
            baseline.exec("psql", "-U", "mabillon", "-d", "mabillon", "-v", "ON_ERROR_STOP=1", "-c",
                    "UPDATE mabillon.geschaeft SET lifecyclestatus = 'In_Bearbeitung', "
                            + "prozessstatus = (SELECT t_id FROM mabillon.prozessstatus WHERE acode = 'FORMELLE_PRUEFUNG'), "
                            + "verantwortlicher = (SELECT t_id FROM mabillon.benutzer WHERE username = 'a.keller') "
                            + "WHERE geschaeftsnummer = 'AGI-G-2026-000421'");
        }
        if ("phaseFourControlViewProvidesOpenAndOverdueMetrics".equals(testMethod)) {
            baseline.exec("psql", "-U", "mabillon", "-d", "mabillon", "-v", "ON_ERROR_STOP=1", "-c",
                    "UPDATE mabillon.geschaeft SET lifecyclestatus = 'In_Bearbeitung', "
                            + "prozessstatus = (SELECT t_id FROM mabillon.prozessstatus WHERE acode = 'FORMELLE_PRUEFUNG') "
                            + "WHERE geschaeftsnummer = 'AGI-G-2026-000421'");
        }
        if ("phaseEightExportsValidatedCatalogWithStableTidAndBasket".equals(testMethod)) {
            baseline.exec("psql", "-U", "mabillon", "-d", "mabillon", "-v", "ON_ERROR_STOP=1", "-c",
                    "INSERT INTO mabillon.unterlagentyp (acode, aname, astatus, beschreibung, t_basket, t_ili_tid) "
                            + "SELECT 'PHASE8_EXPORT_PROBE', 'Phase 8 Export Probe', 'aktiv', "
                            + "'Explizites Fixture fuer den isolierten Exporttest.', t_basket, "
                            + "'8e36dfd6-0c9e-4c9b-b108-b072caf4a52a'::uuid FROM mabillon.unterlagentyp LIMIT 1");
        }
    }

    private static void clearDirectory(Path root) throws IOException {
        if (!Files.exists(root)) {
            Files.createDirectories(root);
            return;
        }
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).filter(path -> !path.equals(root))
                    .forEach(MabillonIntegrationDatabaseIsolationExtension::delete);
        }
        Files.createDirectories(root);
    }

    private static void delete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException failure) {
            throw new IllegalStateException("Persistenter Testzustand konnte nicht zurückgesetzt werden: " + path,
                    failure);
        }
    }
}
