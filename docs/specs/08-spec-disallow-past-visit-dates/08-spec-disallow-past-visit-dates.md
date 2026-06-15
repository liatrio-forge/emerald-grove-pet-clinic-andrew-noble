# 08-spec-disallow-past-visit-dates.md

## Introduction/Overview

The new-visit form currently accepts any date, including dates in the past, so a
staff member can accidentally book a visit for a date that has already passed.
This feature adds a small, declarative validation rule so that a visit's date
must be **today or later**. When a past date is submitted, creation is blocked
and the form is redisplayed with a clear, actionable field-level error; visits
for today and future dates continue to work exactly as before. The rule is
enforced at the application layer with a standard Jakarta Bean Validation
constraint and requires no database or schema change.

## Goals

- Reject any new-visit submission whose date is earlier than the current day.
- Show a clear, internationalized, input-preserving error on the visit form when
  a past date is submitted.
- Preserve existing behavior for visits dated today or in the future (still
  saved, still redirect to the owner detail page).
- Implement the rule declaratively on the `Visit` entity (no controller, template,
  or schema changes), consistent with the existing field constraints.
- Meet the repository's coverage standards (>90% line coverage for new code, the
  past-date branch fully covered) via JUnit and a Playwright end-to-end proof.

## User Stories

- **As a clinic staff member**, I want the system to stop me from booking a visit
  on a date that has already passed so that the schedule reflects only valid,
  actionable appointments.
- **As a clinic staff member**, I want a clear message telling me the date must be
  today or later so that I understand why my submission was blocked and can
  correct it quickly without losing what I typed.
- **As a clinic administrator**, I want confidence that visit records cannot be
  back-dated through the standard form so that scheduling and reporting stay
  trustworthy.

## Demoable Units of Work

### Unit 1: Server-side past-date rejection rule

**Purpose:** Add the declarative constraint that rejects a visit dated before
today and re-renders the form, while leaving today/future submissions unchanged.
Serves clinic staff booking visits and administrators relying on a valid
schedule.

**Functional Requirements:**

- The system shall annotate `Visit.date` with `@FutureOrPresent` so that a date
  earlier than the current day fails validation.
- The system shall, on `POST /owners/{ownerId}/pets/{petId}/visits/new`, when the
  submitted date is before today, not persist the visit and re-render the
  `pets/createOrUpdateVisitForm` view (no redirect).
- The system shall, when the submitted date is today or later (and other fields
  are valid), save the visit and redirect to the owner detail page exactly as it
  does today.
- The system shall continue to apply the existing `@NotBlank` description rule, so
  a past date and a blank description each produce their own field error.

**Proof Artifacts:**

- Test: a `VisitControllerTests` test for the past-date path passes —
  demonstrates a past date is rejected, no save occurs, and the form view is
  returned (not a redirect).
- Test: a `VisitControllerTests` test for the happy path (today/future date)
  still passes — demonstrates existing booking behavior is preserved and still
  redirects.
- Test: a `ValidatorTests`-style unit test asserts that a `Visit` with a
  past date produces exactly one constraint violation on the `date` property, and
  that today's date and a future date produce none — demonstrates the constraint
  itself is correct in isolation.

### Unit 2: Clear, internationalized error message in the UI

**Purpose:** Surface the blocked past date as an inline, actionable error on the
visit form using a clear, translated message. Serves clinic staff who need to
understand and recover from the blocked submission.

**Functional Requirements:**

- The system shall display a clear field-level error on the `date` field when a
  past date is submitted, rendered inline beneath the date input via the existing
  shared `inputField` fragment (no template change required).
- The system shall back the error with a new message key `visit.date.future`
  (English text: "must be today or a future date") referenced by the
  `@FutureOrPresent` constraint's `message` attribute.
- The system shall define the `visit.date.future` key in the default
  `messages.properties` and in all 8 locale bundles (de, es, fa, ko, pt, ru, tr,
  and en), keeping locale-key parity intact.
- The system shall preserve the values the user entered (date and description)
  when redisplaying the form so they are not forced to re-type the submission.

**Proof Artifacts:**

- Test: `VisitControllerTests` assertion that the response contains a field error
  on `date` for the past-date path — demonstrates the error is surfaced on the
  correct field.
- Test: `I18nPropertiesSyncTest` passes with the new key present in every bundle —
  demonstrates locale-key parity is maintained.
- Playwright: an end-to-end spec (in `e2e-tests/tests/features/`) that opens the
  new-visit form, submits a past date, and asserts a visible validation error on
  the form (and that the user is not redirected to the owner page) —
  demonstrates the full user-facing flow works in a real browser.
- Playwright: the same or a companion assertion that a today/future date is
  accepted and redirects to the owner detail page — demonstrates the happy path is
  intact end-to-end.

## Non-Goals (Out of Scope)

1. **Editing existing visits**: There is currently only a create-visit form. No
   visit-edit flow exists or is added here; back-dating prevention applies to the
   new-visit form only.
2. **Pet birth date or other date fields**: This spec changes only the visit date
   rule. Pet birth dates (which legitimately must be in the past) and any other
   date inputs are untouched.
3. **Maximum / upper-bound date limits**: The rule only enforces a lower bound
   (today or later). No "too far in the future" cap is added.
