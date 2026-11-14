package com.voltexastudios.voltexa.api;

import java.sql.SQLException;
import java.util.List;

/**
 * Versioned SQL migrations. Each plugin owns its tables: the core applies its
 * own migrations at startup, and feature plugins apply theirs (typically in
 * {@code onEnable}, which runs after the core thanks to
 * {@code depend: [Voltexa]}). Applied migrations are recorded in the core's
 * {@code schema_migrations} table and never re-applied.
 */
public interface MigrationService {

    /**
     * Applies the given migrations for {@code owner} in version order,
     * skipping versions already recorded. Each migration runs in its own
     * transaction; a failed migration is rolled back and not recorded.
     *
     * @throws SQLException if a migration fails
     */
    void applyMigrations(String owner, List<Migration> migrations) throws SQLException;

    /** All applied migrations, ordered by owner then version. */
    List<MigrationRecord> appliedMigrations();
}
