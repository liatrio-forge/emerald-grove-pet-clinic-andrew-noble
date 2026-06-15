# 08-tasks-disallow-past-visit-dates.md

## Relevant Files

| File | Why It Is Relevant |
| --- | --- |
| `src/main/java/org/springframework/samples/petclinic/owner/Visit.java` | Entity to annotate with `@FutureOrPresent(message = "{visit.date.future}")` on `date`. |
| `src/test/java/org/springframework/samples/petclinic/model/ValidatorTests.java` | Add the constraint unit test (past/today/future) following the existing Bean Validation test pattern. |
| `src/test/java/org/springframework/samples/petclinic/owner/VisitControllerTests.java` | Add controller tests for past-date rejection and confirm happy-path redirect is preserved. |
| `src/main/resources/messages/messages.properties` | Default bundle; add `visit.date.future` key (English text). |
| `src/main/resources/messages/messages_de.properties` | German bundle; add `visit.date.future` (parity required by `I18nPropertiesSyncTest`). |
| `src/main/resources/messages/messages_es.properties` | Spanish bundle; add `visit.date.future`. |
| `src/main/resources/messages/messages_fa.properties` | Persian bundle; add `visit.date.future`. |
| `src/main/resources/messages/messages_ko.properties` | Korean bundle; add `visit.date.future`. |
| `src/main/resources/messages/messages_pt.properties` | Portuguese bundle; add `visit.date.future`. |
| `src/main/resources/messages/messages_ru.properties` | Russian bundle; add `visit.date.future`. |
| `src/main/resources/messages/messages_tr.properties` | Turkish bundle; add `visit.date.future`. |
| `src/main/resources/messages/messages_en.properties` | English bundle; add `visit.date.future`. |
| `src/test/java/org/springframework/samples/petclinic/system/I18nPropertiesSyncTest.java` | Enforces locale-key parity; will pass once the new key is in every bundle (no edit expected). |
| `e2e-tests/tests/features/visit-scheduling.spec.ts` | Update hard-coded past dates (`2024-02-02`, `2024-03-03`) to non-past dates; add past-date rejection case. |
| `e2e-tests/tests/pages/visit-page.ts` | Page object for the visit form (reuse `fillVisitDate`, `fillDescription`, `submit`); extend only if needed. |
| `src/main/resources/templates/pets/createOrUpdateVisitForm.html` | Visit form; renders field errors via shared `inputField` fragment — reference only, no change expected. |

### Notes

- Java unit/integration tests live under `src/test/java/...` mirroring the
  package of the class under test; run with `./mvnw test` (optionally
  `-Dtest=ClassName`).
- E2E tests are a standalone Node project under `e2e-tests/`; run with
  `cd e2e-tests && npm test` (optionally `-- --grep "Visit Scheduling"`).
- Strict TDD is mandatory: write each failing test (RED) before the production
  change (GREEN), then refactor. Never add production code before a failing test.
- Derive dates relative to `LocalDate.now()` (e.g. `minusDays(1)`, `now()`,
  `plusDays(1)`) in Java and `new Date()` math in TS — never hard-code calendar
  dates that will drift into the past.
- The pre-commit `maven-test-check` hook runs the full suite; commit on a feature
  branch (the `no-direct-commits-to-main` hook blocks `main`).

## Tasks

### [x] 1.0 Enforce "today or later" on `Visit.date` with a clear, localized message

#### 1.0 Proof Artifact(s)

- Test: `./mvnw test -Dtest=ValidatorTests` passes — a new test shows a `Visit`
  with `date = today - 1 day` yields exactly one constraint violation on the
  `date` property, while `today` and `today + 1 day` yield zero violations
  (covers Unit 1 FR: `@FutureOrPresent` on `date`, today-inclusive boundary).
- Test: `./mvnw test -Dtest=I18nPropertiesSyncTest` passes — `visit.date.future`
  exists in `messages.properties` and all 8 locale bundles (covers Unit 2 FR:
  new key across all 9 bundles, key parity).
- Diff: `Visit.java` shows `@FutureOrPresent(message = "{visit.date.future}")` on
  the `date` field (covers Unit 1 FR: declarative rule on the entity).

