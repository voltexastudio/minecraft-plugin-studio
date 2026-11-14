package com.voltexastudios.voltexa.api;

/**
 * A single versioned SQL migration: an ordered {@code version}, a short
 * {@code name}, and the {@code sql} to apply. A migration file holds one
 * statement; use consecutive versions for multi-statement changes.
 */
public record Migration(int version, String name, String sql) {

    public Migration {
        if (version <= 0) {
            throw new IllegalArgumentException("migration version must be positive");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("migration name is required");
        }
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("migration sql is required");
        }
    }
}
