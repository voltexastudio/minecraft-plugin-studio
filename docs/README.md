# VoltexaStudios — Studio README

VoltexaStudios builds Minecraft plugins (Java, Paper/Spigot API) and versioned
server configuration profiles.

## Repository layout

```
.
├── plugins/                    # one directory per plugin
│   └── hello-voltexa/          # studio skeleton plugin (first increment)
│       ├── build.gradle        # Gradle build (JDK 21 toolchain)
│       └── src/
│           ├── main/java/com/voltxesa/hellovoltexa/
│           └── main/resources/plugin.yml
├── configs/                    # versioned server config profiles
│   └── baseline/               # QA baseline profile (Paper 1.21.4)
│       ├── README.md           # profile documentation (required for every profile)
│       ├── paper.yml           # partial profile — pins only documented settings
│       ├── server.properties
│       ├── eula.txt
│       └── world/world.properties
└── docs/
    ├── README.md               # this file
    ├── CODING-STANDARDS.md
    ├── RELEASE.md
    └── plugins/<plugin>/       # per-plugin README + TESTPLAN (required)
```

## Toolchain

- **JDK 21** (toolchain is declared in each plugin's `build.gradle`)
- **Gradle 8.5+** (or a Gradle wrapper once one is committed)
- **Paper 1.21.4** for the QA test server (matches the pinned `paper-api` version)

## Building a plugin

```bash
cd plugins/hello-voltexa
gradle build        # compiles, runs unit smoke tests, produces build/libs/hello-voltexa-0.1.0.jar
```

## Testing a plugin

Every plugin change ships with a test plan in `docs/plugins/<plugin>/TESTPLAN.md`.
The standard two-stage smoke test:

1. **Unit stage:** `gradle test` — descriptor/main-class consistency checks (no server needed).
2. **Server stage:** load the built jar on a Paper test server running the
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