4. **Time-of-day or timezone scheduling**: Visits are date-only (`LocalDate`).
   No time component, business-hours, or timezone-aware scheduling is introduced;
   "today" is the server's current local date.
5. **Database constraint / schema migration**: No CHECK constraint or schema
   change is added across H2/MySQL/PostgreSQL. Enforcement is application-level
   only.
6. **Client-side (browser) date restriction**: Disabling past dates in the date
   picker via HTML attributes is out of scope; enforcement is server-side so it
   holds regardless of the client.

## Design Considerations

The visit form (`pets/createOrUpdateVisitForm.html`) renders the date input via
the shared `fragments/inputField` fragment, which already displays
`BindingResult` field errors inline beneath the input. A failed `@FutureOrPresent`
check rejects the `date` field, so the error renders through this same mechanism
with no template change and visual treatment identical to other validation
errors (e.g. the existing `@NotBlank` description error). The message reuses the
application's standard i18n message-key approach (like the existing
`telephone.invalid` key) so the text is clear and translatable.

## Repository Standards

- **Strict TDD (mandatory)**: Follow Red-Green-Refactor. Write the failing
  `ValidatorTests` / `VisitControllerTests` assertions first, then add the
  annotation and message keys, then refactor. No production code before a failing
  test.
- **Layered architecture**: Keep the constraint on the `Visit` entity (domain
  layer); no web-layer logic is needed because the controller already calls
  `@Valid` and branches on `result.hasErrors()`.
- **Bean Validation conventions**: Use the Jakarta `@FutureOrPresent` annotation
  with a `message` attribute referencing a message-bundle key, consistent with the
  existing `@NotBlank` on the same entity.
- **Testing patterns**: Use the `ValidatorTests` pattern (programmatic
  `Validator` from `LocalValidatorFactoryBean`/`Validation.buildDefaultValidatorFactory`)
  for the constraint unit test, and `@WebMvcTest` + `MockMvc` + `@MockitoBean`
  for `VisitControllerTests` (mirroring existing tests). Use Arrange-Act-Assert
  and descriptive test names; set the English locale where message text is
  asserted (as `ValidatorTests` does).
- **i18n**: Add the new key to all locale bundles; `I18nPropertiesSyncTest`
  enforces key parity.
- **Coverage**: >90% line coverage for new code; the past-date branch must be
  explicitly tested.
- **Commits**: Conventional commit messages; feature branch (no direct commits to
  `main`, enforced by pre-commit hook). Pre-commit runs the full Maven test suite.
- **E2E**: Add the Playwright proof to the existing `e2e-tests/` suite following
  its established structure (page objects/fixtures as used by the current visit
  scheduling spec).

## Technical Considerations

- `@FutureOrPresent` (jakarta.validation.constraints) evaluates "now or in the
  future" against the system clock; for a `LocalDate` this means the current local
  date or later, which is exactly the required "today or later" rule. Today's date
  must be treated as valid (boundary inclusive).
- The constraint runs as part of the existing `@Valid Visit visit` binding in
  `VisitController.processNewVisitForm`; the existing `if (result.hasErrors())`
  branch already returns the form view, so no controller change is required.
- The `Visit` no-arg constructor defaults `date` to `LocalDate.now()`, so the
  default (unedited) form remains valid; only an explicitly past date fails.
- Reference the message via the annotation's `message` attribute as a bundle key
  in braces, e.g. `@FutureOrPresent(message = "{visit.date.future}")`, so Spring's
  `MessageSource` resolves the localized text. Confirm the resolved key matches
  the bundle entry exactly.
- Tests must avoid hard-coding a fixed "past" calendar date that could drift;
  derive past/today/future dates relative to `LocalDate.now()` (e.g.
  `now().minusDays(1)`, `now()`, `now().plusDays(1)`) so the suite stays correct
  over time.
- No new dependencies are required (Bean Validation is already on the classpath
  via `spring-boot-starter-validation` / the existing `@NotBlank` usage).

## Security Considerations

- No credentials, API keys, or tokens are involved.
- Input is persisted via JPA parameterized queries; this change adds only a
  declarative validation annotation and introduces no new query or string
  concatenation, so there is no injection surface.
- The error message reveals only that the date must be today or later and exposes
  no sensitive data.
- Playwright proof artifacts (screenshots/traces) contain only synthetic test
  data and are stored under the existing `e2e-tests/test-results/` location; no
  secrets should be committed.

## Success Metrics

1. **Past date blocked**: A new-visit submission with a date before today is
   rejected and returns the form with a field error on `date` — verified by JUnit
   and Playwright (target: 100% of past-date attempts blocked).
2. **No phantom record**: A blocked past-date attempt does not persist a visit —
   verified by test assertion that `save` is not invoked / no visit is added
   (target: 0 visits created on rejection).
3. **Boundary correct**: A visit dated exactly today is accepted — verified by a
   constraint unit test and the controller happy-path test (target: today is
   always valid).
4. **Happy path intact**: A future-dated visit still saves and redirects to the
   owner detail page — verified by existing/updated JUnit tests and Playwright.
5. **i18n parity**: `I18nPropertiesSyncTest` passes with the new key in all 9
   bundles (target: 100% key parity).
6. **Coverage**: New code meets >90% line coverage with the past-date branch
   explicitly exercised.

## Open Questions

No open questions at this time.
