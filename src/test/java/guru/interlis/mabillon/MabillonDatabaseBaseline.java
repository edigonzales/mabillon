package guru.interlis.mabillon;

import org.testcontainers.containers.Container;
import org.testcontainers.containers.PostgreSQLContainer;

final class MabillonDatabaseBaseline {

    private final PostgreSQLContainer<?> postgres;
    private final String snapshot;

    MabillonDatabaseBaseline(PostgreSQLContainer<?> postgres, String snapshot) {
        this.postgres = postgres;
        this.snapshot = snapshot;
    }

    void createSnapshot() throws Exception {
        exec(
                "pg_dump",
                "-U", postgres.getUsername(),
                "-d", postgres.getDatabaseName(),
                "--schema=mabillon",
                "--no-owner",
                "--no-privileges",
                "--file=" + snapshot);
    }

    void restore() throws Exception {
        exec(
                "psql",
                "-U", postgres.getUsername(),
                "-d", postgres.getDatabaseName(),
                "-v", "ON_ERROR_STOP=1",
                "-c", "DROP SCHEMA IF EXISTS mabillon CASCADE");
        exec(
                "psql",
                "-U", postgres.getUsername(),
                "-d", postgres.getDatabaseName(),
                "-v", "ON_ERROR_STOP=1",
                "-f", snapshot);
        exec(
                "psql",
                "-U", postgres.getUsername(),
                "-d", postgres.getDatabaseName(),
                "-v", "ON_ERROR_STOP=1",
                "-c", "DO $reset$ BEGIN "
                        + "IF to_regclass('mabillon_app.number_sequence') IS NOT NULL THEN "
                        + "TRUNCATE TABLE mabillon_app.number_sequence; "
                        + "END IF; END $reset$");
    }

    Container.ExecResult exec(String... command) throws Exception {
        Container.ExecResult result = postgres.execInContainer(command);
        if (result.getExitCode() != 0) {
            throw new IllegalStateException(
                    "PostgreSQL test baseline command failed (" + result.getExitCode() + "): "
                            + String.join(" ", command) + "\n" + result.getStderr() + result.getStdout());
        }
        return result;
    }
}
