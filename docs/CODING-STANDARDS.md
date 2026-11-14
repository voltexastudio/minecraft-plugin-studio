# Coding Standards

## Language and platform

- **Java 25**, built with **Gradle** (Maven acceptable with Lead sign-off).
- Target server API: **Paper** (`paper-api`), pinned in the root `build.gradle`.
  The QA test server runs the matching Paper version (currently **26.3-251**).
- No NMS (Netty/Minecraft-internal) access in the first increment. If a feature
  requires it, propose it in a comment on the task before coding.

## Naming and layout

- Package root: `com.voltexastudios.<plugin>` (e.g. `com.voltexastudios.bank`).
- One Gradle module per plugin under `plugins/<plugin-name>/`; core modules live
  under `core/` (`core:api` → `voltexa-api.jar`, `core:impl` → `voltexa.jar`).
- Feature plugins declare `depend: [Voltexa]` in `plugin.yml` and compile
  against the `voltexa-api` artifact (interfaces only). No feature ships
  duplicate shared code.
- `plugin.yml` lives in `src/main/resources/`; the `main` entry must match the
  actual main class (enforced by the unit smoke test).
- Config profiles live in `configs/<profile-name>/` and are never edited in place
  on a running server — copy the profile, then change the copy.

## Code

- Plain, readable Java. No framework magic in plugin code.
- Log through the plugin's `Logger` (`getLogger()`); no `System.out`.
- Every public class has a short Javadoc stating its role.
- Keep plugin `onEnable`/`onDisable` idempotent and side-effect-light.

## Tests

- **JUnit 5** for unit tests (`src/test/java`).
- Every plugin ships at minimum:
  1. a unit smoke test proving `plugin.yml` ↔ main class consistency, and
  2. a server smoke test documented in `docs/plugins/<plugin>/TESTPLAN.md`.
- **Every plugin change ships with a test plan** (new or updated TESTPLAN entry)
  before review.

## Commits

- Conventional Commits: `feat:`, `fix:`, `docs:`, `test:`, `chore:`, `config:`.
- One logical change per commit; commit messages state the observable change.
- Releases are cut from `docs/RELEASE.md` — see that file for the process.
