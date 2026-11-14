# Bank — changelog

Format: one section per release, newest first. Entries grouped as
**Added** / **Changed** / **Fixed**. Maintained by Community & Content per
`docs/RELEASE.md`.

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
