# 05-audit-find-owners-by-phone-city.md

## Executive Summary

- Overall Status: **PASS**
- Required Gate Failures: 0
- Flagged Risks: 1 (advisory, non-blocking)

## Gateboard

| Gate | Status | Why it failed (<=10 words) | Exact fix target |
| --- | --- | --- | --- |
| Requirement-to-test traceability | PASS | — | — |
| Proof artifact verifiability | PASS | — | — |
| Repository standards consistency | PASS | — | — |
| Open question resolution | PASS | — | — |
| Regression-risk blind spots | FLAG | Search query method swapped under controller | `## Tasks > 2.0` |
| Non-goal leakage | PASS | — | — |

## Standards Evidence Table (Required)

| Source File | Read | Standards Extracted | Conflicts |
| --- | --- | --- | --- |
| `AGENTS.md` | yes | Strict TDD; ≥90% line / 100% branch for critical logic; AAA; conventional commits | none |
| `README.md` | yes | Spring Boot; H2 auto-seeded sample data; run `./mvnw spring-boot:run` | none |
| `.pre-commit-config.yaml` | yes | `maven-test-check` runs full `./mvnw test`; no-direct-commits-to-main; added files <1000 KB | none |
| `docs/TESTING.md` | yes | `@WebMvcTest`+MockMvc+`@MockitoBean` (web); `@DataJpaTest` `ClinicServiceTests` (data); Playwright POM | none |
| `CONTRIBUTING.md` / PR template | not found | fallback to AGENTS.md + pre-commit | n/a |

## Requirement-to-Test Traceability Matrix

| Functional Requirement (spec) | Task(s) | Planned Test Artifact |
| --- | --- | --- |
| Accept optional `lastName`/`city`/`telephone` params on `GET /owners` | 2.2, 2.3 | `OwnerControllerTests` telephone/city/combined cases |
| AND filter: lastName starts-with, city starts-with, telephone exact | 1.2, 2.3 | `ClinicServiceTests` combined case + `OwnerControllerTests` |
| Blank criterion ignored; all blank → all owners | 1.2, 2.2 | `ClinicServiceTests` blank assertion + `OwnerControllerTests` (no-param success) |
| Invalid telephone → reject with `telephone.invalid`, re-render, no search | 2.2 | `OwnerControllerTests` 2.1(d) field-error case |
| Preserve zero/one/many result handling | 2.3, 2.4 | Updated `OwnerControllerTests` find-form cases |
| Repository combined query with pagination | 1.2 | `ClinicServiceTests` (`@DataJpaTest`) |
| Form includes optional Telephone + City inputs (i18n labels) | 3.2 | `OwnerControllerTests` render assertion (3.1) + screenshot |
| Inputs bind to `Owner`, submit via `GET /owners` | 3.2 | E2E `find-owners-search.spec.ts` |
| Telephone validation error displayed clearly | 3.2 | Screenshot + `OwnerControllerTests` 2.1(d) |
| Last-name input/actions unchanged | 1.3, 3.2 | Existing `OwnerControllerTests` + `shouldFindOwnersByLastName` |
| E2E: create owner, find by telephone | 4.2 | `find-owners-search.spec.ts` |
| E2E: find by city | 4.2 | `find-owners-search.spec.ts` |
| E2E: invalid telephone message | 4.2 | `find-owners-search.spec.ts` |

Every functional requirement maps to at least one task and one planned test
artifact.

## Findings

### FLAG Findings

1. The controller's search is moved from `findByLastNameStartingWith` to the new
   combined query, and three existing find-form tests are re-pointed at it.
   - Risk: last-name-only and empty-search behavior must remain byte-for-byte
     equivalent (same starts-with/case semantics, same all-owners result).
   - Suggested remediation: already mitigated — task 1.3 keeps
     `findByLastNameStartingWith` and asserts `shouldFindOwnersByLastName`; the
     updated `OwnerControllerTests` (2.4) cover last-name and no-param paths, and
     the E2E suite plus `owner-management.spec.ts` regression (4.3) cover the
     flow. No blocking action required; verify these stay green during
     implementation.

## Chain-of-Verification

- All REQUIRED gates pass with explicit evidence (traceability matrix, standards
  table, verifiable proof commands, resolved questions file).
- The single FLAG is advisory and already mitigated by tasks 1.3, 2.4, and 4.3.
- Final status: **PASS** — eligible to proceed to `/SDD-3-manage-tasks`.
