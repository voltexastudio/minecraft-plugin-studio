package com.voltexastudios.voltexa.api;

import java.time.Instant;

/** An applied migration as recorded in the core's {@code schema_migrations} table. */
public record MigrationRecord(int version, String owner, String name, Instant appliedAt) {
}
