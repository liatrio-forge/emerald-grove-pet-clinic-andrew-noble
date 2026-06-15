# 10-audit-upcoming-visits-page.md

## Executive Summary

- Overall Status: PASS
- Required Gate Failures: 0
- Flagged Risks: 1

## Gateboard

| Gate | Status | Why it failed (<=10 words) | Exact fix target |
| --- | --- | --- | --- |
| Requirement-to-test traceability | PASS | — | — |
| Proof artifact verifiability | PASS | — | — |
| Repository standards consistency | PASS | — | — |
| Open question resolution | PASS | — | — |
| Regression-risk blind spots | FLAG | Shared nav layout edit may affect existing nav tests | `## Tasks > 3.0` |
| Non-goal leakage | PASS | — | — |

## Standards Evidence Table (Required)

| Source File | Read | Standards Extracted | Conflicts |
| --- | --- | --- | --- |
| `AGENTS.md` | yes | Strict TDD Red-Green-Refactor; >90% line / 100% critical-branch coverage; AAA tests; DTOs between layers; conventional commits | none |
| `README.md` | yes | Spring Boot + seeded H2 default; TDD mandatory; documented docs/workflow | none |
| `.pre-commit-config.yaml` | yes | `maven-test-check` runs full suite; `no-direct-commits-to-main`; markdownlint; gitlint | none |
| `CLAUDE.md` + `docs/TESTING.md` | yes | Test layering `@WebMvcTest`/`@DataJpaTest`; `I18nPropertiesSyncTest` parity; Playwright E2E under `e2e-tests/` | none |

## Requirement-to-Test Traceability (Reference)

| Functional Requirement | Task(s) | Planned Test Artifact |
| --- | --- | --- |
| Query returns visits in inclusive `[start,end]` | 1.4 | `UpcomingVisitsRepositoryTests` (1.1/1.2) |
| Expose owner/pet/date/description via view model | 1.3, 1.4 | `UpcomingVisitsRepositoryTests` (1.1) |
| Order by date asc, ties owner/pet | 1.4 | `UpcomingVisitsRepositoryTests` (1.2) |
| Empty window returns empty (no error) | 1.4 | `UpcomingVisitsRepositoryTests` (1.2) |
| `GET /visits/upcoming` HTML page w/ layout | 2.4, 2.5 | `UpcomingVisitsControllerTests` (2.1) |
| Optional `days`, default 7 | 2.4 | `UpcomingVisitsControllerTests` (2.2) |
| Inclusive today→today+days window | 2.4 | `UpcomingVisitsControllerTests` (2.2) |
| Invalid/non-positive `days` → fallback 7 | 2.4 | `UpcomingVisitsControllerTests` (2.2) |
| Display rows earliest-first | 2.5 | `UpcomingVisitsControllerTests` (2.1) + repo (1.2) |
| Localized empty-state | 2.5, 3.2/3.3 | `UpcomingVisitsControllerTests` (2.3) |
| Indicate active window (days) | 2.4, 2.5 | `UpcomingVisitsControllerTests` (2.2) |
| i18n keys across all locales | 3.2, 3.3 | `I18nPropertiesSyncTest` (3.1/3.3) |
| Nav link via `menuItem` | 3.4 | Diff + screenshot; E2E nav (4.x) |

## FLAG Findings (max 2 in main report)

1. Shared-layout regression risk
   - Risk: Task 3.4 edits `fragments/layout.html`, a shared navigation fragment
     used by every page. Existing navigation E2E (`base-page-navigation.spec.ts`)
     and any menu-count assertions could be affected.
   - Suggested remediation: When implementing 3.4, run the existing navigation
     E2E spec and the full Java suite; if a nav assertion counts menu items,
     update it deliberately as part of the change.
