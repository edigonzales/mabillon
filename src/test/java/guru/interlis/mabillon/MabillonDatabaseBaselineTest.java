package guru.interlis.mabillon;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class MabillonDatabaseBaselineTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("sogis/postgis:16-3.5").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("mabillon")
            .withUsername("mabillon")
            .withPassword("mabillon");

    @Test
    void restoresMabillonSchemaAndResetsTechnicalNumberSequence() throws Exception {
        MabillonDatabaseBaseline baseline = new MabillonDatabaseBaseline(
                POSTGRES, "/tmp/mabillon-baseline-test.sql");

        baseline.exec(
                "psql", "-U", POSTGRES.getUsername(), "-d", POSTGRES.getDatabaseName(),
                "-v", "ON_ERROR_STOP=1",
                "-c", "CREATE SCHEMA mabillon; "
                        + "CREATE TABLE mabillon.probe (id integer PRIMARY KEY, value text NOT NULL); "
                        + "INSERT INTO mabillon.probe VALUES (1, 'baseline')");
        baseline.createSnapshot();

        baseline.exec(
                "psql", "-U", POSTGRES.getUsername(), "-d", POSTGRES.getDatabaseName(),
                "-v", "ON_ERROR_STOP=1",
                "-c", "UPDATE mabillon.probe SET value = 'mutated' WHERE id = 1; "
                        + "CREATE SCHEMA mabillon_app; "
                        + "CREATE TABLE mabillon_app.number_sequence (last_value bigint NOT NULL); "
                        + "INSERT INTO mabillon_app.number_sequence VALUES (99)");

        baseline.restore();

        String restoredValue = baseline.exec(
                "psql", "-U", POSTGRES.getUsername(), "-d", POSTGRES.getDatabaseName(),
                "-At", "-v", "ON_ERROR_STOP=1",
                "-c", "SELECT value FROM mabillon.probe WHERE id = 1").getStdout().trim();
        String sequenceRows = baseline.exec(
                "psql", "-U", POSTGRES.getUsername(), "-d", POSTGRES.getDatabaseName(),
                "-At", "-v", "ON_ERROR_STOP=1",
                "-c", "SELECT count(*) FROM mabillon_app.number_sequence").getStdout().trim();

        assertThat(restoredValue).isEqualTo("baseline");
        assertThat(sequenceRows).isEqualTo("0");
    }
}
