# 08-validation-disallow-past-visit-dates.md

## 1) Executive Summary

- **Overall:** PASS (no gates tripped)
- **Implementation Ready:** **Yes** — all functional requirements are verified by
  passing automated tests and a browser-level proof, with full coverage of the
  changed production code and a clean, traceable commit history.
- **Key metrics:**
  - Requirements Verified: 8/8 (100%)
  - Proof Artifacts Working: 4/4 task proofs (100%)
  - Files Changed vs Expected: 12 non-doc files changed, all within the planned
    "Relevant Files" scope (0 unmapped core changes)

### Gate Results

| Gate | Result | Notes |
| --- | --- | --- |
| A — No CRITICAL/HIGH | PASS | None found |
| B — No `Unknown` in matrix | PASS | All FRs Verified |
| C — Proof artifacts accessible/functional | PASS | JUnit + Playwright re-run green; screenshot present |
| D — File integrity (tiered) | PASS | All core + supporting files mapped to FRs/tasks |
| E — Repository standards | PASS | Strict TDD, layered, i18n parity, coverage, conventional commits |
| F — Security (no secrets) | PASS | Proof scan found no credentials |

## 2) Coverage Matrix

### Functional Requirements

| Requirement | Status | Evidence |
| --- | --- | --- |
| U1-FR1: `@FutureOrPresent` on `Visit.date` rejects past dates | Verified | `Visit.java:41`; `ValidatorTests.shouldNotValidateWhenVisitDateInPast`; commit `1c25946` |
| U1-FR2: Past date not persisted, form re-rendered (no redirect) | Verified | `VisitControllerTests.testProcessNewVisitFormPastDateRejected` (status 200 + form view); commit `dc8d5fe` |
| U1-FR3: Today/future saved and redirects as before | Verified | `VisitControllerTests.testProcessNewVisitFormSuccess` (3xx redirect); `ValidatorTests.shouldValidateWhenVisitDateIsToday/InFuture` |
| U1-FR4: `@NotBlank` description rule still applies | Verified | `VisitControllerTests.testProcessNewVisitFormHasErrors` passes |
| U2-FR1: Inline field-level error on `date` | Verified | `model().attributeHasFieldErrors("visit","date")`; e2e visible "must be today or a future date"; screenshot `08-task-03-past-date-rejected.png` |
| U2-FR2: New `visit.date.future` key referenced by constraint | Verified | `Visit.java:41` `message="{visit.date.future}"`; `ValidatorTests` asserts resolved text |
| U2-FR3: Key present across bundles, parity intact | Verified | `I18nPropertiesSyncTest` passes; key in base + 7 locales (en falls back — see LOW note) |
| U2-FR4: Entered values preserved on redisplay | Verified | `VisitControllerTests` `hasProperty("description"/"date")`; e2e `toHaveValue(description)` |

### Repository Standards

| Standard Area | Status | Evidence & Compliance Notes |
| --- | --- | --- |
| Strict TDD (RED→GREEN→REFACTOR) | Verified | Documented RED failure in Task 1 proof before annotation; test-first commits |
| Layered architecture | Verified | Rule declared on `Visit` entity; no controller change required |
| Bean Validation conventions | Verified | `@FutureOrPresent` alongside existing `@NotBlank`, message-key pattern like `telephone.invalid` |
| Testing patterns | Verified | `@WebMvcTest`+`MockMvc` (controller), `LocalValidatorFactoryBean` (validator), Playwright page objects |
| i18n key parity | Verified | `I18nPropertiesSyncTest` green |
| Coverage >90% for new code | Verified | JaCoCo `Visit`: 9/9 lines, 5/5 methods (100%) |
| Conventional commits / feature branch | Verified | 5 conventional commits on `feat/disallow-past-visit-dates`; `no-direct-commits-to-main` honored |
| E2E suite structure | Verified | New case added to `visit-scheduling.spec.ts` reusing `VisitPage` |

### Proof Artifacts

| Unit/Task | Proof Artifact | Status | Verification Result |
| --- | --- | --- | --- |
| Task 1 | `ValidatorTests` + `I18nPropertiesSyncTest` pass; `Visit.java` annotation | Verified | Re-run: 4 + 2 tests, 0 failures; annotation confirmed at line 41 |
| Task 2 | `VisitControllerTests` past-date + happy-path | Verified | Re-run: 4 tests, 0 failures |
| Task 3 | Playwright `Visit Scheduling` (3 tests) + screenshot | Verified | Screenshot present and shows inline error + preserved input + no redirect |
| Task 4 | Full suite + JaCoCo coverage + commit history | Verified | 84 tests green (5 Docker skips); `Visit` 100% line coverage |

## 3) Validation Issues

No CRITICAL, HIGH, or MEDIUM issues found.

| Severity | Issue | Impact | Recommendation |
| --- | --- | --- | --- |
| LOW | Spec Unit 2 text mentions the key in "all 9 bundles (… and en)", but `messages_en.properties` is intentionally empty and skipped by `I18nPropertiesSyncTest`; English resolves from the base `messages.properties`. The key is present in 8 files (base + 7 locales). | None — requirement (key parity + clear localized message) is fully verified; this is a documented, convention-respecting deviation recorded in the Task 1 proof. | Optionally reword the spec to say "base + all enforced locale bundles" for precision. No code change needed. |

## 4) Evidence Appendix

### Git commits analyzed (`git log main..HEAD`)

```text
fadd90f docs: add task proofs and final verification for disallow-past-visit-dates
e5f2e9c test(e2e): reject past visit dates and fix hard-coded date regression
dc8d5fe test: verify visit controller rejects past dates and preserves input
1c25946 feat: reject past visit dates at the validation layer
276f663 docs: add spec, questions, and planning audit for disallow-past-visit-dates
```

### Changed files (core vs supporting), all mapped

- **Core (production):** `Visit.java` (U1-FR1/2/3, U2-FR2); `messages.properties`
  - `messages_{de,es,fa,ko,pt,ru,tr}.properties` (U2-FR2/FR3).
- **Supporting (tests):** `ValidatorTests.java`, `VisitControllerTests.java`,
  `visit-scheduling.spec.ts` — linked to the core changes above and to Tasks 1–3.
- **Supporting (docs/proofs):** spec, questions, tasks, audit, 4 proof files,
  1 screenshot — Spec 08 planning/validation artifacts.

### Proof artifact re-verification

```text
./mvnw test -Dtest=ValidatorTests,VisitControllerTests,I18nPropertiesSyncTest
[INFO] VisitControllerTests   Tests run: 4, Failures: 0, Errors: 0
[INFO] I18nPropertiesSyncTest Tests run: 2, Failures: 0, Errors: 0
[INFO] ValidatorTests         Tests run: 4, Failures: 0, Errors: 0
[INFO] BUILD SUCCESS
```

### Annotation & key parity checks

```text
Visit.java:41  @FutureOrPresent(message = "{visit.date.future}")
visit.date.future present in 8 message files (base + 7 locales)
```

### Security scan

```text
grep -riE "api_key|secret|token|password|bearer|aws_" 08-proofs/*.md
-> No secrets found
```

---

**Validation Completed:** 2026-06-15
**Validation Performed By:** Claude Opus 4.8 (1M context)
