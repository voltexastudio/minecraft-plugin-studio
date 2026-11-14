# HelloVoltexa

Studio skeleton plugin. Purpose: prove the build + load pipeline end to end —
Gradle build → unit smoke test → jar loads on a Paper server →
`/hellovoltexa` smoke command responds.

## Behavior

- On enable, logs `HelloVoltexa enabled - studio skeleton is alive.`
- Registers the `/hellovoltexa` command, which replies to the sender with
  `Hello from VoltexaStudios! The studio skeleton is alive.`
- On disable, logs `HelloVoltexa disabled.`

The `/voltexa` command namespace belongs to the Voltexa core plugin; the demo
was renamed to `/hellovoltexa` so both plugins can load on the same server.

## Build

```bash
./gradlew :plugins:hello-voltexa:build        # from the repo root
```

- Unit smoke tests run as part of the build.
- Artifact: `build/libs/hello-voltexa-0.2.0.jar`.

## Test plan

See [TESTPLAN.md](TESTPLAN.md) — two stages: unit (no server) and server
smoke test on the QA test server running the `baseline` profile.

## Changelog

See [CHANGELOG.md](CHANGELOG.md).
