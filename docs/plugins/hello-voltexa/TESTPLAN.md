# HelloVoltexa — test plan

Every change to this plugin runs the stages below. Stage 1 must pass before
stage 2; a failed stage 2 is a bug report, not a retry of the same build.

## Stage 1 — unit smoke test (no server)

```bash
./gradlew :plugins:hello-voltexa:test        # from the repo root
```

Pass criteria:

- [ ] `HelloVoltexaSmokeTest` passes (4 tests):
  - `plugin.yml` has the expected name/version/api-version
  - `plugin.yml` `main` matches `HelloVoltexaPlugin`
  - the main class extends `JavaPlugin`
  - the `/hellovoltexa` command is declared in `plugin.yml`

## Stage 2 — server smoke test (QA test server)

Preconditions:

- Paper **26.3-251** test server (`qa-server/`)
- `baseline` config profile deployed (see `configs/baseline/README.md`)
- `hello-voltexa-<version>.jar` in `plugins/`

Steps and pass criteria:

1. **Clean startup** — start the server.
   - [ ] Log contains `HelloVoltexa enabled - studio skeleton is alive.`
   - [ ] No tick-time violations in the log (`max-tick-time=50` pin)
   - [ ] No plugin-related errors or stack traces
2. **Smoke command** — as an online player or via the console, run `/hellovoltexa`.
   - [ ] Sender receives `Hello from VoltexaStudios! The studio skeleton is alive.`
3. **Disable path** — run `/plugin disable HelloVoltexa` (or restart).
   - [ ] Log contains `HelloVoltexa disabled.` and the server stays stable
4. **Re-enable** — run `/plugin enable HelloVoltexa`.
   - [ ] Plugin re-enables cleanly and `/hellovoltexa` works again

Record the server log (enable line, command exchange, disable line) as evidence
on the QA task.

## Regression notes

- Bumping the Paper version: re-run both stages; the `paper-api` pin in the
  root `build.gradle` and the `baseline` profile target version must change
  together.
