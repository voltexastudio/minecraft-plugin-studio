package com.voltexastudios.voltexa.migration;

import com.voltexastudios.voltexa.api.Migration;
import com.voltexastudios.voltexa.api.MigrationRecord;
import com.voltexastudios.voltexa.api.MigrationService;
import com.voltexastudios.voltexa.db.DefaultDatabaseService;
import com.voltexastudios.voltexa.log.DefaultLogService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Versioned SQL migration runner. Records applied migrations in the core's
 * {@code schema_migrations} table; a migration is applied at most once, runs
 * in its own transaction, and applies in version order regardless of input
 * order.
 */
public final class DefaultMigrationService implements MigrationService {

    private final DefaultDatabaseService database;
    private final DefaultLogService log;

    public DefaultMigrationService(DefaultDatabaseService database, DefaultLogService log) {
        this.database = database;
        this.log = log;
    }

    @Override
    public void applyMigrations(String owner, List<Migration> migrations) throws SQLException {
        database.withConnection(connection -> {
            ensureSchemaTable(connection);
            for (Migration migration : migrations.stream()
                    .sorted(Comparator.comparingInt(Migration::version))
                    .toList()) {
                if (isApplied(connection, owner, migration.version())) {
                    continue;
                }
                applyOne(connection, owner, migration);
            }
            return null;
        });
    }

    @Override
    public List<MigrationRecord> appliedMigrations() {
        try {
            return database.withConnection(connection -> {
                ensureSchemaTable(connection);
                List<MigrationRecord> result = new ArrayList<>();
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT version, owner, name, applied_at FROM schema_migrations ORDER BY owner, version")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Timestamp appliedAt = rs.getTimestamp(4);
                            result.add(new MigrationRecord(
                                    rs.getInt(1), rs.getString(2), rs.getString(3),
                                    appliedAt != null ? appliedAt.toInstant() : Instant.EPOCH));
                        }
                    }
                }
                return result;
            });
        } catch (SQLException e) {
            log.error("failed to list applied migrations: %s", e.getMessage());
            return List.of();
        }
    }

    private void ensureSchemaTable(Connection connection) throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS schema_migrations (
                        version INT NOT NULL,
                        owner VARCHAR(128) NOT NULL,
                        name VARCHAR(255) NOT NULL,
                        applied_at TIMESTAMP NOT NULL,
                        PRIMARY KEY (owner, version)
                    )
                    """);
        }
    }

    private boolean isApplied(Connection connection, String owner, int version) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM schema_migrations WHERE owner = ? AND version = ?")) {
            ps.setString(1, owner);
            ps.setInt(2, version);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void applyOne(Connection connection, String owner, Migration migration) throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            try (Statement st = connection.createStatement()) {
                st.execute(migration.sql());
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO schema_migrations (version, owner, name, applied_at) VALUES (?, ?, ?, ?)")) {
                ps.setInt(1, migration.version());
                ps.setString(2, owner);
                ps.setString(3, migration.name());
                ps.setTimestamp(4, Timestamp.from(Instant.now()));
                ps.executeUpdate();
            }
            connection.commit();
            log.info("applied migration %d (%s) for %s", migration.version(), migration.name(), owner);
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackError) {
                log.warn("rollback after failed migration %d also failed: %s",
                        migration.version(), rollbackError.getMessage());
            }
            log.error("migration %d (%s) for %s failed: %s",
                    migration.version(), migration.name(), owner, e.getMessage());
            throw e;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }
}
