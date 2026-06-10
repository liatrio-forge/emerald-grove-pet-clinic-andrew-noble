# 05-spec-find-owners-by-phone-city.md

## Introduction/Overview

The Find Owners screen currently searches only by last name. This feature adds
two optional search inputs — **telephone** and **city** — so staff can locate an
owner record more flexibly when they don't remember (or want to disambiguate by
more than) the last name. Multiple criteria combine with AND to narrow results,
the existing last-name behavior is preserved, and an invalid telephone entry is
rejected with a clear validation message.

## Goals

- Add optional **telephone** and **city** inputs to the Find Owners form
  alongside the existing last-name input.
- Filter owners by all provided (non-empty) criteria combined with **AND**.
- Validate telephone: when provided, it must be a 10-digit number, otherwise
  reject the search with a clear message.
- Preserve existing behavior exactly: last-name-only search, the empty search
  returning all owners, single-result redirect, multi-result pagination, and the
  "not found" path.
- Keep filtered results shareable via the existing `GET /owners` query
  parameters.

## User Stories

- **As clinic staff**, I want to search for an owner by their telephone number
  so that I can find the right record quickly when a client calls.
- **As clinic staff**, I want to filter owners by city (optionally together with
  last name) so that I can disambiguate between owners who share a last name.
- **As clinic staff**, I want a clear message when I type an invalid telephone
  number so that I know to correct it rather than seeing wrong or empty results.

## Demoable Units of Work

### Unit 1: Search backend — combined optional criteria + telephone validation

**Purpose:** Extend the `GET /owners` handler and the owner repository to filter
by last name, city, and telephone together, with telephone validation, while
preserving all existing search behaviors.

**Functional Requirements:**

- The system shall accept optional `lastName`, `city`, and `telephone` request
  parameters on `GET /owners` (bound to the existing `Owner` form object).
- The system shall return owners matching **all** provided non-empty criteria
  (AND): `lastName` starts-with (unchanged), `city` starts-with, and
  `telephone` exact match.
- The system shall treat any criterion left blank as "do not filter by that
  field"; when all three are blank it shall return all owners (current
  parameterless behavior).
- When `telephone` is non-empty and is **not** exactly 10 digits, the system
  shall reject the search, add a field error on `telephone` using the existing
  `telephone.invalid` message, re-render the Find Owners form, and **not** run a
  search.
- The system shall preserve the existing result handling: zero matches →
  re-render the find form with a "not found" message; exactly one match →
  redirect to `/owners/{id}`; multiple matches → the paginated owners list.
- The system shall provide a repository query that supports the combined
  optional criteria with pagination.

**Proof Artifacts:**

- Test: `./mvnw test -Dtest=OwnerControllerTests` passes, including new cases for
  search by telephone (exact), by city (starts-with), by combined city +
  last name (AND), and an invalid telephone rejected with the validation message
  — demonstrates the controller filtering and validation logic.
- Test: a repository/integration test (e.g., in `ClinicServiceTests`) proves the
  combined query returns the expected owners — demonstrates the data-access layer.
- CLI: `curl -s "http://localhost:8080/owners?city=Madison"` returns owners in
  Madison — demonstrates the param works end-to-end.

### Unit 2: Find Owners form — telephone and city inputs

**Purpose:** Add the optional telephone and city inputs (and the telephone
validation message) to the Find Owners form, keeping the last-name input and
existing styling.

**Functional Requirements:**

- The Find Owners form (`owners/findOwners.html`) shall include optional inputs
  for **telephone** and **city**, in addition to the existing last-name input,
  each with an i18n label (reusing the existing `telephone` and `city` message
  keys).
- The inputs shall bind to the `Owner` form object (`th:field="*{telephone}"`,
  `th:field="*{city}"`) and submit via the existing `GET /owners` form.
- The form shall display the telephone validation error clearly near the form
  when an invalid telephone is submitted.
- The form's existing last-name input and Find/Add Owner actions shall remain
  unchanged.

**Proof Artifacts:**

- Screenshot: the Find Owners form showing the Last name, Telephone, and City
  inputs — demonstrates the new optional inputs are present.
- Screenshot: the form after submitting an invalid telephone, showing the
  validation message — demonstrates the rejection path.

### Unit 3: End-to-end verification (Playwright)

**Purpose:** Prove, through a real browser, that a newly created owner can be
found by telephone and by city, and that an invalid telephone is rejected.

**Functional Requirements:**

- The system shall provide a Playwright E2E test under `e2e-tests/tests/features/`
  that creates an owner (with a known city and telephone), then finds that owner
  by **telephone** and asserts the owner's record is reached.
- The test shall also find the owner by **city** and assert the owner appears in
  the results.
- The test shall submit an invalid telephone and assert the validation message
  is shown.

**Proof Artifacts:**

- Test: `e2e-tests/tests/features/find-owners-search.spec.ts` passes —
  demonstrates create-then-find-by-telephone, find-by-city, and the invalid
  telephone message in a real browser.
- Playwright HTML report entry showing the new spec passing — demonstrates
  end-to-end functionality.

## Non-Goals (Out of Scope)

1. **Telephone normalization / partial matching**: No stripping of spaces or
   dashes and no substring/starts-with telephone search; telephone is an exact
   10-digit match.
