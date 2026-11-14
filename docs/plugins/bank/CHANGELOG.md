# Bank — changelog

Format: one section per release, newest first. Entries grouped as
**Added** / **Changed** / **Fixed**. Maintained by Community & Content per
`docs/RELEASE.md`.

## 2026.46 — 2026-11-14

**Added**

- Bank v1 feature work: `BankService` domain logic (account open at
  `starting-balance`, balance lookups, deposit/withdraw with the
  min/max guards, account listing) — each deposit/withdraw is one JDBC
  transaction so the account change and the ledger entry commit or roll
  back together.
- `/bank` command executors: `balance` (self, or another player for an
  operator), `deposit <amount>` / `withdraw <amount>` (self-only),
  `accounts` (operator), with tab completion and a distinct message per
  error case.
- `BankConfig` — reads the `bank.*` block (currency name, starting/max
  balance, min transaction) with defaults, so Bank runs with no config file.
- Domain-logic unit tests (`BankServiceTest`, 13 tests) against the real H2
  driver, including the rejection paths and the append-only ledger.

**Changed**

- Version `0.1.0` → `2026.46` (the `year.week.fix` scheme); the artifact is
  now `bank.jar` (matching the `voltexa.jar` convention).

## 2026.46.1 — 2026-11-14

**Added**

- `VoltexaBridge` — a classloader bridge that resolves the core's
  `VoltexaAPI` through the core plugin's own classloader (the same
  `Class` object the core registered) and wraps it in a proxy that
  implements Bank's copy of the contract. Contract types crossing the
  boundary are translated: functional arguments are wrapped for the
  core, and record data carriers coming back are re-materialized as
  Bank's copies.
- `VoltexaBridgeTest` (14 tests) — verifies the bridge against a
  simulated two-classloader setup.

**Changed**

- `bank.jar` now bundles the `voltexa-api` contract, so it is
  self-contained: it works with the server's normal launch — **no JVM
  flags, no classpath changes**. `BankPlugin` resolves the API through
  `VoltexaBridge` instead of its own copy of the contract.

**Fixed**

- `NoClassDefFoundError` (`MigrationService` / `VoltexaAPI`) on Paper
  26.3 servers started with the normal launch: the contract was
  previously only available via a separate `voltexa-api.jar` on the
  server classpath. [VOL-20](/VOL/issues/VOL-20)

## 0.1.0 — 2026-11-14

**Added**

- Feature plugin wiring: `depend: [Voltexa]`, compiles against
  `voltexa-api`, resolves `VoltexaAPI` through the Bukkit Services API on
  enable.
- Contract stage: bank data model as versioned SQL migrations
  (`0001_bank_accounts`, `0002_bank_transactions`,
  `0003_bank_transactions_account_index`), applied through the core
  migration runner (`MigrationService.applyMigrations("bank",
  BankPlugin.class)`).
- `plugin.yml` declares the `/bank` command namespace.
- Feature work (accounts, deposit/withdraw, `/bank` executors) lands in
  Bank v1.
