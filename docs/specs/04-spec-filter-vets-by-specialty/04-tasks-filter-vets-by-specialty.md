# 04-tasks-filter-vets-by-specialty.md

Implementation tasks for
[`04-spec-filter-vets-by-specialty.md`](./04-spec-filter-vets-by-specialty.md).

Follow strict TDD (RED → GREEN → REFACTOR) per `AGENTS.md`/`CLAUDE.md`: write
the failing test first for every behavior. Work on branch
`feat/filter-vets-by-specialty`.

## Relevant Files

| File | Why It Is Relevant |
| --- | --- |
| `src/main/java/org/springframework/samples/petclinic/vet/VetController.java` | Add the optional `specialty` request param, filtering logic (named / `none` / All / invalid), page-1 reset, and model attributes for the dropdown. |
| `src/test/java/org/springframework/samples/petclinic/vet/VetControllerTests.java` | `@WebMvcTest` + `MockMvc` web-layer tests for the filtering behavior and edge cases. |
| `src/main/resources/templates/vets/vetList.html` | Render the specialty dropdown, mark the active option `selected`, and carry the `specialty` param through pagination links. |
| `src/main/resources/messages/messages.properties` | Default i18n bundle; add filter label/option keys. |
| `src/main/resources/messages/messages_en.properties` | English bundle; add the same keys (kept in sync). |
| `src/main/resources/messages/messages_es.properties` | Spanish bundle; add the same keys. |
| `src/main/resources/messages/messages_de.properties` | German bundle; add the same keys. |
| `src/main/resources/messages/messages_fa.properties` | Persian bundle; add the same keys. |
| `src/main/resources/messages/messages_ko.properties` | Korean bundle; add the same keys. |
| `src/main/resources/messages/messages_pt.properties` | Portuguese bundle; add the same keys. |
| `src/main/resources/messages/messages_ru.properties` | Russian bundle; add the same keys. |
| `src/main/resources/messages/messages_tr.properties` | Turkish bundle; add the same keys. |
| `src/test/java/org/springframework/samples/petclinic/system/I18nPropertiesSyncTest.java` | Existing test that enforces all bundles share the same keys; must stay green after adding keys. |
| `e2e-tests/tests/pages/vet-page.ts` | Page Object for the Vet Directory; add locators/helpers for the specialty dropdown and result rows. |
| `e2e-tests/tests/features/vet-specialty-filter.spec.ts` | New Playwright spec verifying filtering, URL update, shareable direct URL, and the No-specialty option. |
| `e2e-tests/tests/features/vet-directory.spec.ts` | Existing Vet Directory E2E spec; confirm it still passes (regression). |
| `src/main/resources/db/h2/data.sql` | Reference only — seed vets/specialties used by tests and screenshots; not modified. |

### Notes

- Java tests run with `./mvnw test` (web-layer: `./mvnw test -Dtest=VetControllerTests`).
- The `maven-test-check` pre-commit hook runs the full `./mvnw test` suite before
  every commit; all tests must pass to commit.
- E2E tests run from `e2e-tests/` with `npm test` (targeted via `--grep`).
- Add new message keys to **all** bundles listed above; `I18nPropertiesSyncTest`
  fails if any bundle is missing a key.
- Follow existing Thymeleaf/Spring MVC conventions; conventional commits on the
  `feat/filter-vets-by-specialty` branch (direct commits to `main` are blocked).

## Tasks

### [x] 1.0 Controller-side specialty filtering and model data

#### 1.0 Proof Artifact(s)

- Test: `./mvnw test -Dtest=VetControllerTests` passes, including new cases for
  a named specialty (`?specialty=radiology` → only radiology vets), the
  no-specialty sentinel (`?specialty=none` → only vets with zero specialties),
  the default/All view (no param → all vets), and an unrecognized value
  (`?specialty=bogus` → empty vet list, "All" treated as active) —
  demonstrates the filtering logic and edge handling.
- Test: a case asserts that the model exposes the available specialty options
  (`specialties`) and the active selection (`selectedSpecialty`), and that
  `totalItems`/`totalPages` reflect the filtered set with paging reset to page 1
  — demonstrates the pagination-reset and dropdown-data requirements.
- CLI: `curl -s "http://localhost:8080/vets.html?specialty=radiology"` returns
  HTML listing only radiology vets — demonstrates the param works end-to-end.

#### 1.0 Tasks

- [x] 1.1 RED: In `VetControllerTests`, extend the fixtures/mocks so
  `vets.findAll()` returns vets covering all cases (at least one with no
  specialty and one or more with named specialties), then add a failing test
  asserting `GET /vets.html?specialty=radiology` puts only radiology vets in the
  `listVets` model attribute.
- [x] 1.2 RED: Add failing tests for the remaining behaviors: `?specialty=none`
  returns only vets with zero specialties; no param (All) returns all vets;
  `?specialty=bogus` returns an empty `listVets`; and the model contains a
  `specialties` attribute (available options) and a `selectedSpecialty`
  attribute reflecting the request (falling back to "all" for empty/invalid).
- [x] 1.3 GREEN: In `VetController.showVetList`, add
  `@RequestParam(name = "specialty", required = false) String specialty`; load
  vets via `vetRepository.findAll()`, derive the distinct sorted specialty
  names, filter the list per the rules, and manually paginate the filtered list
  (page size 5, page reset to 1 when filtering) using a `PageImpl` so the
  existing pagination model attributes stay correct.
