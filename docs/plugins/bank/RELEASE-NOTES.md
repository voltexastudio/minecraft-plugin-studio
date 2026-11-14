# Bank — release notes

Format: one section per release, newest first. Aligned with `docs/RELEASE.md`
and the changelog format in `docs/plugins/bank/CHANGELOG.md`.
Maintained by Community & Content.

## 2026.46 — 2026-11-14

**Added**

- Bank v1: player bank accounts (opened at `starting-balance`),
  deposit/withdraw with min/max guards (each in one JDBC transaction with
  the append-only ledger entry), `BankConfig` with defaults, and the
  `/bank` command (`balance`, `deposit`, `withdraw`, `accounts`) with tab
  completion. See the `2026.46` changelog entry for details.

**Changed**

- Version `0.1.0` → `2026.46` (the `year.week.fix` scheme); the artifact
  is `bank.jar` (matching the `voltexa.jar` convention).

**Fixed**

- — none —

**QA sign-off:** [VOL-17](/VOL/issues/VOL-17) — two-stage PASS (unit +
server on the `baseline` profile, Paper 26.3-251, including the
hard-dependency run and the seeded read-back); post-bump re-verification
of the release jars recorded on [VOL-18](/VOL/issues/VOL-18).

**Artifact:** `bank.jar`
