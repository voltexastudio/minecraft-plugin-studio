# Bank

First VoltexaStudios feature plugin. **Contract stage**: ships the bank
data model (versioned SQL migrations) and the `/bank` command namespace.
Feature work (accounts, deposit/withdraw, `/bank` executors) lands in
Bank v1.

## What the contract stage ships

- **Data model** — `bank_accounts` (one account per player, integer
  balance in the smallest currency unit) and `bank_transactions`
  (append-only ledger with the resulting balance), defined as versioned
  SQL migrations in `src/main/resources/migrations/` (one statement per
  file, zero-padded version prefix).
- **Migration bootstrap** — on enable, the plugin applies its migrations
  through the core's `MigrationService.applyMigrations("bank",
  BankPlugin.class)`; the core reads the files from Bank's own classloader
  and records them under owner `bank`. No file-loading code is shipped in
  this module.
- **Command namespace** — `plugin.yml` declares `/bank`; executors land in
  Bank v1.

The design document (data model, migration SQL, the exact `voltexa-api`
surface Bank v1 consumes, and the `/bank` command spec) is attached to the
contract issue on Paperclip.

## Wiring (unchanged from the placeholder)

- `plugin.yml` declares `depend: [Voltexa]` — the server refuses to enable
  Bank unless the Voltexa core is present and enabled first.
- The module compiles against the `voltexa-api` artifact (interfaces only).
- On enable, the plugin resolves the core's `VoltexaAPI` through the Bukkit
  Services API.

## Build

```bash
./gradlew :plugins:bank:build        # from the repo root
```

- Unit smoke tests run as part of the build.
- Artifact: `plugins/bank/build/libs/bank-0.1.0.jar`.

## Deployment

Like every Voltexa feature plugin, Bank resolves `VoltexaAPI` through the
Bukkit Services API, which requires the shared `voltexa-api.jar` on the
**server classpath** (see [../voltexa/README.md](../voltexa/README.md#deployment));
`bank-0.1.0.jar` itself goes in `plugins/`.

## Test plan

See [TESTPLAN.md](TESTPLAN.md) — two stages: unit (no server) and server
smoke test on the QA test server running the `baseline` profile.

## Changelog

See [CHANGELOG.md](CHANGELOG.md).
