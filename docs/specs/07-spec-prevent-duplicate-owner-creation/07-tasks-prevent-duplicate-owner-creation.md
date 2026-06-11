# 07-tasks-prevent-duplicate-owner-creation.md

Implementation task list for
[`07-spec-prevent-duplicate-owner-creation.md`](./07-spec-prevent-duplicate-owner-creation.md).

Tasks follow strict TDD (Red-Green-Refactor) per `AGENTS.md`. Each parent task is
a demoable, end-to-end-verifiable slice.

## Relevant Files

| File | Why It Is Relevant |
| --- | --- |
| `src/main/java/org/springframework/samples/petclinic/owner/OwnerRepository.java` | Add the duplicate-lookup query method (case-insensitive, trimmed, parameterized) — data-layer entry point for the rule. |
| `src/main/java/org/springframework/samples/petclinic/owner/OwnerController.java` | Wire the duplicate check into `processCreationForm`; reject `lastName` and re-render the form on a duplicate. |
| `src/main/resources/templates/owners/createOrUpdateOwnerForm.html` | Creation form; renders field-level errors via the shared `inputField` fragment (verify no change needed). |
| `src/main/resources/messages/messages.properties` | Source of the existing `duplicate=is already in use` message reused for the field error (default bundle). |
| `src/main/resources/messages/messages_en.properties` | English bundle — confirm/ensure `duplicate` key parity (enforced by `I18nPropertiesSyncTest`). |
| `src/test/java/org/springframework/samples/petclinic/service/ClinicServiceTests.java` | Repository integration test (`@DataJpaTest`-style) for the new duplicate-lookup query against H2. |
| `src/test/java/org/springframework/samples/petclinic/owner/OwnerControllerTests.java` | Web-layer test (`@WebMvcTest` + MockMvc) for duplicate-path behavior and preserved happy path. |
| `e2e-tests/tests/features/owner-management.spec.ts` | Existing owner E2E spec; add the duplicate-creation scenario here (or a sibling spec) following established patterns. |
| `e2e-tests/tests/pages/owner-page.ts` | Owner page object; may need a helper to read the form-level/field error for the duplicate assertion. |

### Notes

- Tests live alongside existing suites; run Java tests with `./mvnw test` (or
  `-Dtest=<Class>` for a single class) and E2E with `npm test` from `e2e-tests/`.
- Strict TDD: write the failing test first (RED), minimum code to pass (GREEN),
  then refactor. Never write production code before a failing test.
- Follow existing naming, Arrange-Act-Assert structure, and layered architecture
  (data layer in repository, orchestration in controller).
- The full Maven test suite runs as a pre-commit hook (`maven-test-check`); commit
  on a feature branch (direct commits to `main` are blocked).
- Reuse the existing `duplicate` message key; do not introduce a new message
  unless the `I18nPropertiesSyncTest` parity check requires bundle updates.

## Tasks

### [ ] 1.0 Repository duplicate-lookup query (data layer)

Add an `OwnerRepository` query that reports whether an owner already exists
matching first name + last name + telephone, compared case-insensitively and
with surrounding whitespace ignored. Covers spec Unit 1 functional requirements
for the match rule and case/whitespace normalization.

#### 1.0 Proof Artifact(s)

- Test: `./mvnw test -Dtest=ClinicServiceTests` passes, including a new test
  asserting the duplicate-lookup returns true for an existing owner matched with
  differing case/whitespace (e.g. `"  george "`, `"FRANKLIN"`, `"6085551023"`)
  and false for a non-existing combination — demonstrates the match rule works
  against the real (H2) database including normalization.
- Diff: `OwnerRepository.java` shows the new bound-parameter query method —
  demonstrates the lookup is implemented at the data layer with no string
  concatenation (parameterized, injection-safe).

#### 1.0 Tasks

- [ ] 1.1 (RED) In `ClinicServiceTests`, add a failing test (e.g.
  `shouldDetectDuplicateOwnerIgnoringCaseAndWhitespace`) that calls the new
  repository method with an existing owner's first/last/telephone using mixed
  case and surrounding spaces (use known seed data, e.g. George Franklin /
  `6085551023`) and asserts a duplicate is reported.
- [ ] 1.2 (RED) Add a second failing test
  (`shouldNotDetectDuplicateForNonMatchingOwner`) asserting a non-matching
  first/last/telephone combination reports no duplicate.
- [ ] 1.3 (GREEN) Add the lookup method to `OwnerRepository` using a `@Query`
  with bound parameters and `LOWER(...)` on both sides plus trimmed inputs (e.g.
  `existsByFirstNameAndLastNameAndTelephoneIgnoreCase`-style via JPQL returning
  `boolean`/count). Verify behavior on H2.
- [ ] 1.4 (GREEN/REFACTOR) Run `./mvnw test -Dtest=ClinicServiceTests`; confirm
  green. Add a Javadoc comment consistent with the existing
  `findByOptionalCriteria` documentation style; re-run to confirm still green.

