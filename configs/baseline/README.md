# Baseline server config profile

QA baseline profile for the VoltexaStudios test server.

## Purpose

A documented, reproducible server configuration that QA uses to verify
plugins. Same profile + same world seed + same plugin jar = comparable runs.

## Target server

- **Paper 26.3** — must match the `paper-api` version pinned in
  `plugins/hello-voltexa/build.gradle`. If the test server version changes,
  this profile and the plugin's API pin change together, in one reviewed change.

## What this profile is

A set of **partial** config files. Only settings the studio pins are listed,
each with a comment explaining why; any unlisted setting falls back to the
Paper 26.3 default. This keeps the profile small, reviewable, and diffable.

| File | Pins |
| --- | --- |
| `paper.yml` | `settings.bungee-cord`, `settings.allow-native-transport`, `settings.use-alternative-luck-formula`, `chunk-system.view-distance`, `chunk-system.loader-per-tick`, `performance.max-tick-time` |
| `server.properties` | motd, `max-players=10`, `online-mode=true`, `difficulty=normal`, `default-gamemode=survival`, `pvp=true`, `spawn-protection=0`, `view-distance=10`, `white-list=false` |
| `world/world.properties` | `level-name=world`, **`level-seed=1337` (deterministic seed for reproducible QA runs)**, `level-type=minecraft:normal`, `gamemode=survival`, `pvp=true`, mob/animal spawn flags, `keep-inventory=false` |
| `eula.txt` | `eula=true` (internal test server; EULA acceptance is the studio's decision) |

## Why these pins

- **No proxy/bungee, no native transport** — the test environment is a single
  standalone server; removing proxy paths makes behavior identical across machines.
- **Fixed view distance (10) and default chunk loading** — chunk-generation
  cost is reproducible, so TPS measurements are comparable between runs.
- **`max-tick-time=50`** — the standard tick-budget warning threshold; QA
  checks the server log for tick-time violations.
- **Deterministic world seed (1337)** — the same world layout every run, so
  location-dependent plugin behavior is reproducible.
- **`spawn-protection=0`** — QA agents can test near spawn without operator
  permission gates interfering.

## Deploying to a test server

1. Start from a **fresh** Paper 26.3 server directory.
2. Copy `server.properties`, `eula.txt`, and `paper.yml` into the server root.
3. Copy `world/world.properties` into the `world/` folder (create it if the
   world does not exist yet; for an existing world, apply the file before the
   world loads).
4. Drop plugin jars into `plugins/`.
5. Start the server and confirm the log shows a clean startup with no
   tick-time violations.

## Change process

Config profile changes follow the same review path as plugin changes: propose
the change in a task comment, state what is pinned and why, and update this
README's pin table in the same change. Never edit a profile on a running
server directly — copy the profile, change the copy, redeploy.
