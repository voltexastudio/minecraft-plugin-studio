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

    /**
     * Applies the versioned SQL migration files found in the {@code migrations}
     * classpath directory visible to {@code pluginClass}'s classloader
     * (typically the feature plugin's own classloader) under {@code owner}.
     * File names use the same zero-padded version prefix convention as core
     * migrations ({@code 0001_bank_accounts.sql} → version 1, name
     * {@code bank_accounts}); each file holds one statement.
     *
     * <p>Feature plugins call this in {@code onEnable} with their own main
     * class: the core reads the files from the plugin's jar and applies them
     * through the same runner as core migrations, so a feature never ships
     * its own file-loading code.
     *
     * @param owner the owning plugin (e.g. {@code bank}); applied migrations
     *              are recorded under this owner and never re-applied
     * @param pluginClass the feature plugin's main class; its classloader
     *                    must see the plugin's own {@code migrations}
     *                    resources
     * @throws SQLException if the files cannot be read or a migration fails
     */
    void applyMigrations(String owner, Class<?> pluginClass) throws SQLException;

    /** All applied migrations, ordered by owner then version. */
    List<MigrationRecord> appliedMigrations();
}
