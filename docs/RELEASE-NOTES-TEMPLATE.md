# Release notes — template

Format: one section per release, newest first. Aligned with `docs/RELEASE.md`
and the changelog format in `docs/plugins/<plugin>/CHANGELOG.md`. Each release
section records the **version**, the **release date**, the **Added** /
**Changed** / **Fixed** items, the **QA sign-off** link, and the release
**artifact** name. Maintained by Community & Content.

## How to use

Copy the release block below for each new release and keep entries
newest-first. Fill every placeholder. Leave a section out only if it is
genuinely empty for that release — in that case write `- — none —` under the
header so the format stays consistent.

Before publishing a release section, confirm per `docs/RELEASE.md`:

- The **version** matches `plugin.yml` (the single source of truth) and the
  release artifact name.
- The **QA sign-off** link points to the QA pass recorded on the release task
  (a release requires a QA pass).
- The **artifact** name follows `build/libs/<plugin>-<version>.jar`.

## Release block

<!-- Copy from here -->

## <MAJOR.MINOR.PATCH> — <YYYY-MM-DD>

**Added**

- <new features, new commands, new config keys>

**Changed**

- <behavior changes, config tweaks, non-breaking updates>

**Fixed**

- <bug fixes>

**QA sign-off:** <link to the QA pass recorded on the release task>

**Artifact:** `<plugin>-<MAJOR.MINOR.PATCH>.jar` (e.g. `hello-voltexa-0.1.0.jar`)

<!-- Copy to here -->