2. **New searchable fields**: Searching by first name, address, or pet
   attributes is out of scope.
3. **Changing last-name match semantics**: The existing last-name starts-with
   (and its case behavior) is unchanged.
4. **JSON/API changes**: No changes to any REST/`@ResponseBody` endpoints; only
   the HTML Find Owners flow is affected.
5. **OR / fuzzy search**: Criteria combine with AND only; no relevance ranking
   or fuzzy matching.
6. **Empty-search gating**: Submitting an empty form still returns all owners
   (no "enter at least one criterion" prompt).

## Design Considerations

- The two new inputs are added to `owners/findOwners.html` in the same
  `form-horizontal liatrio-form` style as the existing last-name control, each
  with a `control-group` and an i18n `<label>`.
- The existing error block (`#fields.allErrors()`) already renders binding
  errors; the telephone validation error must surface there (or in an adjacent
  help-inline element) so the user sees a clear message.
- Field order on the form: Last name, Telephone, City (last name remains first
  to preserve the primary workflow).
- Inputs are optional — no field is marked required on the search form.

## Repository Standards

- **Web layer**: Follow the existing `OwnerController` conventions
  (`@GetMapping`, `Owner` form-object binding, `BindingResult`, pagination via
  `addPaginationModel`). Do not add `@Valid` to the search owner (that would
  trigger the entity's `@NotBlank` constraints); validate telephone explicitly.
- **Data access**: Add a query to `OwnerRepository` following Spring Data
  conventions. A `@Query` (JPQL) method that treats blank parameters as
  "match all" is acceptable, as is a derived/Specification approach — keep
  pagination support and preserve last-name starts-with semantics.
- **Templating**: Follow existing Thymeleaf conventions in `findOwners.html`
  (`th:field`, `th:text="#{...}"`, `#fields` error handling).
- **i18n**: Reuse existing `lastName`, `city`, `telephone` keys; add any new
  keys (e.g., a help hint) to all bundles, keeping `I18nPropertiesSyncTest`
  green.
- **Testing**: Web-layer tests in `OwnerControllerTests` (`@WebMvcTest` +
  `MockMvc` + `@MockitoBean`); data-access coverage in `ClinicServiceTests`
  (`@DataJpaTest`). E2E under `e2e-tests/` using the existing Page Object Model
  (`owner-page.ts`).
- **TDD**: Per `AGENTS.md`/`CLAUDE.md`, write failing tests first
  (Red-Green-Refactor), maintain >90% coverage on new code.
- **Workflow**: Conventional commits on `feat/find-owners-phone-city`; direct
  commits to `main` are blocked.

## Technical Considerations

- **Validation coexistence**: `Owner.telephone`, `city`, and `address` are
  `@NotBlank`, and `telephone` is `@Pattern("\\d{10}")`. The current
  `processFindForm` does not `@Valid` the owner, so binding `city`/`telephone`
  onto it will not trigger `@NotBlank`. Telephone format must therefore be
  validated explicitly in the controller (e.g., check non-empty + matches
  `\\d{10}`) and rejected via `result.rejectValue("telephone", ...)` with the
  existing `telephone.invalid` message key.
- **Combined query**: A single JPQL query is the simplest cache-free approach,
  e.g. parameters defaulting to empty string:
  `WHERE (:lastName = '' OR o.lastName LIKE concat(:lastName,'%'))
  AND (:city = '' OR o.city LIKE concat(:city,'%'))
  AND (:telephone = '' OR o.telephone = :telephone)`, returning `Page<Owner>`.
  The existing `findByLastNameStartingWith` may remain for the last-name-only
  path or be superseded by the combined query — either is acceptable as long as
  existing behavior and tests pass.
- **Behavior preservation**: Normalize null params to empty string (as the
  controller already does for `lastName`) so the parameterless `/owners` request
  still returns all owners and single/multiple-result handling is unchanged.
- **No new runtime dependencies**: Spring MVC, Spring Data JPA, Thymeleaf, and
  Bean Validation are already present.
- **Regression surface**: `OwnerControllerTests` (find-form cases) and any E2E
  owner specs should be checked, since the controller handler and the
  `findOwners.html` markup change.

## Security Considerations

- Search parameters (`lastName`, `city`, `telephone`) are user input used in a
  parameterized JPA query; binding/parameter placeholders prevent SQL injection
  — do not build queries via string concatenation.
- Thymeleaf escapes rendered values by default; reflected search terms in
  messages/results must remain escaped.
- Telephone numbers are mildly sensitive personal data but are already stored
  and displayed by the existing app; this feature does not expose any new data,
  only a new way to filter existing records.
- Proof artifacts (screenshots, test reports) show only sample/seed data; no real
  personal data or credentials should be committed.

## Success Metrics

1. **New inputs present**: The Find Owners form renders optional Telephone and
   City inputs in addition to Last name.
2. **Correct filtering**: Searching by telephone returns the exact-match owner;
   by city returns city-matching owners; combined criteria narrow with AND;
   last-name-only and empty searches behave exactly as before.
3. **Validation**: An invalid telephone (not 10 digits) is rejected with a clear
   message and no search is run.
4. **Automated proof**: New `OwnerControllerTests` cases, a repository/data test,
   and the Playwright spec pass in CI; full `./mvnw test` stays green.

## Open Questions

No open questions at this time.
