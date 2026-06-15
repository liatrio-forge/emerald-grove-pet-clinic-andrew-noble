# 10-spec-upcoming-visits-page.md

## Introduction/Overview

The clinic currently has no single place to see which visits are coming up;
visits are only visible per-pet on an owner's detail page. This feature adds a
simple, read-only **Upcoming Visits** page at `/visits/upcoming` that lists all
visits scheduled within the next N days (default 7), showing the owner, pet,
date, and description for each. The primary goal is to give clinic staff a quick
at-a-glance schedule of what is coming up, without any editing capability.

## Goals

- Provide a read-only page at `/visits/upcoming` that lists upcoming visits.
- Support a `days` query parameter (default `7`) that controls how far ahead the
  window extends.
- For each upcoming visit, display the owner name, pet name, visit date, and
  description.
- Reuse existing data-access and presentation patterns (Spring Data JPA query,
  Thymeleaf layout fragment, i18n message keys) so the feature fits cleanly into
  the codebase.
- Prove the feature with both a rendered-URL artifact and a Playwright test that
  creates a visit inside the window and confirms it appears.

## User Stories

- **As a clinic staff member**, I want to see all visits scheduled for the next
  several days on one page so that I can prepare for upcoming appointments
  without clicking through each owner.
- **As a clinic staff member**, I want to adjust how many days ahead I look
  (e.g., 7, 14, 30) via the URL so that I can plan over a longer or shorter
  horizon.
- **As a clinic staff member**, I want each row to clearly show who the owner is,
  which pet is visiting, when, and why, so that the list is actionable on its
  own.

## Demoable Units of Work

### Unit 1: Upcoming-visits data query

**Purpose:** Provide the data-access capability to retrieve visits within a date
window, joined to their pet and owner, so the page has something to render.
Because `Visit` has no back-reference to `Pet`/`Owner` and no `VisitRepository`
exists, this query traverses the existing `Owner` → `Pet` → `visits`
associations.

**Functional Requirements:**

- The system shall provide a repository query that returns all visits whose
  `date` falls between a given start date and end date (inclusive on both ends).
- The query shall expose, for each matching visit, the owning owner, the pet, and
  the visit's date and description (via a view model / projection, since `Visit`
  alone does not reference its pet or owner).
- The system shall return results ordered by visit date ascending (earliest
  first); visits sharing a date may be further ordered by owner last name then
  pet name for stable output.
- The query shall return an empty result (not an error) when no visits fall in
  the window.

**Proof Artifacts:**

- Test: A `@DataJpaTest` integration test against the seeded H2 data passes,
  demonstrating the query returns the expected visits for a known date window and
  excludes visits outside it.
- Test: A test asserting an empty window returns an empty collection demonstrates
  the no-results path.

### Unit 2: Upcoming Visits page (controller + view)

**Purpose:** Render the read-only page at `/visits/upcoming`, wiring the query to
a Thymeleaf view and handling the `days` parameter.

**Functional Requirements:**

- The system shall serve an HTTP `GET /visits/upcoming` endpoint that returns an
  HTML page using the shared layout fragment.
- The system shall accept an optional `days` query parameter; when absent it
  shall default to `7`.
- The window shall span from today (inclusive) through today plus `days`
  (inclusive), using the application's system date.
- When `days` is missing, non-numeric, or less than `1`, the system shall fall
  back to the default of `7` rather than erroring.
- The page shall display, for each upcoming visit, the owner name, pet name,
  visit date, and description, ordered earliest-first.
- When there are no upcoming visits in the window, the page shall render
  successfully and show a localized "no upcoming visits" empty-state message
  instead of an empty table.
- The page shall indicate the active window (e.g., the number of days being
  shown) so the user understands what range is displayed.
- All user-facing text shall use i18n message keys added to every
  `messages*.properties` locale file (consistent with `I18nPropertiesSyncTest`).
- A navigation link to the page shall be added to the shared layout menu using
  the existing `menuItem` fragment, so the page is reachable from the UI.

**Proof Artifacts:**

- URL proof: A screenshot or saved HTML of `/visits/upcoming` rendering a list of
  upcoming visits demonstrates the page exists and shows owner/pet/date/
  description.
- URL proof: A screenshot of `/visits/upcoming?days=30` demonstrates the `days`
  parameter widens the window.
- Test: `@WebMvcTest` controller tests pass, demonstrating: default window when
  `days` absent, custom window when `days` provided, fallback when `days` invalid,
  correct view name and model attributes, and the empty-state path.
- Test (E2E): A Playwright test that creates a visit dated within the window and
  then loads `/visits/upcoming` verifies the new visit appears in the list,
  demonstrating end-to-end functionality.

## Non-Goals (Out of Scope)

1. **Editing or deleting visits from this page**: The view is strictly read-only;
   no create/update/delete actions are offered here.
2. **Filtering by vet, owner, pet, or specialty**: Only the `days` window is
   supported in this initial scope.
3. **Pagination, sorting controls, or column toggles**: The list renders all
   matching visits in a fixed ascending-date order with no interactive controls.
4. **Past visits / history view**: Only visits from today forward within the
   window are shown.