#### 1.0 Tasks

- [x] 1.1 (RED) In `ValidatorTests.java`, add a test that builds a `Visit` with
  `date = LocalDate.now().minusDays(1)` and asserts the validator returns exactly
  one violation whose property path is `date` and whose message equals
  "must be today or a future date". Set `Locale.ENGLISH` (as the existing test
  does). Run `./mvnw test -Dtest=ValidatorTests` and confirm it FAILS.
- [x] 1.2 (RED) In the same test class, add assertions (same or a second test)
  that a `Visit` with `date = LocalDate.now()` and a `Visit` with
  `date = LocalDate.now().plusDays(1)` each return zero violations on `date`
  (today-inclusive boundary). Confirm the new assertions FAIL or error before
  implementation.
- [x] 1.3 (GREEN) Add `@FutureOrPresent(message = "{visit.date.future}")` to the
  `date` field in `Visit.java` (import `jakarta.validation.constraints.FutureOrPresent`).
- [x] 1.4 (GREEN) Add the key `visit.date.future=must be today or a future date`
  to `messages.properties` and `messages_en.properties`.
- [x] 1.5 (GREEN) Add the `visit.date.future` key with an appropriate translation
  to each remaining bundle: `messages_de`, `messages_es`, `messages_fa`,
  `messages_ko`, `messages_pt`, `messages_ru`, `messages_tr`.
- [x] 1.6 (VERIFY) Run `./mvnw test -Dtest=ValidatorTests` and
  `./mvnw test -Dtest=I18nPropertiesSyncTest`; confirm both PASS.
- [x] 1.7 (REFACTOR) Review the test for Arrange-Act-Assert clarity and
  descriptive names; ensure dates are derived from `LocalDate.now()`, not
  hard-coded.

### [x] 2.0 Verify the controller rejects past dates and preserves the happy path

#### 2.0 Proof Artifact(s)

- Test: `./mvnw test -Dtest=VisitControllerTests` passes — a new test POSTs a
  past date to `/owners/{ownerId}/pets/{petId}/visits/new`, asserts the response
  status is 200, the view is `pets/createOrUpdateVisitForm` (no redirect), and
  the model has a field error on `visit.date` (covers Unit 1 FR: no-save +
  re-render; Unit 2 FR: error on the `date` field).
- Test: `./mvnw test -Dtest=VisitControllerTests` happy-path test (today/future
  date) still redirects to `redirect:/owners/{ownerId}` (covers Unit 1 FR:
  existing booking behavior preserved; Success Metric 4).

#### 2.0 Tasks

- [x] 2.1 (RED) In `VisitControllerTests.java`, add `testProcessNewVisitFormPastDateRejected`:
  POST to `/owners/{ownerId}/pets/{petId}/visits/new` with a valid `description`
  and `date` = `LocalDate.now().minusDays(1)` formatted `yyyy-MM-dd`. Assert
  `status().isOk()`, `view().name("pets/createOrUpdateVisitForm")`, and
  `model().attributeHasFieldErrors("visit", "date")`. Run
  `./mvnw test -Dtest=VisitControllerTests` and confirm it FAILS (currently the
  past date would redirect).
- [x] 2.1a (RED) In the same `testProcessNewVisitFormPastDateRejected` test, also
  assert the re-rendered form preserves the user's input: the `visit` model
  attribute retains the submitted `description`
  (`model().attribute("visit", hasProperty("description", is("...")))`) and the
  rejected `date` value is retained on the bound object. This closes the Unit 2
  requirement that entered values are preserved on redisplay. Confirm the new
  assertion FAILS before implementation.
- [x] 2.2 (VERIFY happy path) Confirm `testProcessNewVisitFormSuccess` still
  passes after the entity change (it posts no `date`, so the constructor default
  of today remains valid). If brittle, make its valid date explicit using
  `LocalDate.now()` formatted `yyyy-MM-dd`.
- [x] 2.3 (GREEN) Confirm no controller code change is required — the existing
  `if (result.hasErrors())` branch already returns the form view. Make the test
  from 2.1 pass via the entity annotation added in 1.3 only. If a gap is found,
  make the minimum change in `VisitController` to satisfy the test.
