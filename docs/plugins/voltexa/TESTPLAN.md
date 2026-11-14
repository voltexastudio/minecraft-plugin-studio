# Voltexa — test plan

Every change to this plugin runs the stages below. Stage 1 must pass before
stage 2; a failed stage 2 is a bug report, not a retry of the same build.

## Stage 1 — unit smoke test (no server)

```bash
./gradlew :core:impl:test        # from the repo root
```

Pass criteria:

- [ ] `VoltexaSmokeTest` passes (4 tests):
  - `plugin.yml` has the expected name/version/api-version
  - `plugin.yml` `main` matches `VoltexaPlugin`
  - the main class extends `JavaPlugin`
  - the `/voltexa` command is declared in `plugin.yml`
- [ ] `DefaultMigrationServiceTest` passes (H2 in-memory):
  - baseline migration applies and is recorded in `schema_migrations`
  - re-running is idempotent (no re-apply)
  - a new version applies on top of the recorded ones
  - input order does not change apply order (version order)
  - a failed migration is rolled back and not recorded
  - the same version under different owners is tracked separately
  - the `applyMigrations(owner, pluginClass)` overload applies a "plugin's"
    own migration files (compiled marker class + `migrations/` directory on
    a `URLClassLoader`), records them under the owner, and is idempotent
    on re-apply
- [ ] `ConnectionPoolTest` passes: borrow/release reuse, exhaustion timeout,
  slot freed on release, closed pool rejects borrows
- [ ] `H2FileProviderTest` passes: the H2 database file is created on first use
- [ ] `MigrationFilesTest` passes: baseline listed from the class directory,
  baseline SQL readable, jar branch skips foreign/short entries and nested
  files (regression for the `substring` before the prefix guard),
  classloader-parameterized listing and `loadMigrations` parse a foreign
  plugin's files (version prefix, name without the separator underscore,
  SQL) and an empty directory yields no migrations

## Stage 2 — server smoke test (QA test server)

Preconditions:

- Paper **26.3-251** test server (`qa-server/`)
- `baseline` config profile deployed (see `configs/baseline/README.md`)
- `voltexa-api.jar` on the server platform classpath
  (`--patch-module java.base=<abs-path>/voltexa-api.jar`
  `--add-exports java.base/com.voltexastudios.voltexa.api=ALL-UNNAMED`)
- `voltexa.jar` in `plugins/`

Steps and pass criteria:

1. **Clean startup** — start the server:
   `java --patch-module java.base=<abs-path>/voltexa-api.jar \
     --add-exports java.base/com.voltexastudios.voltexa.api=ALL-UNNAMED \
     -jar paper-26.3-251.jar`
   - [ ] Log contains `Voltexa 2026.46 ready` with the applied migration count
   - [ ] Log contains the applied-migration line for the core baseline
   - [ ] No plugin-related errors or stack traces
2. **Database + migration evidence**
   - [ ] `plugins/Voltexa/voltexa.mv.db` exists after first load
   - [ ] `/voltexa status` reports the applied migration count
3. **Status command** — run `/voltexa status` from the console.
   - [ ] Output shows database product, pool size, applied migrations, config keys
4. **Version command** — run `/voltexa version`.
   - [ ] Output is `Voltexa 2026.46`
5. **Disable path** — stop the server. (Paper 26.3 removed the `/plugin`
   command, so the disable path is the server shutdown; the core's
   `onDisable` must log `Voltexa disabled.` and close the pool cleanly.)
   - [ ] Log contains `Voltexa disabled.` and the server stays stable
6. **Re-enable** — start the server again.
   - [ ] Core re-enables cleanly (ready line), the H2 file is reused
     (no second, double-suffixed file appears), migrations are not
     re-applied (ready line still reports the same applied count),
     `/voltexa status` works again

Record the server log (enable line, command exchange, disable line) as evidence
on the QA task.

## Regression notes

- Bumping the Paper version: re-run both stages; the `paper-api` pin in the
  root `build.gradle` and the `baseline` profile target version must change
  together.
- Paper 26.3 removed the `/plugin` command and closes a plugin's
  classloader on disable: mid-session disable/re-enable is unavailable, and
  the core caches its parsed migration list so a restart does not re-read
  migration files through a closed classloader. If a future Paper build
  restores `/plugin`, re-test the mid-session disable path.
