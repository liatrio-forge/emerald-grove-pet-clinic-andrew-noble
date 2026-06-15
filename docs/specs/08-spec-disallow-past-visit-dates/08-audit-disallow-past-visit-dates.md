# 08-audit-disallow-past-visit-dates.md

## Executive Summary

- Overall Status: PASS (after Run 2 remediation)
- Required Gate Failures: 0
- Flagged Risks: 1 (accepted)

## Gateboard

| Gate | Status | Why it failed (<=10 words) | Exact fix target |
| --- | --- | --- | --- |
| Requirement-to-test traceability | PASS | Fixed: sub-task 2.1a covers preserve-input FR | `## Tasks > 2.0` |
| Proof artifact verifiability | PASS | — | — |
| Repository standards consistency | PASS | — | — |
| Open question resolution | PASS | — | — |
| Regression-risk blind spots | FLAG | Mitigated by mandatory 3.1 + 3.5 | `## Tasks > 3.0` |
| Non-goal leakage | PASS | — | — |

## Standards Evidence Table (Required)

| Source File | Read | Standards Extracted | Conflicts |
| --- | --- | --- | --- |
| `AGENTS.md` | yes | Strict TDD (RED-GREEN-REFACTOR); >90% line / 100% branch; Arrange-Act-Assert; conventional commits | none |
| `README.md` | yes | Strict TDD methodology; documented doc/artifact paths; H2 default DB | none |
| `.pre-commit-config.yaml` | yes | `maven-test-check` runs `./mvnw test`; `no-direct-commits-to-main`; markdownlint `--fix` | none |
| `CONTRIBUTING.md` | not found | — | — |
| `.github/pull_request_template.md` | not found | — | — |

## Findings

### REQUIRED Failures (max 3 in main report)

1. Unit 2 functional requirement "preserve the values the user entered (date and
   description) when redisplaying the form" has no mapped test artifact.
   - Missing item: an assertion that the re-rendered form retains the submitted
     `date` and `description` after a past-date rejection.
   - File section to edit: `## Tasks > 2.0` (sub-task 2.1) and optionally
     `## Tasks > 3.0` (e2e 3.2).
   - Acceptance condition: a controller-test assertion verifies the `visit` model
     attribute retains the submitted `description` (and the rejected `date` value)
     on the re-rendered form, so the FR is traceable to a test.

### FLAG Findings (max 2 in main report)

1. Regression coverage depends on updating the existing e2e dates.
   - Risk: if sub-task 3.1 is skipped, the pre-existing hard-coded past dates in
     `visit-scheduling.spec.ts` will fail under the new rule and mask real status.
   - Suggested remediation: keep 3.1 mandatory and run the full `Visit Scheduling`
     block (3.5) before declaring done.

## User-Approved Remediation Plan

- Approved (R1 + R2) — Completed

- R1 (fixes REQUIRED): Added sub-task 2.1a asserting the re-rendered form
  preserves submitted input (`description` retained, rejected `date` retained),
  closing the Unit 2 preserve-input traceability gap. Completed.
- R2 (addresses FLAG): Added an assertion to e2e 3.2 that the form still shows the
  previously entered description after the past-date error. Completed.

## Re-Audit Delta (Run 2)

- Changed gate statuses since previous run:
  - Requirement-to-test traceability: FAIL → PASS (sub-task 2.1a maps the
    preserve-input FR to a test assertion).
- Still-failing REQUIRED gates: none.
- Newly introduced findings: none.