### [ ] 2.0 Controller duplicate-detection rule and UI error (web layer)

Wire the repository lookup into `OwnerController.processCreationForm` so that,
after standard `@Valid` validation passes, a detected duplicate rejects the
`lastName` field with the existing `duplicate` message, re-renders the creation
form with the user's input preserved, and does not save. Covers spec Unit 1
(no-save on duplicate; save preserved on non-duplicate; check after validation)
and Unit 2 (field-level error on `lastName`; reuse `duplicate` message; preserve
input).

#### 2.0 Proof Artifact(s)

- Test: `./mvnw test -Dtest=OwnerControllerTests` passes, including a new test
  for the duplicate path asserting `status().isOk()`,
  `view().name("owners/createOrUpdateOwnerForm")`,
  `model().attributeHasFieldErrors("owner", "lastName")`,
  `model().attributeHasFieldErrorCode("owner", "lastName", "duplicate")`, and
  `verify(owners, never()).save(any())` — demonstrates the duplicate is blocked,
  surfaced on the correct field with the correct code, and no record is saved.
- Test: existing `testProcessCreationFormSuccess` (non-duplicate happy path)
  still passes with `status().is3xxRedirection()` — demonstrates existing
  creation behavior is preserved.
- Diff: `OwnerController.java` shows the duplicate check placed after the
  `result.hasErrors()` guard — demonstrates the check runs only after field
  validation.

#### 2.0 Tasks

- [ ] 2.1 (RED) In `OwnerControllerTests`, add a failing test
  (`testProcessCreationFormRejectsDuplicate`) that stubs the new repository
  lookup to report a duplicate, posts a valid owner, and asserts `isOk()`, the
  `createOrUpdateOwnerForm` view, a field error with code `duplicate` on
  `lastName`, and `verify(owners, never()).save(any())`.
- [ ] 2.2 (RED) Add/confirm a test that when the lookup reports no duplicate, a
  valid post still redirects (extend or assert alongside
  `testProcessCreationFormSuccess`); stub the lookup to return "not duplicate" in
  setup so existing success test stays green.
- [ ] 2.3 (GREEN) In `processCreationForm`, after the `result.hasErrors()`
  guard, call the repository lookup; on a duplicate, call
  `result.rejectValue("lastName", "duplicate")`, add the existing flash/error
  pattern if appropriate, and return `VIEWS_OWNER_CREATE_OR_UPDATE_FORM` without
  saving.
- [ ] 2.4 (GREEN) Confirm `messages.properties` / `messages_en.properties`
  contain the `duplicate` key; run `./mvnw test -Dtest=I18nPropertiesSyncTest` to
  confirm bundle parity (add the key to any bundle missing it).
- [ ] 2.5 (REFACTOR) Run `./mvnw test -Dtest=OwnerControllerTests` and confirm
  green; verify the creation form still renders the `lastName` field error via
  the shared `inputField` fragment (no template change required, or adjust if
  needed).

### [ ] 3.0 End-to-end browser proof (Playwright)

Add a Playwright scenario to the existing `e2e-tests/` suite that creates an
owner, attempts to create the same owner again, asserts a visible duplicate error
on the creation form, and confirms no second record is created. Covers the spec's
end-to-end Proof Artifacts for Unit 2 and the issue's Playwright demo
requirement.

#### 3.0 Proof Artifact(s)

- Test: `npm test -- --grep "duplicate"` (run from `e2e-tests/`) passes —
  demonstrates the full user-facing duplicate-blocking flow works in a real
  browser.
- Screenshot: `e2e-tests/test-results/artifacts/.../duplicate-owner-error.png`
  captured during the test showing the visible field error on the creation form
  — demonstrates the actionable error is presented to the user (synthetic test
  data only, no secrets).

#### 3.0 Tasks

- [ ] 3.1 (RED) Add a Playwright test (in `owner-management.spec.ts` or a new
  `duplicate-owner.spec.ts`) that uses `createOwner()` to build a unique owner,
  creates it successfully, then navigates back to the new-owner form and submits
  the identical data again. Assert a visible duplicate error (text from the
  `duplicate` message rendered on the `lastName` field). Run to confirm it fails
  against current behavior.
- [ ] 3.2 (GREEN) With backend tasks 1.0/2.0 complete, run the spec and confirm
  it passes; add a `page.screenshot` saving `duplicate-owner-error.png` to the
  test's `testInfo.outputPath`.
- [ ] 3.3 Add an assertion guarding "no second record": after the duplicate
  attempt, search owners by the unique last name and assert exactly one matching
  result (single-owner redirect to detail, not a multi-result list) — confirms no
  duplicate row was created.
- [ ] 3.4 If a new error-reading helper is needed, add it to
  `e2e-tests/tests/pages/owner-page.ts` following the existing page-object
  pattern; run the full owner E2E group to confirm no regressions.
