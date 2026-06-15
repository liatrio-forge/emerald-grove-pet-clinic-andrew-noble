# 10-tasks-upcoming-visits-page.md

Task list derived from
[`10-spec-upcoming-visits-page.md`](10-spec-upcoming-visits-page.md).

## Relevant Files

| File | Why It Is Relevant |
| --- | --- |
| `src/main/java/org/springframework/samples/petclinic/owner/UpcomingVisit.java` | New read-only view model / DTO carrying owner name, pet name, date, and description for one upcoming visit. |
| `src/main/java/org/springframework/samples/petclinic/owner/VisitRepository.java` | New Spring Data JPA repository holding the JPQL window query that traverses `Owner → Pet → visits`. |
| `src/main/java/org/springframework/samples/petclinic/owner/UpcomingVisitsController.java` | New `@Controller` serving `GET /visits/upcoming`, handling the `days` param and building the model. |
| `src/main/resources/templates/visits/upcomingVisits.html` | New Thymeleaf template rendering the read-only table and empty-state using the shared layout fragment. |
| `src/main/resources/templates/fragments/layout.html` | Add a `menuItem` navigation link to the new page. |
| `src/main/resources/messages/messages.properties` | Base i18n keys for page title, column headers, window label, and empty-state. |
| `src/main/resources/messages/messages_*.properties` | Locale files (de, es, fa, ko, pt, ru, tr) — must receive the same keys for `I18nPropertiesSyncTest` parity. |
| `src/test/java/org/springframework/samples/petclinic/owner/UpcomingVisitsRepositoryTests.java` | New `@DataJpaTest` for the window query (in-window, out-of-window, empty, ordering). |
| `src/test/java/org/springframework/samples/petclinic/owner/UpcomingVisitsControllerTests.java` | New `@WebMvcTest` for the controller (default/custom/invalid `days`, view name, model, empty-state). |
| `e2e-tests/tests/features/upcoming-visits.spec.ts` | New Playwright spec proving a created in-window visit appears on the page. |
| `e2e-tests/tests/pages/upcoming-visits-page.ts` | Optional page object for the new page, following the existing `pages/` pattern. |

### Notes

- Strict TDD: write each failing test first (RED), implement the minimum to pass
  (GREEN), then refactor. Never write production code before a failing test.
- Java tests run with `./mvnw test` (optionally `-Dtest=ClassName`); follow the
  Arrange-Act-Assert pattern and AssertJ assertions used across the suite.
- Place Java tests alongside the package under test
  (`org.springframework.samples.petclinic.owner`).
- E2E specs live in `e2e-tests/tests/features/`; run from `e2e-tests/` with
  `npm test` (optionally `-- --grep "Upcoming Visits"`).
- Reuse the shared `fragments/layout.html` layout and existing `liatrio-*`
  table/card styling classes (see `templates/vets/vetList.html`).
- Add every new message key to all `messages*.properties` files;
  `I18nPropertiesSyncTest` enforces key parity.
- Work on the existing feature branch; conventional commits; the pre-commit hook
  runs the full Maven suite and blocks direct commits to `main`.

## Tasks

### [x] 1.0 Upcoming-visits query and read-only view model (data access)

Provide the data-access capability to fetch visits within an inclusive date
window, joined to their pet and owner, exposed through a read-only view model.
Because `Visit` has no association to `Pet`/`Owner` and no `VisitRepository`
exists, the query traverses the `Owner → Pet → visits` JPA associations.

#### 1.0 Proof Artifact(s)

- Test: `UpcomingVisitsRepositoryTests` (`@DataJpaTest`,
  `@AutoConfigureTestDatabase(replace = NONE)`) passes, demonstrating the query
  returns visits inside a known window, excludes visits outside it, and orders
  results by date ascending.
- Test: A test asserting an empty window returns an empty collection passes,
  demonstrating the no-results path.
- CLI: `./mvnw test -Dtest=UpcomingVisitsRepositoryTests` exits `0`,
  demonstrating the query implementation works against seeded H2 data.

#### 1.0 Tasks

- [x] 1.1 (RED) Write `UpcomingVisitsRepositoryTests` asserting the query returns
  the expected visits for a known `[start, end]` window over seeded/inserted
  data, including owner name, pet name, date, and description on each result.
- [x] 1.2 (RED) Add test cases for: visits exactly on the start and end
  boundaries are included (inclusive); visits outside the window are excluded;
  an empty window returns an empty collection; results are ordered by date
  ascending (then owner last name, pet name for ties).
- [x] 1.3 (GREEN) Create the `UpcomingVisit` read-only view model with owner
  name, pet name, `LocalDate` date, and description (immutable; constructor or
  record-style getters consistent with the codebase).
- [x] 1.4 (GREEN) Create `VisitRepository` with a JPQL constructor-expression
  query (`SELECT new ...UpcomingVisit(...) FROM Owner o JOIN o.pets p JOIN
  p.visits v WHERE v.date BETWEEN :start AND :end ORDER BY v.date, o.lastName,
  p.name`) returning `List<UpcomingVisit>`.
- [x] 1.5 (REFACTOR) Add Javadoc consistent with `OwnerRepository`, verify naming
  conventions, and confirm all tests in 1.1–1.2 pass via
  `./mvnw test -Dtest=UpcomingVisitsRepositoryTests`.

### [x] 2.0 Upcoming Visits page: controller and view with `days` handling

Render the read-only page at `GET /visits/upcoming`, wiring the query to a
Thymeleaf view, defaulting `days` to 7, computing the inclusive today→today+days
window, gracefully falling back on missing/invalid/non-positive `days`, and
showing an empty-state when no visits match.

