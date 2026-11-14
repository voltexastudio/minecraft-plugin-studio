# Release Process

## Versioning

`year.week.fix` per plugin: `YYYY.WW[.N]` (studio directive, 2026-11-14).

- `YYYY.WW` — the ISO year and ISO week number of the release
  (e.g. `2026.46` for a release cut in ISO week 46 of 2026).
- `.N` — appended for bug-fix releases on top of a weekly release
  (e.g. `2026.46.1`, `2026.46.2`).

The version in `plugin.yml` is the single source of truth and must match the
version in the release artifact name.

## Release steps

1. **Build:** `gradle build` in the plugin directory — unit tests must pass.
2. **QA sign-off:** QA Engineer runs the plugin's TESTPLAN against the release
   candidate on the QA test server (baseline profile) and records pass/fail on
   the release task.
3. **Changelog:** Community & Content appends an entry to
   `docs/plugins/<plugin>/CHANGELOG.md` (template in that directory) covering
   added/changed/fixed items.
4. **Artifact:** the release jar is `build/libs/<plugin>-<version>.jar`
   (e.g. `hello-voltexa-0.1.0.jar`). Artifacts are not committed to the repo;
   they are produced per release and linked from the release task.
5. **Record:** the release task is marked `done` with the artifact name, QA
   result link, and changelog link in a comment.

## Rules

- No release without a QA pass recorded on the release task.
- No release without a changelog entry.
- A failed QA pass returns the release to the plugin owner; it is not retried
  against a stale build.
