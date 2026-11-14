# VoltexaStudios — Studio README

VoltexaStudios builds Minecraft plugins (Java, Paper/Spigot API) and versioned
server configuration profiles.

## Repository layout

```
.
├── settings.gradle / build.gradle   # root: one multi-project Gradle build
├── gradlew                          # committed Gradle wrapper (9.3.0)
├── core/
│   ├── api/                         # voltexa-api.jar - typed service contracts (interfaces only)
│   └── impl/                        # voltexa.jar - the Voltexa core Paper plugin
├── plugins/
│   ├── bank/                        # first feature plugin (placeholder, depend: [Voltexa])
│   └── hello-voltexa/               # studio skeleton demo
├── configs/
│   └── baseline/                    # QA baseline profile (Paper 26.3-251)
│       ├── README.md                # profile documentation (required for every profile)
│       ├── paper.yml                # partial profile - pins only documented settings
│       ├── server.properties
│       ├── eula.txt
│       └── world/world.properties
└── docs/
    ├── README.md                    # this file
    ├── CODING-STANDARDS.md
    ├── RELEASE.md
    └── plugins/<plugin>/            # per-plugin README + TESTPLAN (required)
```

## Toolchain

- **JDK 25** (toolchain is declared in the root `build.gradle`)
- **Gradle wrapper 9.3.0** (committed; `./gradlew build` from the repo root)
- **Paper 26.3-251** for the QA test server (matches the pinned `paper-api`
  version `26.3.build.251-stable`)

## Building a plugin

```bash
./gradlew build        # from the repo root - compiles all modules, runs unit tests
```

Module artifacts: `core/api/build/libs/voltexa-api.jar`,
`core/impl/build/libs/voltexa.jar`,
`plugins/<plugin>/build/libs/<plugin>-<version>.jar`.

## Testing a plugin

Every plugin change ships with a test plan in `docs/plugins/<plugin>/TESTPLAN.md`.
The standard two-stage smoke test:

1. **Unit stage:** `./gradlew :<module>:test` (or `./gradlew build`) —
   descriptor/main-class consistency checks (no server needed).
2. **Server stage:** load the built jar on the Paper test server running the
   `baseline` config profile, following the plugin's TESTPLAN.

## Config profiles

Profiles under `configs/` are versioned and documented. A profile is a set of
**partial** config files: only settings the studio pins are listed, each with a
comment; unlisted settings fall back to the Paper defaults for the pinned
server version. Every profile must have a `README.md` stating its purpose,
target server version, deployment steps, and what is pinned and why.

## Team

| Role | Responsibility |
| --- | --- |
| Lead Plugin Engineer | Architecture, coding standards, release process, reviews |
| Plugin Developer | Plugin feature development |
| Config & Server Ops | Server configs, tuning, test servers |
| QA Engineer | Verification, regression suites, bug reports |
| Community & Content | Docs, changelogs, release notes |
