# Bank — test plan

Every change to this plugin runs the stages below. Stage 1 must pass before
stage 2; a failed stage 2 is a bug report, not a retry of the same build.

## Stage 1 — unit smoke test (no server)

```bash
./gradlew :plugins:bank:test        # from the repo root
```

Pass criteria:

- [ ] `BankSmokeTest` passes (5 tests):
  - `plugin.yml` has the expected name/version/api-version
  - `plugin.yml` `main` matches `BankPlugin`
  - the main class extends `JavaPlugin`
  - `plugin.yml` `depend` includes `Voltexa`
  - `plugin.yml` declares the `bank` command
- [ ] `BankServiceTest` passes (13 tests) — the domain logic against the real
  H2 driver (in-memory, the same engine the core shades) with the contract
  tables:
  - `balanceOfUnknownPlayerIsEmpty`
  - `depositCreatesAccountAtStartingBalance`
  - `depositAddsToExistingAccount`
  - `depositDoesNotCreateADuplicateAccountOnSecondDeposit`
  - `withdrawReducesBalance`
  - `withdrawInsufficientBalanceIsRejectedAndBalanceUnchanged`
  - `withdrawWithoutAccountIsRejected`
  - `depositExceedingMaxBalanceIsRejectedAndCreatesNoAccount`
  - `depositBelowMinTransactionIsRejected`
  - `withdrawBelowMinTransactionIsRejected`
  - `listAccountsReturnsAllAccounts`
  - `ledgerRecordsDepositAndWithdrawWithResultingBalance`
  - `balanceReflectsDepositThenWithdraw` (the acceptance flow: create,
    deposit, withdraw, balance reflects it)
- [ ] `BankMigrationResourcesTest` passes (4 tests):
  - the contract migration files are present
    (`0001_bank_accounts.sql`, `0002_bank_transactions.sql`,
    `0003_bank_transactions_account_index.sql`)
  - file names follow the `<version>_<name>.sql` convention
  - every migration's SQL is present and non-blank
  - the migrations create `bank_accounts` / `bank_transactions`
    (the ledger references the accounts table)

The core-side support (the `MigrationService.applyMigrations(owner, pluginClass)`
overload, the `VoltexaAPI` service registration, and the `DatabaseService`
transaction contract) is covered by `:core:impl:test` and
`:core:api:test` — see [../voltexa/TESTPLAN.md](../voltexa/TESTPLAN.md).

## Stage 2 — server smoke test (QA test server)

Preconditions:

- Paper **26.3-251** test server (`qa-server/`)
- `baseline` config profile deployed (see `configs/baseline/README.md`)
- `voltexa-api.jar` on the server classpath
  (`--patch-module java.base=<abs-path>/voltexa-api.jar`
  `--add-exports java.base/com.voltexastudios.voltexa.api=ALL-UNNAMED`)
- `voltexa.jar` and `bank.jar` in `plugins/`

Steps and pass criteria:

1. **Clean startup** — start the server.
   - [ ] Log contains `Voltexa 2026.46 ready` with the applied migration count
   - [ ] On a fresh database the log contains
     `applied migration 1 (bank_accounts) for bank`,
     `applied migration 2 (bank_transactions) for bank`,
     `applied migration 3 (bank_transactions_account_index) for bank`
   - [ ] Log contains
     `Bank 2026.46 enabled - VoltexaAPI 2026.46 resolved, 3 bank migration(s) applied, coins currency`
   - [ ] No plugin-related errors or stack traces
2. **Load order** — verify in the log that `Voltexa` enabled before `Bank`.
   - [ ] `depend: [Voltexa]` enforced by the server
3. **Hard depend** — restart the server with `voltexa.jar` removed from
   `plugins/` (Bank only).
   - [ ] The server reports
     `UnknownDependencyException: Unknown/missing dependency plugins: [Voltexa]`
     and does **not** load `Bank`
   - [ ] Restore `voltexa.jar` before continuing
4. **Command wiring** (console, which has all permissions):
   - [ ] `/bank` → `Only players can check their own balance (use /bank balance <player>).`
   - [ ] `/bank accounts` → `No bank accounts yet.` (on a fresh DB)
   - [ ] `/bank deposit 100` → `Only players can deposit.`
   - [ ] `/bank bogus` → `Unknown subcommand: bogus (use balance, deposit, withdraw, accounts)`
5. **Read path + data persistence** — seed the H2 file with a test account
   (e.g. `Steve`, balance `120`, a `DEPOSIT 200 → 200` and a
   `WITHDRAW 80 → 120` ledger row) while the server is stopped, then start
   the server.
   - [ ] No bank migration is re-applied (no new
     `applied migration … for bank` lines)
   - [ ] `/bank accounts` → `#1 Steve: 120 coins` (the seeded balance reads
     back through the plugin)
   - [ ] The `Bank 2026.46 enabled` line appears again (3 bank migrations
     reported from the recorded state)

   A live player deposit/withdraw is not exercised headlessly; that path is
   covered by `BankServiceTest` (stage 1) against the real H2 driver, and the
   command wiring above proves the console/OP and player-only routing.
6. **Disable path** — stop the server. (Paper 26.3 removed the `/plugin`
   command, so the disable path is the server shutdown; the plugin's
   `onDisable` must log `Bank disabled.` cleanly.)
   - [ ] Log contains `Bank disabled.` and the server stays stable

Record the server log (enable line, applied-migration lines, the
hard-dependency error, the command responses, the seeded read-back, and the
disable line) as evidence on the QA task.

## Regression notes

- Bumping the Paper version: re-run both stages; the `paper-api` pin in the
  root `build.gradle` and the `baseline` profile target version must change
  together.
- Adding a Bank migration: append a new file with the next version number
  (never edit or remove an applied file); re-run both stages and verify the
  new migration applies once and is recorded under owner `bank`.
- Changing a `BankConfig` default: the `bank.*` keys are read with defaults
  (`currency-name`, `starting-balance`, `max-balance`, `min-transaction`);
  re-run stage 1 and confirm the enable line reports the expected currency.
