# Bank

First VoltexaStudios feature plugin — **placeholder** for now. Feature work
(accounts, deposit/withdraw, `/bank` commands) lands in a follow-up issue.

## Purpose of the placeholder

Prove the feature-plugin wiring end to end:

- `plugin.yml` declares `depend: [Voltexa]` — the server refuses to enable
  Bank unless the Voltexa core is present and enabled first.
- The module compiles against the `voltexa-api` artifact (interfaces only).
- On enable, the plugin resolves the core's `VoltexaAPI` through the Bukkit
  Services API and logs the resolved core version.

The placeholder ships no domain logic and no commands.

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