5. **A REST/JSON API for upcoming visits**: This is a server-rendered HTML page
   only.
6. **Authentication or per-user scoping**: The page shows all clinic visits,
   consistent with the rest of the application.

## Design Considerations

- The page shall use the existing shared Thymeleaf `layout` fragment
  (`fragments/layout.html`) for consistent header, navigation, and styling.
- Present the visits in a Bootstrap-styled table with columns: Owner, Pet, Date,
  Description, matching the visual conventions of existing tables (e.g., the vet
  list and owner details).
- Display the active window (number of days) near the page heading.
- Provide a clear, localized empty-state message when no visits fall in the
  window.
- Owner names should link to the owner's detail page where reasonable, reusing
  existing link patterns (optional nicety; not required for acceptance).

## Repository Standards

- **Strict TDD (Red-Green-Refactor)** as mandated by `CLAUDE.md` and
  `docs/DEVELOPMENT.md`: write failing tests before production code.
- **Layered architecture**: presentation (controller + Thymeleaf) → data
  (Spring Data JPA). Follow the existing `owner` package organization; place new
  classes alongside the existing visit/owner components in
  `org.springframework.samples.petclinic.owner`.
- **Testing conventions**: `@WebMvcTest` with MockMvc + `@MockitoBean` for the
  controller (see `OwnerControllerTests`/`VisitControllerTests`); `@DataJpaTest`
  with `@AutoConfigureTestDatabase(replace = NONE)` for the query (see
  `ClinicServiceTests`); AssertJ assertions; Arrange-Act-Assert structure.
- **i18n**: add new message keys to all `messages*.properties` locale files;
  `I18nPropertiesSyncTest` enforces key parity across locales.
- **E2E**: add a Playwright spec under `e2e-tests/` following the existing suite's
  patterns and naming.
- **Conventional commits** and the project's pre-commit hooks (which run the full
  Maven test suite); work on a feature branch (no direct commits to `main`).
- **Coverage**: meet the repository's >90% line / 100% critical-branch coverage
  expectations for the new code.

## Technical Considerations

- **Data model constraint**: `Visit` (`owner/Visit.java`) has only `date` and
  `description` and no association to `Pet` or `Owner`. The `Pet` entity owns the
  relationship via `@OneToMany ... @JoinColumn(name = "pet_id")`. There is no
  `VisitRepository`. Therefore the upcoming-visits query should be a JPQL query
  that traverses associations, e.g.:
  `SELECT ... FROM Owner o JOIN o.pets p JOIN p.visits v WHERE v.date BETWEEN :start AND :end ORDER BY v.date`.
  This can live on a new `VisitRepository` (preferred, query rooted at `Owner`
  via the entity graph) or as an additional method on `OwnerRepository`. The
  implementer should choose the option that keeps the query expressible in JPQL
  given the unidirectional mapping; record the choice during task generation.
- **View model**: Because a single JPA entity cannot carry owner+pet+visit
  together, introduce a small read-only view model / DTO (or JPQL constructor
  expression / interface projection) holding owner name, pet name, date, and
  description for the view.
- **Date source**: Use the system clock (`LocalDate.now()`), consistent with the
  `Visit` default constructor. Tests must seed visits relative to "today" (or use
  a fixed window over seeded data) to remain deterministic.
- **`days` handling**: Bind `days` as an optional request parameter with a default
  of `7`; coerce missing/invalid/non-positive values to `7`. Avoid letting a bad
  parameter produce a 400/500 — degrade gracefully to the default.
- **Caching**: No new caching is required; the existing `Vets` cache is unrelated.
- **No schema changes**: This feature reads existing tables; no new migrations or
  columns are needed.

## Security Considerations

- The page is read-only and exposes only data already visible elsewhere in the
  application (owner names, pet names, visit dates/descriptions); no new sensitive
  data is introduced.
- All dynamic content is rendered through Thymeleaf, which escapes output by
  default, mitigating XSS.
- The `days` parameter is used only to compute a date offset and is never
  interpolated into a query string; parameterized JPQL prevents injection.
- No new credentials, tokens, or secrets are involved. Proof artifacts
  (screenshots/HTML) contain only seeded sample data and are safe to commit.

## Success Metrics

1. **Functional correctness**: `GET /visits/upcoming` returns HTTP 200 and lists
   exactly the visits whose dates fall in the default 7-day window; `?days=N`
   adjusts the window accordingly.
2. **Test coverage**: New controller and query code meet the repository's >90%
   line coverage standard, with all branch paths (default/custom/invalid `days`,
   populated/empty list) covered.
3. **Proof artifacts present**: Rendered-URL artifact(s) and a passing Playwright
   test demonstrating a newly created in-window visit appears on the page.
4. **No regressions**: The full Maven test suite and existing E2E suite pass.

## Open Questions

1. Should owner/pet names link to their detail pages, or remain plain text in the
   initial version? (Default assumption: plain text is acceptable; linking is an
   optional nicety.)
2. Is there a desired upper bound on `days` (e.g., cap at 365) to avoid extreme
   windows, or is any positive integer acceptable? (Default assumption: no upper
   cap required for initial scope.)
