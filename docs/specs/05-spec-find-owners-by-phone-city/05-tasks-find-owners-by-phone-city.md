# 05-tasks-find-owners-by-phone-city.md

Implementation tasks for
[`05-spec-find-owners-by-phone-city.md`](./05-spec-find-owners-by-phone-city.md).

Follow strict TDD (RED → GREEN → REFACTOR) per `AGENTS.md`/`CLAUDE.md`: write
the failing test first for every behavior. Work on branch
`feat/find-owners-phone-city`.

## Relevant Files

| File | Why It Is Relevant |
| --- | --- |
| `src/main/java/org/springframework/samples/petclinic/owner/OwnerRepository.java` | Add a combined optional-criteria query (lastName starts-with, city starts-with, telephone exact) with pagination; keep `findByLastNameStartingWith`. |
| `src/main/java/org/springframework/samples/petclinic/owner/OwnerController.java` | Bind optional `city`/`telephone`, validate telephone explicitly, call the combined query, and preserve zero/one/many + empty-search behavior. |
| `src/main/resources/templates/owners/findOwners.html` | Add optional Telephone and City inputs (i18n labels), keep the Last name input, and surface the telephone validation message. |
| `src/test/java/org/springframework/samples/petclinic/owner/OwnerControllerTests.java` | `@WebMvcTest` cases for telephone/city/combined search, telephone validation, preserved behaviors, and the rendered form inputs; update existing find tests to mock the new query. |
| `src/test/java/org/springframework/samples/petclinic/service/ClinicServiceTests.java` | `@DataJpaTest` coverage proving the combined query filters correctly against seed data. |
| `e2e-tests/tests/pages/owner-page.ts` | Page Object for Find Owners; add helpers/locators for telephone & city search and the validation error. |
| `e2e-tests/tests/features/find-owners-search.spec.ts` | New Playwright spec: create owner, find by telephone, find by city, invalid telephone message. |
| `e2e-tests/tests/features/owner-management.spec.ts` | Existing owner E2E spec; confirm it still passes (regression). |
| `src/main/resources/db/h2/data.sql` | Reference only — seed owners (cities/telephones) used by data tests and screenshots; not modified. |

### Notes

- Java tests: `./mvnw test` (web-layer: `-Dtest=OwnerControllerTests`; data:
  `-Dtest=ClinicServiceTests`). The `maven-test-check` pre-commit hook runs the
  full `./mvnw test` suite before every commit.
- E2E tests run from `e2e-tests/` with `npm test` (targeted via `--grep`).
- i18n: the `lastName`, `city`, and `telephone` message keys already exist and
  are reused; **no new bundle keys are required**, so `I18nPropertiesSyncTest`
  is unaffected.
- Do **not** add `@Valid` to the search `Owner`; validate telephone explicitly so
  the entity's `@NotBlank` constraints don't fire on an optional search.
- Conventional commits on `feat/find-owners-phone-city` (direct commits to
  `main` are blocked).

## Tasks

### [x] 1.0 Repository combined search query (data layer)

#### 1.0 Proof Artifact(s)

- Test: `./mvnw test -Dtest=ClinicServiceTests` passes, including a new case
  proving the combined query returns owners filtered by city (starts-with),
  telephone (exact), and last name together (AND), with blank criteria ignored —
  demonstrates the data-access layer filters correctly against the seed data.
- Test: the existing `shouldFindOwnersByLastName` still passes — demonstrates
  the original last-name query/behavior is preserved.

#### 1.0 Tasks

- [x] 1.1 RED: In `ClinicServiceTests`, add a test (e.g.
  `shouldFindOwnersByCityAndTelephone`) that calls the new combined query with a
  seed city (e.g. "Madison") and asserts the expected owner(s) are returned, and
  a second assertion that a blank/empty value for a field does not filter on it.
- [x] 1.2 GREEN: Add a combined query method to `OwnerRepository` (e.g.
  `findByOptionalCriteria(String lastName, String city, String telephone, Pageable pageable)`)
  using a JPQL `@Query` where blank parameters match all:
  `(:lastName = '' OR o.lastName LIKE concat(:lastName,'%'))
  AND (:city = '' OR o.city LIKE concat(:city,'%'))
  AND (:telephone = '' OR o.telephone = :telephone)`, returning `Page<Owner>`.
- [x] 1.3 REFACTOR + VERIFY: Confirm last-name starts-with semantics are
  preserved, keep `findByLastNameStartingWith` for the existing repository API,
  and run `./mvnw test -Dtest=ClinicServiceTests` (incl. `shouldFindOwnersByLastName`).

### [x] 2.0 Controller: optional criteria binding, telephone validation, preserved behaviors

#### 2.0 Proof Artifact(s)

