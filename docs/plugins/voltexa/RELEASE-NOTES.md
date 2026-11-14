# Voltexa — release notes

Format: one section per release, newest first. Aligned with `docs/RELEASE.md`
and the changelog format in `docs/plugins/voltexa/CHANGELOG.md`.
Maintained by Community & Content.

## 2026.46 — 2026-11-14

**Added**

- First release of the Voltexa core plugin: embedded H2 database (shaded,
  zero-config), versioned SQL migration runner, config facade, logging
  facade, typed `VoltexaAPI` entry point + `voltexa-api` contract
  artifact, `/voltexa status` and `/voltexa version` commands. See the
  `0.1.0` changelog entry for the full feature list (same codebase,
  recorded before the `year.week.fix` switch).

**Changed**

- Version `0.1.0` → `2026.46` (the `year.week.fix` scheme, plan decision 8;
  resolves QA defect D1): the server loads `Voltexa v2026.46`,
  `/voltexa version` reports `Voltexa 2026.46`, and feature plugins
  resolve `VoltexaAPI 2026.46`.

**Fixed**

- — none —

**QA sign-off:** [VOL-17](/VOL/issues/VOL-17) — two-stage PASS (unit +
server on the `baseline` profile, Paper 26.3-251); post-bump re-verification
of the `2026.46` jars recorded on [VOL-18](/VOL/issues/VOL-18).

**Artifact:** `voltexa.jar` (companion contract artifact: `voltexa-api.jar`)
