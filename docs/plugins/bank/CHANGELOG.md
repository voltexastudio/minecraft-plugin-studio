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
