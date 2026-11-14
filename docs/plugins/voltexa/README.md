# Voltexa

Voltexa core plugin. Purpose: own the shared infrastructure every
VoltexaStudios feature plugin builds on, so features ship domain logic only.

## What the core owns

- **Database** — embedded H2 (zero-config, file-based) behind a JDBC
  abstraction; the core owns the connection pool. MySQL/PostgreSQL backends
  can be swapped in later by implementing the provider interface.
- **Migrations** — versioned SQL migration runner; each plugin owns its tables.
  Applied migrations are recorded in `schema_migrations` and never re-applied.
- **Config** — core `config.yml` + per-plugin config loading helpers.
- **Logging** — one consistent logger for all studio output.
- **Typed API** — `VoltexaAPI` entry point (plus `DatabaseService`,
  `ConfigService`, `LogService`, `MigrationService`), exposed through the
  Bukkit Services API as versioned services.

## Commands

- `/voltexa status` — core health: database product, pool usage, applied
  migrations, config keys.
- `/voltexa version` — core version.

## Feature plugins

Feature plugins (first: `Bank`) declare `depend: [Voltexa]` in `plugin.yml`,
compile against the `voltexa-api` artifact (interfaces only), and resolve the
core's services through the Bukkit Services API. No feature ships duplicate
shared code.

## Build

```bash
./gradlew :core:impl:build        # from the repo root
```

- Unit tests run as part of the build.
- Artifacts:
  - `core/impl/build/libs/voltexa.jar` — the plugin (H2 is shaded in).
  - `core/api/build/libs/voltexa-api.jar` — the typed service contracts.

## Deployment

Bukkit plugin classloaders are isolated (parent-last), and the Bukkit
`ServicesManager` matches registrations by `Class` identity. For feature
plugins to resolve the core's typed API, **all plugins must see the same
`VoltexaAPI` class** — so `voltexa-api.jar` goes on the **server
classpath**, not into `plugins/`:

```bash
java --patch-module java.base=<path-to>/voltexa-api.jar \
     --add-exports java.base/com.voltexastudios.voltexa.api=ALL-UNNAMED \
     -jar paper-<version>.jar
```

Then `voltexa.jar` (and feature plugin jars) go in `plugins/`. Paper's
modular launcher builds its classpath from a fixed manifest list, so a jar
dropped in `libraries/` is **not** picked up, and the launcher's
classloader skips the system classpath; `--patch-module java.base=` puts
the contract on the platform classpath, which every plugin classloader
delegates to (JDK 25 removed `-Xbootclasspath/a`). `--add-exports` is
required because a patched package is not exported from `java.base` to the
unnamed plugin modules — without it, plugin classes fail the
superinterface check with `IllegalAccessError`.

## Test plan

See [TESTPLAN.md](TESTPLAN.md) — two stages: unit (no server) and server
smoke test on the QA test server running the `baseline` profile.

## Changelog

See [CHANGELOG.md](CHANGELOG.md).