#### 2.0 Proof Artifact(s)

- Test: `UpcomingVisitsControllerTests` (`@WebMvcTest`) passes, demonstrating:
  HTTP 200 and correct view name; default 7-day window when `days` absent; custom
  window when `days` provided; fallback to 7 when `days` is invalid/non-positive;
  populated-list model attribute; empty-state path.
- URL/Screenshot: `/visits/upcoming` rendered page showing a table of
  owner/pet/date/description demonstrates the page exists and lists upcoming
  visits.
- URL/Screenshot: `/visits/upcoming?days=30` rendered page demonstrates the
  `days` parameter widens the window.

#### 2.0 Tasks

- [x] 2.1 (RED) Write `UpcomingVisitsControllerTests` with `@WebMvcTest` and a
  `@MockitoBean VisitRepository`, asserting `GET /visits/upcoming` returns 200,
  view name `visits/upcomingVisits`, and an `upcomingVisits` model attribute.
- [x] 2.2 (RED) Add cases asserting: default window uses 7 days when `days` is
  absent; a provided `days=30` widens the window (verify the `[start,end]`
  passed to the repository); invalid/non-positive/non-numeric `days` falls back
  to 7 without error; the `days` value is exposed to the model for display.
- [x] 2.3 (RED) Add a case asserting the empty-result path still returns 200 and
  an empty `upcomingVisits` list (template renders empty-state).
- [x] 2.4 (GREEN) Implement `UpcomingVisitsController` with `GET /visits/upcoming`,
  an optional `days` request param (default 7), `LocalDate.now()`-based inclusive
  window, fallback coercion for invalid/non-positive values, and model
  population (`upcomingVisits`, `days`).
- [x] 2.5 (GREEN) Create `templates/visits/upcomingVisits.html` using the shared
  layout fragment and `liatrio-*` table/card styling, with columns Owner, Pet,
  Date, Description, a window-days heading label, and a localized empty-state
  block shown when the list is empty.
- [x] 2.6 (REFACTOR) Confirm graceful `days` coercion has no duplicated logic,
  verify Thymeleaf output escaping, and run
  `./mvnw test -Dtest=UpcomingVisitsControllerTests`.

### [ ] 3.0 Internationalization and navigation discoverability

Add all user-facing strings as i18n keys across every `messages*.properties`
locale (so `I18nPropertiesSyncTest` passes) and add a navigation link to the
shared layout menu so the page is reachable from the UI.

#### 3.0 Proof Artifact(s)

- Test: `I18nPropertiesSyncTest` passes, demonstrating new message keys exist with
  parity across all locale files.
- Diff: `fragments/layout.html` change adding a `menuItem` entry demonstrates the
  page is linked in the navigation bar.
- Screenshot: Navigation bar showing the "Upcoming Visits" link demonstrates UI
  discoverability.

#### 3.0 Tasks

- [ ] 3.1 (RED) Run `./mvnw test -Dtest=I18nPropertiesSyncTest` after adding keys
  only to the base file to confirm it fails on missing-locale parity (proves the
  guard works), or assert the new keys resolve in a controller/view test.
- [ ] 3.2 (GREEN) Add new keys (e.g., `upcomingVisits`, `upcomingVisits.subtitle`,
  `upcomingVisits.days`, `upcomingVisits.none`, plus reused `date`/`description`
  if not present) to `messages.properties`.
- [ ] 3.3 (GREEN) Add the same keys to every locale file (`messages_de`, `_es`,
  `_fa`, `_ko`, `_pt`, `_ru`, `_tr`) with appropriate translations (or English
  fallback where translation is unavailable), then confirm
  `I18nPropertiesSyncTest` passes.
- [ ] 3.4 (GREEN) Add a `menuItem` navigation entry in `fragments/layout.html`
  linking to `/visits/upcoming` with an appropriate Font Awesome glyph and the
  `upcomingVisits` label, marking the active menu key.
- [ ] 3.5 (REFACTOR) Verify the view references message keys (no hardcoded
  strings) and the menu link renders/active-highlights correctly.

### [ ] 4.0 End-to-end proof (Playwright)

Add a Playwright spec under `e2e-tests/` that creates a visit dated within the
window, loads `/visits/upcoming`, and verifies the visit appears; capture the
rendered-URL proof artifact.

#### 4.0 Proof Artifact(s)

- Test (E2E): `e2e-tests/tests/features/upcoming-visits.spec.ts` passes,
  demonstrating a newly created in-window visit appears on `/visits/upcoming`.
- Screenshot: Playwright-captured screenshot of `/visits/upcoming` showing the
  created visit demonstrates end-to-end functionality.
- CLI: `npm test -- --grep "Upcoming Visits"` (run from `e2e-tests/`) exits `0`,
  demonstrating the E2E journey passes.

#### 4.0 Tasks

- [ ] 4.1 Add `upcoming-visits.spec.ts` under `e2e-tests/tests/features/`,
  reusing existing fixtures/page objects (`visit-page.ts`, `owner-page.ts`) to
  create a visit dated within the next 7 days for a seeded owner's pet.
- [ ] 4.2 In the spec, navigate to `/visits/upcoming`, assert the page renders a
  list, and assert the created visit's owner, pet, date, and description appear
  in a row.
- [ ] 4.3 Add an assertion (or a second test) that `?days=N` controls the window
  (e.g., a visit just outside the default 7-day window appears only when `days`
  is widened).
- [ ] 4.4 Capture a screenshot of `/visits/upcoming` as the rendered-URL proof
  artifact and confirm `npm test -- --grep "Upcoming Visits"` passes.
