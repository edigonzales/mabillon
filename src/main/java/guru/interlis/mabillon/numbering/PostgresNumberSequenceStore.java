package guru.interlis.mabillon.numbering;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.cayenne.runtime.CayenneRuntime;
import org.springframework.stereotype.Component;

@Component
public final class PostgresNumberSequenceStore implements NumberSequenceStore {

    private static final Object INITIALIZATION_LOCK = new Object();
    private final CayenneRuntime runtime;
    private volatile boolean initialized;

    public PostgresNumberSequenceStore(CayenneRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public long next(String organisationCode, NumberObjectType type, int year) {
        ensureTable();
        try (Connection connection = runtime.getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO mabillon_app.number_sequence "
                                + "(organisation_code, object_type, year, last_value) VALUES (?, ?, ?, 0) "
                                + "ON CONFLICT (organisation_code, object_type, year) DO NOTHING")) {
                    insert.setString(1, organisationCode);
                    insert.setString(2, type.name());
                    insert.setInt(3, year);
                    insert.executeUpdate();
                }
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE mabillon_app.number_sequence SET last_value = last_value + 1 "
                                + "WHERE organisation_code = ? AND object_type = ? AND year = ? "
                                + "RETURNING last_value")) {
                    update.setString(1, organisationCode);
                    update.setString(2, type.name());
                    update.setInt(3, year);
                    try (ResultSet result = update.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalStateException("Nummersequenz konnte nicht inkrementiert werden.");
                        }
                        long value = result.getLong(1);
                        connection.commit();
                        return value;
                    }
                }
            } catch (RuntimeException | SQLException failure) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                if (failure instanceof RuntimeException runtimeFailure) {
                    throw runtimeFailure;
                }
                throw new IllegalStateException("Nummersequenz konnte nicht fortgeschrieben werden.", failure);
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("Nummersequenz konnte nicht geöffnet werden.", failure);
        }
    }

    private void ensureTable() {
        if (initialized) {
            return;
        }
        synchronized (INITIALIZATION_LOCK) {
            if (initialized) {
                return;
            }
            try (Connection connection = runtime.getDataSource().getConnection()) {
                try (var statement = connection.createStatement()) {
                    statement.executeUpdate("CREATE SCHEMA IF NOT EXISTS mabillon_app");
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS mabillon_app.number_sequence ("
                            + "organisation_code varchar(80) NOT NULL, "
                            + "object_type varchar(40) NOT NULL, "
                            + "year integer NOT NULL, "
                            + "last_value bigint NOT NULL, "
                            + "PRIMARY KEY (organisation_code, object_type, year))");
                }
                initialized = true;
            } catch (SQLException failure) {
                throw new IllegalStateException("Technische Nummernsequenz konnte nicht initialisiert werden.", failure);
            }
        }
    }
}
