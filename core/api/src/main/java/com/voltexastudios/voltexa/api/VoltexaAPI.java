package com.voltexastudios.voltexa.api;

/**
 * Typed entry point to the Voltexa core, exposed to feature plugins through
 * the Bukkit Services API. A feature plugin resolves this once (typically in
 * {@code onEnable}) and uses the service accessors instead of touching
 * infrastructure directly.
 */
public interface VoltexaAPI {

    /** Core plugin version (semver), e.g. {@code 0.1.0}. */
    String version();

    /** Shared database access. The core owns the connection pool. */
    DatabaseService database();

    /** Core and per-plugin configuration access. */
    ConfigService config();

    /** One consistent logger for everything the studio ships. */
    LogService log();

    /** Versioned SQL migrations; each plugin owns its tables. */
    MigrationService migrations();
}
