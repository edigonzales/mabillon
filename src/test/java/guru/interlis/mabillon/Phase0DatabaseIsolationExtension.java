package guru.interlis.mabillon;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Restores the imported Golden-Path state before every test method in
 * {@link Phase0CompatibilityTest}.
 *
 * <p>The expensive INTERLIS fixture import is performed only once by the test
 * class. A PostgreSQL dump of that pristine state is then used as the baseline
 * for each method. Persistent test filesystem state is cleared as well.</p>
 */
public final class Phase0DatabaseIsolationExtension implements BeforeEachCallback {

    private static final String SNAPSHOT = "/tmp/mabillon-phase0-baseline.sql";
    private static final Path ARCHIVE_ROOT = Path.of(System.getProperty("java.io.tmpdir"), "mabillon-sips")
            .toAbsolutePath().normalize();
    private static final Set<Class<?>> SNAPSHOT_CREATED = ConcurrentHashMap.newKeySet();

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();
        if (testClass != Phase0CompatibilityTest.class) {
            return;
        }

        PostgreSQLContainer<?> postgres = staticField(testClass, "POSTGRES", PostgreSQLContainer.class);
        Path storageRoot = staticField(testClass, "STORAGE_ROOT", Path.class);
        MabillonDatabaseBaseline baseline = new MabillonDatabaseBaseline(postgres, SNAPSHOT);

        if (SNAPSHOT_CREATED.add(testClass)) {
            baseline.createSnapshot();
        }
        baseline.restore();
        clearDirectory(storageRoot);
        clearDirectory(ARCHIVE_ROOT);
    }

    private static void clearDirectory(Path root) throws IOException {
        if (!Files.exists(root)) {
            Files.createDirectories(root);
            return;
        }
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(root))
                    .forEach(Phase0DatabaseIsolationExtension::delete);
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

    @SuppressWarnings("unchecked")
    private static <T> T staticField(Class<?> owner, String name, Class<T> type) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            Object value = field.get(null);
            if (!type.isInstance(value)) {
                throw new IllegalStateException("Unerwarteter Typ für Testfeld " + name);
            }
            return (T) value;
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Testfeld fehlt: " + owner.getName() + "." + name, failure);
        }
    }
}