- Test: `./mvnw test -Dtest=OwnerControllerTests` passes, including new cases for
  search by telephone (exact), by city (starts-with), by combined city + last
  name (AND), an invalid telephone rejected with a `telephone` field error
  (`telephone.invalid`) and the find form re-rendered with no search, and the
  preserved zero/one/many-result behaviors — demonstrates the controller logic.
- CLI: `curl -s "http://localhost:8080/owners?city=Madison"` returns owners in
  Madison — demonstrates the param works end-to-end at the controller level.

#### 2.0 Tasks

- [x] 2.1 RED: Add `OwnerControllerTests` cases: (a) `?telephone=<seed number>`
  resolves to the matching owner; (b) `?city=<seed city>` returns the owners
  list; (c) `?city=...&lastName=...` combined (AND); (d) `?telephone=abc`
  returns HTTP 200, `attributeHasFieldErrorCode("owner","telephone","telephone.invalid")`,
  view `owners/findOwners`, and the combined query is never invoked.
- [x] 2.2 GREEN: In `OwnerController.processFindForm`, read the bound
  `owner.getCity()`/`owner.getTelephone()` (normalize null → ""), and before
  searching, if telephone is non-blank and does not match `\\d{10}`,
  `result.rejectValue("telephone", "telephone.invalid", ...)` and return
  `owners/findOwners` without searching.
- [x] 2.3 GREEN: Replace the search call with the combined query
  (`findByOptionalCriteria`), keeping the existing pagination, single-result
  redirect, multi-result list, and "not found" (`lastName` rejected) handling.
- [x] 2.4 GREEN: Update the existing find-form tests
  (`testProcessFindFormSuccess`, `testProcessFindFormByLastName`,
  `testProcessFindFormNoOwnersFound`) to mock the new combined query method.
- [x] 2.5 REFACTOR + VERIFY: Run `./mvnw test -Dtest=OwnerControllerTests`; start
  the app and capture the `curl` output for `?city=Madison`.

### [x] 3.0 Find Owners form — telephone and city inputs

#### 3.0 Proof Artifact(s)

- Screenshot: the Find Owners form showing Last name, Telephone, and City inputs
  — demonstrates the new optional inputs are present.
- Screenshot: the form after submitting an invalid telephone, showing the
  validation message — demonstrates the rejection path is visible to the user.
- Test: an `OwnerControllerTests` assertion that `GET /owners/find` renders
  inputs named `telephone` and `city` — demonstrates the form wiring.

#### 3.0 Tasks

- [x] 3.1 RED: Add an `OwnerControllerTests` case asserting the
  `GET /owners/find` response contains `name="telephone"` and `name="city"`
  inputs (e.g. `content().string(containsString(...))`).
- [x] 3.2 GREEN: In `findOwners.html`, add `control-group` blocks for Telephone
  (`th:field="*{telephone}"`, label `#{telephone}`) and City
  (`th:field="*{city}"`, label `#{city}`) after the Last name input, matching the
  existing form styling, and ensure the existing `#fields.allErrors()` block
  surfaces the telephone error.
- [x] 3.3 VERIFY: Start the app, submit a valid and an invalid telephone, and
  capture the two screenshots (form with three inputs; invalid-telephone
  message). Keep screenshots under 1 MB (pre-commit limit).

### [x] 4.0 End-to-end verification (Playwright)

#### 4.0 Proof Artifact(s)

- Test: `npm test -- --grep "Find Owners Search"` (in `e2e-tests/`) passes —
  demonstrates creating an owner, then finding them by telephone and by city,
  plus the invalid-telephone validation message, in a real browser.
- Playwright HTML report entry showing the new `find-owners-search.spec.ts`
  passing — demonstrates end-to-end functionality.
- Confirmation that the existing `owner-management.spec.ts` still passes —
  demonstrates no regression to the owner flows.

#### 4.0 Tasks

- [x] 4.1 Extend `owner-page.ts` with locators/helpers: `searchByTelephone(tel)`,
  `searchByCity(city)` (fill the respective input and submit), and a locator for
  the validation error message.
- [x] 4.2 RED: Create `e2e-tests/tests/features/find-owners-search.spec.ts`
  (describe "Find Owners Search") that: creates an owner with a unique last name,
  a known city, and a 10-digit telephone; (a) finds the owner by telephone and
  asserts their record is reached; (b) finds the owner by city and asserts they
  appear; (c) submits an invalid telephone and asserts the validation message is
  shown.
- [x] 4.3 VERIFY: Run `npm test -- --grep "Find Owners Search"` and confirm it
  passes; run `npm test -- --grep "Owner Management"` and confirm no regression;
  locate the HTML report entry.
- [x] 4.4 VERIFY: Run the full `./mvnw test` suite to confirm the Java side is
  green before the final commit.
