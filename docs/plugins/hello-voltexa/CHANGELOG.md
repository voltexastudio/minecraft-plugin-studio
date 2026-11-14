# HelloVoltexa — changelog

Format: one section per release, newest first. Entries grouped as
**Added** / **Changed** / **Fixed**. Maintained by Community & Content per
`docs/RELEASE.md`.

## 0.2.0 — 2026-11-14

**Changed**

- Renamed the smoke command `/voltexa` → `/hellovoltexa` so the `/voltexa`
  namespace belongs to the Voltexa core plugin (both plugins now load on the
  same server).
- Normalized the package to `com.voltexastudios.hellovoltexa` (was the
  `com.voltxesa` typo) and the build group to `com.voltexastudios`.
- Now built by the studio root Gradle build (`./gradlew build`); the
  standalone `settings.gradle` was removed.

## 0.1.0 — 2026-11-14

**Added**

- Studio skeleton plugin: `HelloVoltexaPlugin` main class, `plugin.yml`,
  `/voltexa` smoke command.
- Unit smoke test suite (`HelloVoltexaSmokeTest`) and server smoke test plan
  (`TESTPLAN.md`).
