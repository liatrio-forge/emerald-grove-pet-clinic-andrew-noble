# 03-audit-header-language-selector.md

## Executive Summary

- Overall Status: PASS
- Required Gate Failures: 0
- Flagged Risks: 0

## Gateboard

| Gate | Status | Why it failed (<=10 words) | Exact fix target |
| --- | --- | --- | --- |
| Requirement-to-test traceability | PASS | — | — |
| Proof artifact verifiability | PASS | — | — |
| Repository standards consistency | PASS | — | — |
| Open question resolution | PASS | — | — |
| Regression-risk blind spots (FLAG) | PASS | — | — |
| Non-goal leakage (FLAG) | PASS | — | — |

## Standards Evidence Table (Required)

| Source File | Read | Standards Extracted | Conflicts |
| --- | --- | --- | --- |
| `AGENTS.md` | yes | Strict TDD (test-first); ≥90% line coverage new code; Arrange-Act-Assert; conventional commits; layered architecture | none |
| `README.md` | yes | TDD Red-Green-Refactor; run via `./mvnw`; Java 17; docs under `docs/` | none |
| `.pre-commit-config.yaml` | yes | `maven-test-check` runs full suite on commit; `no-direct-commits-to-main`; markdownlint | none |
| `e2e-tests/package.json` | yes | Playwright runner; `npm test` = `playwright test` | none |
| `e2e-tests/tests/pages/base-page.ts` | yes | Page Object Model; navbar locators via `nav.navbar` + `getByRole('link')`; `@pages`/`@fixtures` aliases | none |

## Requirement-to-Test Traceability (Evidence)

| Functional Requirement (spec) | Planned Test Artifact |
| --- | --- |
| Selector displayed in global header on every page | `LanguageSelectorViewTests` (renders shared layout) + E2E navigation in `language-selector.spec.ts` (Task 2.3) |
| Bootstrap dropdown styled with navbar | `LanguageSelectorViewTests` markup assertion (Task 1.1) + expanded-dropdown screenshot (Task 1.6) |
| Exactly three native-name languages (English/Español/Deutsch) | `LanguageSelectorViewTests` asserts 3 option links + labels (Task 1.1) |
| Selecting reloads current page with `?lang=` appended (stay on page) | `LanguageSelectorViewTests` asserts hrefs carry `?lang=` + path (Task 1.1); E2E stays-on-page assertion (Task 2.3) |
| Visually indicate active language | `LanguageSelectorViewTests` active-marker assertion (Task 1.2); E2E `activeLanguage()` (Task 2.3) |
| Reuse existing interceptor/resolver (no mechanism change) | Enforced by leaving `WebConfiguration.java` unmodified (Relevant Files: read-only); persistence proven by E2E (Task 2.3) |
| Selector label uses i18n message key | `language` key added (Task 1.3); rendered-label assertion in `LanguageSelectorViewTests` |
| E2E: switch updates visible UI text | `language-selector.spec.ts` (Task 2.2) |
| E2E: language persists across navigation in session | `language-selector.spec.ts` persistence assertion (Task 2.3) |
| E2E: selector reflects active language | `language-selector.spec.ts` `activeLanguage()` assertion (Task 2.3) |

## Chain-of-Verification

- Self-question: Do all REQUIRED gates pass with explicit evidence? Yes.
- Fact-check: Every spec functional requirement maps to a named, observable test
  artifact (table above); proof artifacts cite exact files/paths/commands;
  standards drawn from 5 read sources incl. `AGENTS.md` and root `README.md`;
  spec lists no open questions; regression coverage explicit in Tasks 3.1–3.2;
  tasks respect all five spec Non-Goals.
- Inconsistency resolution: none required.
- Final synthesis: PASS — ready for `/SDD-3-manage-tasks`.