- [x] 1.4 GREEN: Add the `specialties` (option list) and `selectedSpecialty`
  model attributes via the existing `addPaginationModel` path (or a sibling
  helper), normalizing unknown/empty values to "all".
- [x] 1.5 REFACTOR: Extract the filtering/normalization into a private helper
  method, ensure null-safety and case-insensitive matching, keep `@Cacheable`
  usage intact, and confirm `testShowVetListHtml`/`testShowResourcesVetList`
  still pass.
- [x] 1.6 RED→GREEN: Add a `VetControllerTests` case that mocks a filtered set
  of more than 5 matching vets and asserts `totalPages > 1` and that requesting
  page 2 with the same `specialty` keeps the filter applied (closes the audit
  pagination FLAG).
- [x] 1.7 VERIFY: Run `./mvnw test -Dtest=VetControllerTests`; start the app and
  capture the `curl` output for `?specialty=radiology`.

### [x] 2.0 Specialty filter UI, pagination param carry, and i18n

#### 2.0 Proof Artifact(s)

- Screenshot: `/vets.html` showing the specialty dropdown listing
  All / dentistry / radiology / surgery / No specialty — demonstrates the
  control is present and populated.
- Screenshot: `/vets.html?specialty=surgery` showing only surgery vets with
  "surgery" selected in the dropdown — demonstrates filtering and active-state.
- Screenshot: `/vets.html?specialty=none` showing only vets with no specialty
  (e.g., James Carter, Sharon Jenkins) — demonstrates the No-specialty option.
- Test: `./mvnw test -Dtest=VetControllerTests,I18nPropertiesSyncTest` passes
  after adding filter message keys to all bundles and wiring the template —
  demonstrates rendering correctness and that i18n bundles stay in sync.

#### 2.0 Tasks

- [x] 2.1 RED: Add a `VetControllerTests` case asserting the rendered response
  for `GET /vets.html?specialty=surgery` contains a `<select name="specialty">`
  control and marks the `surgery` option as `selected` (using
  `content().string(containsString(...))`), so the template wiring is verified.
- [x] 2.2 GREEN: Add the i18n keys (e.g., `vets.filter.label`,
  `vets.filter.all`, `vets.filter.none`) to `messages.properties` and every
  locale bundle (`_en, _es, _de, _fa, _ko, _pt, _ru, _tr`), mirroring existing
  key style; provide translated values where a translation already exists for
  comparable terms and a sensible default otherwise.
- [x] 2.3 GREEN: In `vetList.html`, add a GET form targeting `/vets.html` with a
  Bootstrap-styled `<select name="specialty">` above the table: an "All" option
  (empty value), one option per `${specialties}` entry (`th:value`/`th:text`,
  `th:selected` when it equals `${selectedSpecialty}`), and a "No specialty"
  option (value `none`). Use the i18n label and an `onchange` submit with a
  no-JS submit fallback.
- [x] 2.4 GREEN: Update the pagination links in `vetList.html` to include the
  active `specialty` parameter so paging preserves the filter (extend the
  `@{...}` link expressions to carry `specialty=${selectedSpecialty}` when set).
- [x] 2.5 REFACTOR + VERIFY: Tidy the template markup, run
  `./mvnw test -Dtest=VetControllerTests,I18nPropertiesSyncTest`, start the app,
  and capture the three screenshots (default, `?specialty=surgery`,
  `?specialty=none`). Keep screenshots under 1 MB (pre-commit limit).

### [x] 3.0 End-to-end browser verification (Playwright)

#### 3.0 Proof Artifact(s)

- Test: `npm test -- --grep "Vet Specialty Filter"` (in `e2e-tests/`) passes —
  demonstrates filtering by a named specialty, the No-specialty option, that
  applying the filter updates the URL to `?specialty=...`, and that navigating
  directly to a filtered URL reproduces the same list (shareable).
- Playwright HTML report entry under `e2e-tests/test-results/html-report/`
  showing the new `vet-specialty-filter.spec.ts` passing — demonstrates the
  flow works in a real browser.
- Confirmation that the existing `vet-directory.spec.ts` still passes —
  demonstrates no regression to the Vet Directory page.

#### 3.0 Tasks

- [x] 3.1 Extend `vet-page.ts` with locators/helpers for the filter: a
  `specialtyFilter()` locator (`select[name="specialty"]`), a
  `selectSpecialty(value)` helper that selects an option and waits for
  navigation, and a `vetRows()` locator for `table#vets tbody tr`.
- [x] 3.2 RED: Create `e2e-tests/tests/features/vet-specialty-filter.spec.ts`
  (describe block "Vet Specialty Filter") with tests that: (a) select "surgery"
  via the dropdown and assert only surgery rows show and the URL contains
  `specialty=surgery`; (b) navigate directly to `/vets.html?specialty=surgery`
  and assert the same filtered result; (c) select "No specialty" and assert
  vets with no specialty are shown.
- [x] 3.3 VERIFY: Run `npm test -- --grep "Vet Specialty Filter"` and confirm it
  passes; run the existing `vet-directory.spec.ts` (e.g.,
  `npm test -- --grep "Vet Directory"`) and confirm no regression; locate the
  HTML report entry.
- [x] 3.4 VERIFY: Run the full `./mvnw test` suite once more to confirm the Java
  side is green before the final commit.
