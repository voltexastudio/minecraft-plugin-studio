# Voltexa

Voltexa core plugin. Purpose: own the shared infrastructure every
VoltexaStudios feature plugin builds on, so features ship domain logic only.

## What the core owns

- **Database** — embedded H2 (zero-config, file-based) behind a JDBC
  abstraction; the core owns the connection pool. MySQL/PostgreSQL backends
  can be swapped in later by implementing the provider interface.
- **Migrations** — versioned SQL migration runner; each plugin owns its
  tables. Applied migrations are recorded in `schema_migrations` and never
  re-applied. Feature plugins apply their own migration files with
  `MigrationService.applyMigrations(owner, pluginClass)` — the core reads
  the files from the plugin's own classloader, so a feature never ships
  file-loading code.
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

Feature plugins (first: `Bank`) declare `depend: [Voltexa]` in `plugin.yml`
and bundle the `voltexa-api` contract (interfaces only) into their own jar.
They resolve the core's services through a classloader bridge (Bank:
`VoltexaBridge`) that looks the service up through the core plugin's own
classloader — see [Deployment](#deployment). No feature ships duplicate
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

The plugin jars are **self-contained**: each bundles the `voltexa-api`
contract, so there are **no JVM flags and no classpath changes** — drop
the jars in `plugins/` and start the server normally:

```bash
java -jar paper-<version>.jar
```

- `voltexa.jar` — the core (H2 and the contract are shaded in).
- Feature plugin jars (e.g. `bank.jar`) — each bundles its own copy of the
  contract.

### How feature plugins resolve the core's API

Bukkit isolates each plugin's classloader (parent-first), so a feature's
copy of the contract is a different `Class` object from the core's, and
the Bukkit `ServicesManager` matches registrations by `Class` identity. A
feature therefore cannot look the core's service up with its own copy of
`VoltexaAPI`. Instead, each feature resolves the service **through the
core plugin's own classloader** (Bank: `VoltexaBridge`), which yields the
same `Class` object the core registered, and wraps the result in a dynamic
proxy that implements the feature's copy of the contract. Contract types
that cross the boundary are translated: functional arguments are wrapped
for the core, and record data carriers coming back are re-materialized as
the feature's copies. The feature's typed code then works unchanged.

The former `--patch-module java.base=voltexa-api.jar` / `--add-exports`
launcher flags are **no longer needed** and were removed in `2026.46.1`.

## Test plan

See [TESTPLAN.md](TESTPLAN.md) — two stages: unit (no server) and server
smoke test on the QA test server running the `baseline` profile.

## Changelog

See [CHANGELOG.md](CHANGELOG.md).
