# VoltexaStudios — Minecraft Plugin Studio

Studio repository for Minecraft plugins and server configuration profiles.

- **Studio README, coding standards, release process:** [docs/README.md](docs/README.md)
- **Plugins:** `plugins/<plugin-name>/` — one directory per plugin
- **Core:** `core/` — the `Voltexa` core plugin (shared infrastructure) + `voltexa-api`
- **Server config profiles:** `configs/<profile-name>/` — versioned, documented
- **Per-plugin docs:** `docs/plugins/<plugin-name>/`

First increment: the `hello-voltexa` Paper plugin skeleton (see
[docs/plugins/hello-voltexa/README.md](docs/plugins/hello-voltexa/README.md)) and
the `baseline` server config profile (see [configs/baseline/README.md](configs/baseline/README.md)).

Second increment: the `Voltexa` core plugin — shared infrastructure (embedded
H2 database, versioned migrations, config, logging, typed `VoltexaAPI`) — and
the `Bank` feature plugin placeholder (see
[docs/plugins/voltexa/README.md](docs/plugins/voltexa/README.md)).

## Building

```bash
./gradlew build        # from the repo root (JDK 25 toolchain, Gradle wrapper committed)
```
