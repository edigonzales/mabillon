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
import org.testcontainers.containers.Container;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Restores the imported Golden-Path database before every test method in
 * {@link Phase0CompatibilityTest}.
 *
 * <p>The expensive INTERLIS fixture import is performed only once by the test
 * class. A PostgreSQL dump of that pristine state is then used as the baseline
 * for each method. This keeps the existing shared container without sharing
 * mutations between tests.</p>
 */
public final class Phase0DatabaseIsolationExtension implements BeforeEachCallback {

    private static final String SNAPSHOT = "/tmp/mabillon-phase0-baseline.sql";
    private static final Set<Class<?>> SNAPSHOT_CREATED = ConcurrentHashMap.newKeySet();

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();
        if (testClass != Phase0CompatibilityTest.class) {
            return;
        }

        PostgreSQLContainer<?> postgres = staticField(testClass, "POSTGRES", PostgreSQLContainer.class);
        Path storageRoot = staticField(testClass, "STORAGE_ROOT", Path.class);

        if (SNAPSHOT_CREATED.add(testClass)) {
            createSnapshot(postgres);
        }
        restoreSnapshot(postgres);
        clearStorage(storageRoot);
    }

    private static void createSnapshot(PostgreSQLContainer<?> postgres) throws Exception {
        exec(postgres,
                "pg_dump",
                "-U", postgres.getUsername(),
                "-d", postgres.getDatabaseName(),
                "--schema=mabillon",
                "--no-owner",
                "--no-privileges",
                "--file=" + SNAPSHOT);
    }

    private static void restoreSnapshot(PostgreSQLContainer<?> postgres) throws Exception {
        exec(postgres,
                "psql",
                "-U", postgres.getUsername(),
                "-d", postgres.getDatabaseName(),
                "-v", "ON_ERROR_STOP=1",
                "-c", "DROP SCHEMA IF EXISTS mabillon CASCADE");
        exec(postgres,
                "psql",
                "-U", postgres.getUsername(),
                "-d", postgres.getDatabaseName(),
                "-v", "ON_ERROR_STOP=1",
                "-f", SNAPSHOT);
        exec(postgres,
                "psql",
                "-U", postgres.getUsername(),
                "-d", postgres.getDatabaseName(),
                "-v", "ON_ERROR_STOP=1",
                "-c", "DO $reset$ BEGIN "
                        + "IF to_regclass('mabillon_app.number_sequence') IS NOT NULL THEN "
                        + "TRUNCATE TABLE mabillon_app.number_sequence; "
                        + "END IF; END $reset$");
    }

    private static void clearStorage(Path root) throws IOException {
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

    private static void exec(PostgreSQLContainer<?> postgres, String... command) throws Exception {
        Container.ExecResult result = postgres.execInContainer(command);
        if (result.getExitCode() != 0) {
            throw new IllegalStateException(
                    "PostgreSQL test baseline command failed (" + result.getExitCode() + "): "
                            + String.join(" ", command) + "\n" + result.getStderr() + result.getStdout());
        }
    }

    private static void delete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException failure) {
            throw new IllegalStateException("Test-Storage konnte nicht zurückgesetzt werden: " + path, failure);
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