- [x] 2.4 (VERIFY) Run `./mvnw test -Dtest=VisitControllerTests`; confirm the new
  past-date test and the existing happy-path/error tests all PASS.
- [x] 2.5 (REFACTOR) Ensure the new test uses Arrange-Act-Assert, a descriptive
  name, and a date derived from `LocalDate.now()`.

### [x] 3.0 Prove the flow end-to-end and remove the past-date regression in existing e2e

#### 3.0 Proof Artifact(s)

- Test: `cd e2e-tests && npm test -- --grep "Visit Scheduling"` passes — a new
  case submits a past date and asserts the localized validation error is visible
  and the user is NOT redirected to the owner page; the existing happy-path case
  is updated to use a non-past date and still reaches "Pets and Visits" (covers
  Unit 2 FR: end-to-end past-date error + happy-path intact).
- Screenshot: Playwright artifact captured on the past-date attempt under
  `e2e-tests/test-results/` showing the inline error (synthetic data only, no
  secrets).

#### 3.0 Tasks

- [x] 3.1 (REGRESSION FIX) In `visit-scheduling.spec.ts`, replace the hard-coded
  past dates (`2024-02-02` in the happy-path test and `2024-03-03` in the
  description-required test) with a computed non-past date (today or a near-future
  date derived from `new Date()`, formatted `yyyy-MM-dd`). Confirm both existing
  cases still pass under the new rule.
- [x] 3.2 (RED) Add a new test case in the `Visit Scheduling` describe block that
  navigates to the new-visit form, fills `date` with a computed past date
  (e.g. yesterday), fills a description, submits, and asserts: the localized error
  text "must be today or a future date" is visible AND the page is still on the
  visit form (heading from `visitPage.heading()` visible / not redirected to
  "Pets and Visits"). Also assert the form still shows the previously entered
  description after the past-date error (input preserved end-to-end). Run
  `npm test -- --grep "Visit Scheduling"` and confirm the new case FAILS before
  the backend change is present (or document that it is run against the built app
  with the feature).
- [x] 3.3 (GREEN) Run the e2e suite against the app with the feature implemented
  (Playwright auto-starts the app via Maven per `docs/TESTING.md`); confirm the
  new past-date case passes.
- [x] 3.4 (ARTIFACT) Ensure the past-date test captures a screenshot to
  `testInfo.outputPath(...)` (mirroring the existing happy-path screenshot) so a
  reviewable artifact lands under `e2e-tests/test-results/`. Use only synthetic
  data.
- [x] 3.5 (VERIFY) Run `cd e2e-tests && npm test -- --grep "Visit Scheduling"`;
  confirm all cases in the block PASS.

### [x] 4.0 Final verification, coverage, and conventional commit

#### 4.0 Proof Artifact(s)

- CLI: `./mvnw test` runs the full suite green (covers all FRs; pre-commit
  `maven-test-check` gate).
- Coverage: `target/site/jacoco/index.html` shows new/changed code at >90% line
  coverage with the past-date rejection branch exercised (covers AGENTS.md
  coverage standard; Success Metric 6).
- CLI: `git log --oneline -1` shows a conventional commit on a feature branch
  (not `main`), satisfying the `no-direct-commits-to-main` hook.

#### 4.0 Tasks

- [x] 4.1 Run the full Java suite: `./mvnw test`; confirm green (this is also the
  pre-commit `maven-test-check` gate).
- [x] 4.2 Generate the coverage report: `./mvnw clean test jacoco:report` and open
  `target/site/jacoco/index.html`; confirm the changed `Visit` code and the
  past-date rejection path are covered, meeting >90% line coverage for new code.
- [x] 4.3 Confirm work is on a dedicated feature branch (e.g.
  `feat/disallow-past-visit-dates`), not `main`; create one if needed.
- [x] 4.4 Stage changes and commit with a conventional message (e.g.
  `feat: reject past visit dates with validation message`); let the pre-commit
  hooks (markdownlint, Maven-test-check, branch guard) run and pass.
- [x] 4.5 Verify `git log --oneline -1` shows the commit on the feature branch and
  the working tree is clean.
