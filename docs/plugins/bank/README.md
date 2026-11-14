# Bank

First VoltexaStudios feature plugin. **v1** ships the bank data model
(versioned SQL migrations), the `/bank` command, and the domain logic:
player accounts, deposit/withdraw, and balance lookups.

## What v1 ships

- **Data model** — `bank_accounts` (one account per player, integer balance
  in the smallest currency unit) and `bank_transactions` (append-only ledger
  with the resulting balance), defined as versioned SQL migrations in
  `src/main/resources/migrations/` (one statement per file, zero-padded
  version prefix).
- **Migration bootstrap** — on enable, the plugin applies its migrations
  through the core's `MigrationService.applyMigrations("bank",
  BankPlugin.class)`; the core reads the files from Bank's own classloader
  and records them under owner `bank`. No file-loading code is shipped in
  this module.
- **Domain logic** — `BankService` (pure domain + JDBC, no Bukkit): account
  open (auto-created at `starting-balance` on first deposit), balance
  lookups, deposit/withdraw with the min/max guards, and the account
  listing. Every deposit/withdraw is one JDBC transaction (the core's
  `withConnection` block): the account change and the ledger entry commit or
  roll back together, and the `CHECK (balance >= 0)` constraint is the last
  line of defense.
- **`/bank` command** — `balance` (the caller's, or another player's for an
  operator), `deposit <amount>` / `withdraw <amount>` (self-only), and
  `accounts` (operator). No permission nodes in v1 — the permission system
  is deferred to core v2, so OP checks (`isOp()`) stand in. Each error case
  is a distinct message.

The design document (data model, migration SQL, the exact `voltexa-api`
surface Bank consumes, and the `/bank` command spec) is attached to the
contract issue on Paperclip.

## Configuration

Bank reads a `bank.*` block from its own config (via the core's
`ConfigService.forPlugin("Bank", …)`), all with defaults, so it runs with no
config file:

| key | default | meaning |
| --- | --- | --- |
| `bank.currency-name` | `coins` | the display name of the currency |
| `bank.starting-balance` | `0` | the balance a new account opens at |
| `bank.max-balance` | `1000000000000` | the per-account cap |
| `bank.min-transaction` | `1` | the smallest allowed deposit/withdraw |

## Wiring

- `plugin.yml` declares `depend: [Voltexa]` — the server refuses to enable
  Bank unless the Voltexa core is present and enabled first.
- The module compiles against the `voltexa-api` artifact (interfaces only)
  and bundles it into `bank.jar`, so the jar is self-contained on the
  server.
- On enable, the plugin resolves the core's `VoltexaAPI` through
  `VoltexaBridge`: because Bukkit isolates plugin classloaders, Bank looks
  the service up through the core plugin's own classloader (the same
  `Class` object the core registered) and wraps it in a proxy that
  implements Bank's copy of the contract. Bank then obtains the
  `DatabaseService`, `ConfigService`, `LogService`, and `MigrationService`
  from it. Bank ships **no** DB-setup code — the core owns the pool.

## Build

```bash
./gradlew :plugins:bank:build        # from the repo root
```

- Unit tests (domain logic + smoke) run as part of the build.
- Artifact: `plugins/bank/build/libs/bank.jar`.

## Deployment

`bank.jar` is self-contained: it bundles the `voltexa-api` contract, so it
works with the server's normal launch — **no JVM flags, no classpath
changes**. Drop `bank.jar` (and `voltexa.jar`) in `plugins/` and start the
server normally.

Because Bukkit isolates plugin classloaders, Bank resolves the core's
`VoltexaAPI` through the core plugin's own classloader (`VoltexaBridge`)
rather than its own copy of the contract — see
[../voltexa/README.md](../voltexa/README.md#deployment).

## Test plan

See [TESTPLAN.md](TESTPLAN.md) — two stages: unit (no server) and server
smoke test on the QA test server running the `baseline` profile.

## Changelog

See [CHANGELOG.md](CHANGELOG.md).
