package guru.interlis.mabillon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class InterlisSchemaConstraintIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("sogis/postgis:16-3.5").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("mabillon")
            .withUsername("mabillon")
            .withPassword("mabillon");

    private static final List<Column> OPTIONAL_REFERENCES = List.of(
            new Column("dossier", "federfuehrung"),
            new Column("dossier", "verantwortlicher"),
            new Column("geschaeft", "prozessstatus"),
            new Column("geschaeft", "resultatstatus"),
            new Column("geschaeft", "federfuehrung"),
            new Column("geschaeft", "verantwortlicher"),
            new Column("unterlage", "registriertvon"),
            new Column("aufgabe", "zugewiesenanbenutzer"),
            new Column("aufgabe", "zugewiesenanorganisationseinheit"),
            new Column("archivablieferung", "erstelltvon"),
            new Column("sippaket", "erstelltvon"),
            new Column("ereignis", "benutzer"),
            new Column("organisationseinheit", "uebergeordneteeinheit"),
            new Column("ordnungssystemposition", "oberposition"),
            new Column("ordnungssystemposition", "federfuehrendeeinheit"),
            new Column("unterlage", "geschaeftskontext"),
            new Column("fachsystemreferenz", "referenziertesgeschaeft"),
            new Column("fachsystemreferenz", "referenziertesdossier"));

    @BeforeAll
    static void createSchemaAndImportFixtures() {
        InterlisTestFixture.importGoldenPath(POSTGRES);
    }

    @Test
    void normalMandatoryColumnsAndReferencesAreEnforcedWithoutCreateMandatoryChecks() throws Exception {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            assertThat(columnNullable(connection, "dossier", "dossiernummer")).isFalse();
            assertThat(columnNullable(connection, "geschaeft", "geschaeftsdossier")).isFalse();
            assertThat(columnNullable(connection, "unterlage", "ablagedossier")).isFalse();

            assertThatThrownBy(() -> execute(connection,
                    "UPDATE mabillon.dossier SET dossiernummer = NULL "
                            + "WHERE dossiernummer = 'AGI-D-2026-000007'"))
                    .isInstanceOf(SQLException.class);
            connection.rollback();

            assertThatThrownBy(() -> execute(connection,
                    "UPDATE mabillon.geschaeft SET geschaeftsdossier = NULL "
                            + "WHERE geschaeftsnummer = 'AGI-G-2026-000421'"))
                    .isInstanceOf(SQLException.class);
            connection.rollback();

            assertThatThrownBy(() -> execute(connection,
                    "UPDATE mabillon.unterlage SET ablagedossier = NULL "
                            + "WHERE t_id = (SELECT t_id FROM mabillon.unterlage LIMIT 1)"))
                    .isInstanceOf(SQLException.class);
            connection.rollback();
        }
    }

    @Test
    void formerlyRepairedOptionalReferencesRemainNullable() throws Exception {
        try (Connection connection = connection()) {
            for (Column column : OPTIONAL_REFERENCES) {
                assertThat(columnNullable(connection, column.table(), column.name()))
                        .as("%s.%s", column.table(), column.name())
                        .isTrue();
            }

            connection.setAutoCommit(false);
            assertThat(execute(connection,
                    "UPDATE mabillon.dossier SET federfuehrung = NULL, verantwortlicher = NULL "
                            + "WHERE dossiernummer = 'AGI-D-2026-000007'"))
                    .isEqualTo(1);
            connection.rollback();
        }
    }

    @Test
    void schemaContainsNoAdditionalMandatoryCheckConstraints() throws Exception {
        try (Connection connection = connection();
             var statement = connection.prepareStatement(
                     "SELECT c.conname, pg_get_constraintdef(c.oid) "
                             + "FROM pg_constraint c "
                             + "JOIN pg_namespace n ON n.oid = c.connamespace "
                             + "WHERE n.nspname = 'mabillon' AND c.contype = 'c'")) {
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    String definition = result.getString(2).toLowerCase(java.util.Locale.ROOT);
                    assertThat(definition)
                            .as("CHECK constraint %s", result.getString(1))
                            .doesNotContain(" is not null");
                }
            }
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static boolean columnNullable(Connection connection, String table, String column) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT is_nullable FROM information_schema.columns "
                        + "WHERE table_schema = 'mabillon' AND table_name = ? AND column_name = ?")) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (var result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalStateException("Spalte fehlt: mabillon." + table + "." + column);
                }
                return "YES".equals(result.getString(1));
            }
        }
    }

    private static int execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            return statement.executeUpdate(sql);
        }
    }

    private record Column(String table, String name) {
    }
}
