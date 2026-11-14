# Voltexa — changelog

Format: one section per release, newest first. Entries grouped as
**Added** / **Changed** / **Fixed**. Maintained by Community & Content per
`docs/RELEASE.md`.

## 2026.46 — 2026-11-14

**Added**

- First release of the Voltexa core plugin under the `year.week.fix`
  scheme (the `0.1.0` entry below is the same codebase, recorded before
  the scheme switch — its Added list is the feature content of this
  release).

**Changed**

- Version `0.1.0` → `2026.46` (the `year.week.fix` scheme, plan decision 8;
  resolves QA defect D1 recorded on [VOL-17](/VOL/issues/VOL-17)): the
  server now loads `Voltexa v2026.46`, `/voltexa version` reports
  `Voltexa 2026.46`, and feature plugins resolve `VoltexaAPI 2026.46`.
  The artifact stays `voltexa.jar` (the version lives in `plugin.yml`, not
  the file name).
- `docs/RELEASE.md` versioning section updated from semver to
  `year.week.fix` to match the studio directive.

**Fixed**

- — none —

## 2026.46.1 — 2026-11-14

**Changed**

- `voltexa.jar` now bundles the `voltexa-api` contract (shaded in
  alongside H2), so the plugin is self-contained: it works with the
  server's normal launch — **no JVM flags, no classpath changes**.
  Feature plugins resolve the core's services through their classloader
  bridges (Bank: `VoltexaBridge`), because Bukkit isolates plugin
  classloaders and the `ServicesManager` matches registrations by
  `Class` identity.
- Deployment docs rewritten: the `--patch-module java.base=
  voltexa-api.jar` / `--add-exports` launcher flags are no longer needed
  (see [README.md](README.md#deployment)).

**Fixed**

- `NoClassDefFoundError` (`MigrationService` / `VoltexaAPI`) on Paper
  26.3 servers started with the normal launch: the contract was
  previously only available via a separate `voltexa-api.jar` on the
  server classpath. [VOL-20](/VOL/issues/VOL-20)

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
