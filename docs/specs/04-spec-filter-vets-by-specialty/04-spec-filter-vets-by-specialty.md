# 04-spec-filter-vets-by-specialty.md

## Introduction/Overview

The Vet Directory (`/vets.html`) currently lists every veterinarian in a
paginated table, with no way to narrow the list. This feature adds a
**specialty filter** so users can quickly find vets by their area of expertise
(for example, only those who do `surgery`). The selected filter is carried in a
`?specialty=` query parameter so a filtered view can be bookmarked and shared.
The primary goal is to make the directory easier to navigate as the care team
grows, without changing how vet data is stored or how the page is otherwise
rendered.

## Goals

- Add a visible specialty filter control to the Vet Directory page.
- Show only the veterinarians that match the selected specialty.
- Provide an **"All"** option (the default) that shows every vet, and a
  **"No specialty"** option that shows vets with no assigned specialty.
- Support a shareable `?specialty=` query parameter so filtered URLs reproduce
  the same view.
- Preserve the existing pagination behavior, carrying the active filter across
  pages and resetting to page 1 when the filter changes.

## User Stories

- **As a clinic visitor**, I want to filter the veterinarian list by specialty
  so that I can quickly find a vet who handles the care my pet needs.
- **As a user sharing a link**, I want the filtered view to live in the URL so
  that I can bookmark or send a colleague a link that opens the same filtered
  list.
- **As any user**, I want a clear "All" default and a way to see vets with no
  listed specialty so that no veterinarian is hidden from me by the filter.

## Demoable Units of Work

### Unit 1: Specialty filter on the Vet Directory

**Purpose:** Let a user on `/vets.html` filter the veterinarian list by
specialty using a dropdown, with the selection reflected in a shareable
`?specialty=` query parameter and only matching vets displayed.

**Functional Requirements:**

- The system shall add an optional `specialty` request parameter to the
  `GET /vets.html` handler in `VetController` (defaulting to "no filter" /
  "All" when absent).
- The page shall display a dropdown (`<select>`) control labeled with an i18n
  message (e.g., "Filter by specialty") above the vet table.
- The dropdown shall list, in this order: an **"All"** option (default), one
  option per distinct specialty present in the data (e.g., `dentistry`,
  `radiology`, `surgery`), and a **"No specialty"** option.
- When the user changes the selected option, the system shall reload
  `/vets.html` with the appropriate `?specialty=` value (the specialty **name**
  for a named specialty; a reserved value of `none` for "No specialty"; and no
  `specialty` parameter, or an empty value, for "All").
- When `specialty` matches a known specialty name, the system shall display
  only veterinarians who have that specialty.
- When `specialty` equals the reserved `none` value, the system shall display
  only veterinarians who have zero specialties.
- When `specialty` is absent or empty ("All"), the system shall display all
  veterinarians (current behavior).
- When `specialty` is a value that matches neither a known specialty nor the
  reserved `none` value, the system shall display an empty vet list and the
  dropdown shall fall back to showing "All" as selected (graceful handling of a
  bad/shared URL).
- The dropdown shall indicate the currently active option as selected, based on
  the `specialty` parameter, so the control reflects the current URL.
- Applying or changing the filter shall reset paging to page 1, and the
  pagination links shall carry the active `specialty` parameter so that paging
  preserves the filter.
- The control and its option labels ("Filter by specialty", "All specialties",
  "No specialty") shall use i18n message keys, consistent with the existing
  message-bundle pattern.

**Proof Artifacts:**

- Screenshot: the Vet Directory showing the specialty dropdown with options
  All / dentistry / radiology / surgery / No specialty — demonstrates the
  filter control is present and lists the available specialties.
- Screenshot: the Vet Directory at `/vets.html?specialty=surgery` showing only
  vets with the surgery specialty — demonstrates that selecting a specialty
  filters the list.
- Screenshot: the Vet Directory at `/vets.html?specialty=none` showing only
  vets with no specialty (e.g., James Carter, Sharon Jenkins) — demonstrates
  the "No specialty" option works.

### Unit 2: End-to-end verification of specialty filtering

**Purpose:** Prove, through an automated browser test, that the filter narrows
the list to matching vets and that the filtered view is reproducible via the
`?specialty=` query parameter.

**Functional Requirements:**

- The system shall provide a Playwright E2E test under
  `e2e-tests/tests/features/` that opens the Vet Directory, applies a specialty
  filter via the dropdown, and asserts that only matching vets are shown.
- The test shall assert that applying a filter updates the URL to include the
  expected `?specialty=` value.
- The test shall navigate directly to a filtered URL (e.g.,
  `/vets.html?specialty=surgery`) and assert the same filtered result,
  demonstrating that the filter is shareable via the query parameter.
- The test shall cover the "No specialty" option, asserting that vets with no
  specialty are shown for `?specialty=none`.

**Proof Artifacts:**

- Test: `e2e-tests/tests/features/vet-specialty-filter.spec.ts` passes —
  demonstrates filtering by a named specialty, by "No specialty", and via a
  shared filtered URL.
- Playwright HTML report entry (`e2e-tests/test-results/html-report/`) showing
  the new test passing — demonstrates end-to-end functionality in a real
  browser.

## Non-Goals (Out of Scope)

1. **Multi-select / combined filters**: Filtering by more than one specialty at
   once, or combining specialty with other criteria, is out of scope; the
   filter selects a single value at a time.
2. **Free-text search of vets**: Searching vets by name or other attributes is
   not part of this feature.
3. **Changing the data model or seed data**: The `Vet`/`Specialty` entities,
   the `vet_specialties` join table, and the seed data remain unchanged.
