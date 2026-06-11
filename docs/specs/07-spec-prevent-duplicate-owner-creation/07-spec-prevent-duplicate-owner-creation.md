# 07-spec-prevent-duplicate-owner-creation.md

## Introduction/Overview

The owner-creation form currently saves every submission, so re-submitting the
same person (for example, an accidental double-submit or a staff member
re-entering a known client) silently creates a second, redundant owner record.
This feature adds a small, explicit duplicate-detection rule to the owner
creation flow: when a new owner would match an existing owner on first name,
last name, and telephone, creation is blocked and the form is redisplayed with a
clear, actionable error. The primary goal is to keep the owner directory clean by
preventing accidental duplicate records, without changing the database schema.

## Goals

- Block creation of an owner that duplicates an existing owner on first name +
  last name + telephone (compared case-insensitively, with surrounding
  whitespace trimmed).
- Show a clear, actionable, input-preserving error on the creation form when a
  duplicate is detected.
- Guarantee that a blocked duplicate attempt does **not** persist a second owner
  record.
- Implement the rule entirely at the application layer (controller + repository
  query) with no database schema migration.
- Reach the repository's coverage standards (>90% line coverage for new code,
  duplicate path fully covered) via JUnit and a Playwright end-to-end proof.

## User Stories

- **As a clinic staff member**, I want the system to stop me from creating an
  owner who already exists so that the owner directory does not fill up with
  duplicate records that make finding the right client harder.
- **As a clinic staff member**, I want a clear message telling me an owner with
  the same name and telephone already exists so that I understand why my
  submission was blocked and what to do next.
- **As a clinic administrator**, I want confidence that an accidental
  double-submission of the new-owner form does not create two records so that
  reporting and lookups stay accurate.

## Demoable Units of Work

### Unit 1: Duplicate-detection rule in the creation flow

**Purpose:** Add the server-side rule that detects an existing owner matching the
submitted first name + last name + telephone and prevents a duplicate from being
saved. Serves clinic staff creating owners and administrators relying on a clean
directory.

**Functional Requirements:**

- The system shall, on `POST /owners/new`, after standard field validation
  passes, check whether an existing owner matches the submitted first name, last
  name, and telephone.
- The system shall perform the match case-insensitively and ignore leading and
  trailing whitespace on first name, last name, and telephone.
- The system shall, when a matching owner exists, not persist the submitted
  owner (no new row is created).
- The system shall, when no matching owner exists, save the owner exactly as it
  does today (preserving existing creation behavior).
- The system shall expose the duplicate lookup as an `OwnerRepository` query
  method so the rule is testable in isolation.

**Proof Artifacts:**

- Test: `OwnerControllerTests` test for the duplicate path passes — demonstrates
  that a duplicate submission is rejected, no save occurs, and the form view is
  returned (not a redirect).
- Test: `OwnerControllerTests` test for the non-duplicate (happy) path still
  passes — demonstrates existing creation behavior is preserved.
- Test: an `OwnerRepository`-level test (e.g. in `ClinicServiceTests`) for the
  new duplicate-lookup query passes — demonstrates the match rule works against a
  real database, including case-insensitive/trimmed matching.

### Unit 2: Clear, actionable error in the UI

**Purpose:** Surface the blocked duplicate to the user as an inline, actionable
error on the creation form, reusing the repository's existing error-rendering
pattern and message. Serves clinic staff who need to understand and recover from
the blocked submission.

**Functional Requirements:**

- The system shall, when a duplicate is detected, re-render the
  `owners/createOrUpdateOwnerForm` view with a field-level error attached to the
  `lastName` field.
- The system shall use the existing `duplicate` message ("is already in use") for
  the field-level error so the message is internationalized consistently with the
  rest of the application.
- The system shall preserve the values the user entered when redisplaying the
  form so they are not forced to re-type the submission.

**Proof Artifacts:**

- Test: `OwnerControllerTests` assertion that the response contains a field error
  on `lastName` for the duplicate path — demonstrates the error is surfaced on the
  correct field.
- Playwright: an end-to-end spec that creates an owner, attempts to create the
  same owner again, and asserts a visible error on the creation form (and that no
  second record is created) — demonstrates the full user-facing flow works in a
  real browser.
- Screenshot/trace: Playwright artifact captured on the duplicate attempt —
  demonstrates the actionable error is visible to the user.

## Non-Goals (Out of Scope)

1. **Edit-time duplicate detection**: This spec covers creation only
   (`POST /owners/new`). Detecting duplicates when editing an existing owner
   (`POST /owners/{id}/edit`) is explicitly out of scope and may be a future spec.
