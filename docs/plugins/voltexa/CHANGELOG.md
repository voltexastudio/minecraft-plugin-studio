# Voltexa — changelog

Format: one section per release, newest first. Entries grouped as
**Added** / **Changed** / **Fixed**. Maintained by Community & Content per
`docs/RELEASE.md`.

## 0.1.0 — 2026-11-14

**Added**

- Core Paper plugin `Voltexa` (package `com.voltexastudios`): owns the shared
  infrastructure for all VoltexaStudios feature plugins.
- Embedded H2 database (zero-config, file-based, shaded into the jar) behind a
  JDBC abstraction, with a fixed-size connection pool owned by the core.
- Versioned SQL migration runner (`schema_migrations` tracking table,
  transactional apply, idempotent re-runs) with the core baseline migration.
- Config facade (core `config.yml` + per-plugin config helpers) and the studio
  logging facade.
- Typed `VoltexaAPI` entry point + `voltexa-api` artifact (interfaces only),
  registered through the Bukkit Services API (versioned services).
- `/voltexa status` and `/voltexa version` commands.