4. **Filtering the JSON endpoint**: The `GET /vets` (`@ResponseBody`) endpoint
   is not modified; only the HTML `GET /vets.html` view gains filtering.
5. **Admin management of specialties**: Creating, editing, or deleting
   specialties is out of scope.
6. **Persisting the filter across sessions**: The filter lives only in the URL;
   no cookie, profile, or database preference is stored.

## Design Considerations

- The filter control is a Bootstrap-styled `<select>` placed in the
  `liatrio-card-header` area of `vets/vetList.html`, above the existing vet
  table, so it is clearly associated with the list.
- Changing the dropdown navigates to the filtered URL. This can be done with a
  minimal inline form (GET method targeting `/vets.html`) so that selecting an
  option and submitting carries the `specialty` parameter; an `onchange`
  auto-submit may be used for convenience but must degrade to a working control
  without JavaScript.
- The currently selected specialty is rendered as the `selected` option so the
  control matches the URL state.
- The control should be keyboard-accessible and labeled via an associated
  `<label>` (using an i18n message) for screen readers.
- When a filter yields no vets, the table renders with no data rows (an empty
  table body); a "no veterinarians" message is optional and not required.

## Repository Standards

- **Web layer**: Follow existing Spring MVC conventions in `VetController`
  (`@GetMapping`, `@RequestParam`, `Model`, returning the `vets/vetList` view).
  Keep the existing `Vets`/pagination model attributes intact.
- **Data access**: Reuse `VetRepository`. Prefer deriving the filtered list and
  the set of available specialties from the already-`@Cacheable` vet data; only
  add a repository query method if needed, following Spring Data naming
  conventions (see `VetRepository` JavaDoc).
- **Templating**: Follow existing Thymeleaf conventions in `vetList.html`
  (`th:text="#{key}"`, `th:href="@{...}"`, `th:each`, `th:selected`).
- **i18n**: Add new message keys to all relevant bundles under
  `src/main/resources/messages/`, mirroring the existing key style (the
  `I18nPropertiesSyncTest` enforces that bundles stay in sync).
- **Testing**: Add MVC/web-layer tests in
  `src/test/java/.../vet/VetControllerTests.java` using `@WebMvcTest` +
  `MockMvc` with a mocked `VetRepository`, per the Testing Guide. Add the E2E
  spec under `e2e-tests/tests/` following the existing Page Object Model.
- **TDD**: Per `CLAUDE.md`, write the failing test first and follow
  Red-Green-Refactor, maintaining >90% coverage for new code.
- **Commits / workflow**: Use Conventional Commits on the feature branch
  `feat/filter-vets-by-specialty` (direct commits to `main` are blocked by the
  `no-direct-commits-to-main` pre-commit hook).

## Technical Considerations

- **Filtering approach**: The dataset is small and `VetRepository.findAll()` is
  `@Cacheable("vets")`. The simplest, cache-friendly approach is to load all
  vets, filter in memory by specialty (or by "has no specialties" for `none`),
  then paginate the filtered list for display. Adding a derived Spring Data
  query (e.g., `findBySpecialtiesNameIgnoreCase`) is an acceptable alternative;
  the choice should keep pagination totals (`totalPages`, `totalItems`)
  consistent with the filtered result set.
- **Available specialty options**: The dropdown's specialty options can be
  derived from the distinct specialty names across all vets (already loaded via
  the cached `findAll()`), sorted alphabetically, avoiding a new repository.
- **Reserved value**: `none` is used as the parameter value for "No specialty".
  Because no seeded specialty is named "none", this sentinel does not collide
  with a real specialty; this assumption should be noted near the handling code.
- **URL building**: Pagination links in `vetList.html` must append the current
  `specialty` parameter so paging preserves the filter; build links from the
  current parameters rather than hard-coding `?page=` only.
- **No new runtime dependencies**: Bootstrap (for the select styling),
  Thymeleaf, and Spring MVC are already present.
- **Regression surface**: Existing `VetControllerTests` and any E2E specs that
  assert on the Vet Directory should be checked, since the controller signature
  and the `vetList.html` markup change.

## Security Considerations

- The `specialty` parameter is user-controlled input reflected into filtering
  logic and into the selector state. It must be compared against the known set
  of specialty names (and the `none` sentinel) and never used to build dynamic
  queries by string concatenation; Spring Data parameter binding or in-memory
  comparison avoids injection risk.
- The parameter value must not be echoed unescaped into the page; Thymeleaf's
  default escaping handles this, and an unrecognized value falls back to "All"
  rather than being reflected as a selected option.
- No credentials, tokens, or sensitive data are involved.
- Proof artifacts are screenshots and test reports of public UI only; nothing
  sensitive should be committed. Follow existing handling of
  `e2e-tests/test-results/` artifacts.

## Success Metrics

1. **Control present**: The specialty dropdown renders on the Vet Directory and
   lists "All", each available specialty, and "No specialty".
2. **Correct filtering**: `/vets.html?specialty=surgery` shows only surgery
   vets; `/vets.html?specialty=none` shows only vets with no specialty; "All"
   (no parameter) shows every vet.
3. **Shareable URLs**: Navigating directly to a `?specialty=` URL reproduces the
   same filtered list (verified by the E2E test).
4. **Pagination preserved**: Page links retain the active `specialty` parameter,
   and changing the filter resets to page 1, with no empty/orphaned pages.
5. **Automated proof**: The new Playwright spec passes in CI, and new web-layer
   tests for the controller pass with >90% coverage on the added code.

## Open Questions

No open questions at this time.
