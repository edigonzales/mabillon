import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

/**
 * Removes the false mandatory checks that ili2pg 5.5.2 emits for optional
 * INTERLIS reference attributes and 0..1 association roles.
 *
 * The model remains the source of truth. This is a deterministic repair of a
 * known ili2pg 5.5.2 schema-import limitation; mandatory checks created by
 * ili2pg are intentionally left untouched.
 */
public final class SchemaConstraintRepair {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private static final String[][] OPTIONAL_REFERENCE_CONSTRAINTS = {
        {"dossier", "ili_federfuehrung1"},
        {"dossier", "ili_verantwortlicher1"},
        {"geschaeft", "ili_prozessstatus1"},
        {"geschaeft", "ili_resultatstatus1"},
        {"geschaeft", "ili_federfuehrung1"},
        {"geschaeft", "ili_verantwortlicher1"},
        {"unterlage", "ili_registriertvon1"},
        {"aufgabe", "ili_zugewiesenanbenutzer1"},
        {"aufgabe", "ili_zugewiesenanorganisationseinheit1"},
        {"archivablieferung", "ili_erstelltvon1"},
        {"sippaket", "ili_erstelltvon1"},
        {"ereignis", "ili_benutzer1"},
        {"organisationseinheit", "ili_uebergeordneteeinheit"},
        {"ordnungssystemposition", "ili_oberposition"},
        {"ordnungssystemposition", "ili_federfuehrendeeinheit"},
        {"unterlage", "ili_geschaeftskontext"},
        {"fachsystemreferenz", "ili_referenziertesgeschaeft"},
        {"fachsystemreferenz", "ili_referenziertesdossier"}
    };

    private SchemaConstraintRepair() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 6) {
            throw new IllegalArgumentException(
                "Usage: SchemaConstraintRepair <host> <port> <database> <user> <password> <schema>");
        }

        String host = args[0];
        String port = args[1];
        String database = args[2];
        String user = args[3];
        String password = args[4];
        String schema = checkedIdentifier(args[5]);

        Class.forName("org.postgresql.Driver");
        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database;
        try (Connection connection = DriverManager.getConnection(jdbcUrl, user, password)) {
            connection.setAutoCommit(false);
            for (String[] constraint : OPTIONAL_REFERENCE_CONSTRAINTS) {
                dropConstraint(connection, schema, constraint[0], constraint[1]);
            }
            connection.commit();
        }

        System.out.println("Applied optional-reference constraint repair to schema " + schema + ".");
    }

    private static void dropConstraint(Connection connection, String schema, String table, String constraint)
        throws SQLException {
        String sql = "ALTER TABLE " + quoted(schema) + "." + quoted(checkedIdentifier(table))
            + " DROP CONSTRAINT IF EXISTS " + quoted(checkedIdentifier(constraint));
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String checkedIdentifier(String identifier) {
        if (!IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException("Invalid SQL identifier");
        }
        return identifier;
    }

    private static String quoted(String identifier) {
        return "\"" + identifier + "\"";
    }
}
