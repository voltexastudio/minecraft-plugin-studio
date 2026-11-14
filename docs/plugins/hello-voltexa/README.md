# HelloVoltexa

Studio skeleton plugin. Purpose: prove the build + load pipeline end to end —
Gradle build → unit smoke test → jar loads on a Paper server → `/voltexa`
smoke command responds.

## Behavior

- On enable, logs `HelloVoltexa enabled - studio skeleton is alive.`
- Registers the `/voltexa` command, which replies to the sender with
  `Hello from VoltexaStudios! The studio skeleton is alive.`
- On disable, logs `HelloVoltexa disabled.`

## Build

```bash
cd plugins/hello-voltexa
gradle build        # JDK 21 (toolchain declared) + Gradle 8.5+
```

- Unit smoke tests run as part of the build (`gradle test`).
- Artifact: `build/libs/hello-voltexa-0.1.0.jar`.

## Test plan

See [TESTPLAN.md](TESTPLAN.md) — two stages: unit (no server) and server
smoke test on the QA test server running the `baseline` profile.

## Changelog

See [CHANGELOG.md](CHANGELOG.md).