2. **Database unique constraint / schema migration**: No unique index or schema
   change will be added across H2/MySQL/PostgreSQL. Enforcement is
   application-level only.
3. **Merging or de-duplicating existing records**: This feature prevents new
   duplicates; it does not find, merge, or clean up duplicates already present in
   the data.
4. **Fuzzy / similarity matching**: Matching is exact (after case-folding and
   trimming) on the three chosen fields only. Nickname matching, typo tolerance,
   phonetic matching, and address-based matching are out of scope.
5. **Redirecting to the existing owner**: A duplicate attempt re-renders the form
   with an error; it does not navigate the user to the matching owner's detail
   page.

## Design Considerations

The creation form (`owners/createOrUpdateOwnerForm.html`) renders each field via
the shared `fragments/inputField` fragment, which already displays
Bean Validation / `BindingResult` field errors inline beneath the input. The
duplicate error will be rendered through this same mechanism by rejecting the
`lastName` field value on the `BindingResult`, so no template changes should be
required and the visual treatment will match existing validation errors. The
error text reuses the existing `duplicate` message key ("is already in use"),
which is already translated across the locale bundles.

## Repository Standards

- **Strict TDD (mandatory)**: Follow Red-Green-Refactor. Write the failing
  `OwnerControllerTests` / repository test first, then the minimum implementation,
  then refactor. No production code before a failing test.
- **Layered architecture**: Keep the lookup in `OwnerRepository` (data layer) and
  the orchestration in `OwnerController` (web layer), consistent with the existing
  `findByOptionalCriteria` pattern.
- **Spring Data JPA conventions**: Add the duplicate lookup as a repository method
  (derived query or `@Query`) consistent with the existing repository style.
- **Testing patterns**: Use `@WebMvcTest` + `MockMvc` + `@MockitoBean` for the
  controller test (mirroring `OwnerControllerTests`) and `@DataJpaTest`-style
  repository testing (mirroring `ClinicServiceTests`) for the query. Use
  Arrange-Act-Assert and descriptive test names.
- **i18n**: Reuse existing message keys; if any new key is required, add it to all
  locale bundles (the repository has an `I18nPropertiesSyncTest` that enforces
  key parity).
- **Coverage**: >90% line coverage for new code; the duplicate branch must be
  explicitly tested.
- **Commits**: Conventional commit messages; feature branch (no direct commits to
  `main`, enforced by pre-commit hook). Pre-commit runs the full Maven test suite.
- **E2E**: Add the Playwright proof to the existing `e2e-tests/` suite following
  its established structure.

## Technical Considerations

- The duplicate check must run **after** standard `@Valid` field validation in
  `processCreationForm`, so that an empty/invalid telephone or name produces the
  normal validation error rather than a misleading duplicate error.
- Case-insensitive, trimmed matching is best handled in the query (e.g.
  `LOWER(TRIM(...)) = LOWER(TRIM(:value))` or an equivalent derived
  `IgnoreCase` query combined with trimming the inputs in the controller). Ensure
  the chosen approach behaves consistently on H2 (default test DB).
- The repository method should return enough information to decide existence
  (e.g. a boolean `exists...`, a count, or a list) without loading unnecessary
  data.
- On detecting a duplicate, reject the value on `BindingResult`
  (`result.rejectValue("lastName", "duplicate")`) and return
  `VIEWS_OWNER_CREATE_OR_UPDATE_FORM` — mirroring how the existing `hasErrors()`
  branch returns the form view — so submitted input is preserved and no save
  occurs.
- No new dependencies are required.

## Security Considerations

- No credentials, API keys, or tokens are involved.
- Inputs are persisted via JPA parameterized queries (no SQL injection risk); the
  new lookup must likewise use bound parameters, not string concatenation.
- The error message reveals only that an owner with the same name and telephone
  already exists, which is consistent with the application's existing
  (unauthenticated, demo) data model and does not expose additional sensitive
  detail.
- Playwright proof artifacts (screenshots/traces) contain only synthetic test
  data and are stored under the existing `e2e-tests/test-results/` location; no
  secrets should be committed.

## Success Metrics

1. **Duplicate blocked**: A second creation attempt with the same first name +
   last name + telephone is rejected and returns the form with a field error —
   verified by JUnit and Playwright (target: 100% of duplicate attempts blocked).
2. **No phantom record**: After a blocked duplicate attempt, the owner count is
   unchanged — verified by test assertion (target: 0 additional records created).
3. **Happy path intact**: Non-duplicate owner creation still succeeds and
   redirects to the owner detail page — verified by existing/updated JUnit tests.
4. **Coverage**: New code meets >90% line coverage with the duplicate branch
   explicitly exercised.

## Open Questions

No open questions at this time.
