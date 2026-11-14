# Bank — test plan

Every change to this plugin runs the stages below. Stage 1 must pass before
stage 2; a failed stage 2 is a bug report, not a retry of the same build.

## Stage 1 — unit smoke test (no server)

```bash
./gradlew :plugins:bank:test        # from the repo root
```

Pass criteria:

- [ ] `BankSmokeTest` passes (4 tests):
  - `plugin.yml` has the expected name/version/api-version
  - `plugin.yml` `main` matches `BankPlugin`
  - the main class extends `JavaPlugin`
  - `plugin.yml` `depend` includes `Voltexa`

## Stage 2 — server smoke test (QA test server)

Preconditions:

- Paper **26.3-251** test server (`qa-server/`)
- `baseline` config profile deployed (see `configs/baseline/README.md`)
- `voltexa-api.jar` on the server classpath
  (`--patch-module java.base=<abs-path>/voltexa-api.jar`
  `--add-exports java.base/com.voltexastudios.voltexa.api=ALL-UNNAMED`)
- `voltexa.jar` and `bank-<version>.jar` in `plugins/`

Steps and pass criteria:

1. **Clean startup** — start the server.
   - [ ] Log contains `Bank 0.1.0 enabled (placeholder) - VoltexaAPI 0.1.0 resolved from core.`
   - [ ] No plugin-related errors or stack traces
2. **Load order** — verify in the log that `Voltexa` enabled before `Bank`.
   - [ ] `depend: [Voltexa]` enforced by the server
3. **Disable path** — stop the server. (Paper 26.3 removed the `/plugin`
   command, so the disable path is the server shutdown; the plugin's
   `onDisable` must log `Bank disabled.` cleanly.)
   - [ ] Log contains `Bank disabled.` and the server stays stable
4. **Re-enable** — start the server again.
   - [ ] Plugin re-enables cleanly and resolves `VoltexaAPI` again
     (the enable line from step 1 appears in the new log)

Record the server log (enable line, disable line) as evidence on the QA task.

## Regression notes

- Bumping the Paper version: re-run both stages; the `paper-api` pin in the
  root `build.gradle` and the `baseline` profile target version must change
  together.
