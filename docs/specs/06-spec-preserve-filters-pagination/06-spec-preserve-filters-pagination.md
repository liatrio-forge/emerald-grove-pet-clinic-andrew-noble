# 06-spec-preserve-filters-pagination.md

## Introduction/Overview

When a user searches the Owners list (by last name, city, or telephone) and the
results span multiple pages, clicking a pagination link currently drops the
search criteria and reverts to showing all owners. This feature makes
pagination links carry the active filter parameters so that paging
forward/backward keeps the user inside their filtered result set. The
Veterinarians list already preserves its `specialty` filter across pages
(delivered in #12); this spec brings the Owners list to parity and adds
regression coverage so the Vets behavior cannot silently break.

## Goals

- Preserve the active Owners search criteria (`lastName`, `city`, `telephone`)
  on every pagination link of the Owners results page.
- Ensure navigating to any page (first/previous/next/last/numbered) keeps the
  filtered result set and never resets to "all owners".
- Lock in the existing Vets `specialty` filter-preservation behavior with
  explicit regression coverage.
- Keep the change scoped to the existing list pages with no new endpoints,
  parameters, or UI controls.

## User Stories

- **As a clinic receptionist**, I want to page through a filtered list of owners
  so that I can review every matching record without re-entering my search each
  time I change pages.
- **As a clinic staff member**, I want a pagination link I copy or bookmark to
  include my current filter so that reopening it shows the same filtered list.
- **As a clinic staff member viewing the vet directory**, I want paging through a
  specialty-filtered list to keep that specialty applied so that my view stays
  consistent.

## Demoable Units of Work

### Unit 1: Owners pagination preserves search criteria

**Purpose:** Make the Owners results page keep `lastName`, `city`, and
`telephone` filters across all pagination links, serving staff who search owners
and need to page through results.

**Functional Requirements:**

- The system shall expose the active `lastName`, `city`, and `telephone` filter
  values to the Owners results view so the template can build pagination links.
- The system shall include the active filter values as query parameters on every
  Owners pagination link (numbered pages, first, previous, next, last).
- The system shall return the same filtered result set when a pagination link
  carrying filter parameters is requested directly (e.g., a copied/bookmarked
  URL), rather than resetting to all owners.
- The system shall omit empty filter parameters from generated pagination URLs
  so that an unfiltered list keeps clean `?page=N` links.

**Proof Artifacts:**

- Test: `OwnerControllerTests` cases pass demonstrating the controller adds the
  active filter values to the model for the results view.
- Playwright: a spec that applies an Owners filter spanning multiple pages, pages
  forward and back, and asserts the result set stays filtered demonstrates the
  end-to-end behavior.
- Screenshot: Owners results page after paging while filtered, with the
  pagination URL visible/recorded, demonstrates filter parameters persist in the
  link.

### Unit 2: Vets pagination filter-preservation regression coverage

**Purpose:** Protect the already-working Vets `specialty` preservation behavior
with explicit tests so future changes cannot silently drop it.

**Functional Requirements:**

- The system shall continue to include the active `specialty` value on every
  Vets pagination link.
- The test suite shall include a case proving that a Vets pagination link, when
  followed, preserves the selected specialty and its filtered result set.

**Proof Artifacts:**

- Test: a `VetControllerTests` and/or Playwright case passes demonstrating Vets
  pagination preserves the `specialty` filter across pages.

## Non-Goals (Out of Scope)

1. **No new filter fields or search controls**: This spec only preserves the
   existing Owners and Vets filters; it does not add new criteria.
2. **No changes to page size, sorting, or pagination layout**: The visual
   pagination control and page size remain unchanged.
3. **No changes to the JSON `/vets` endpoint**: Only the HTML list views are in
   scope.
4. **No filter UI on the Owners results page itself**: Owners are filtered via
   the existing `owners/find` form; this spec does not add an inline filter
   widget to `ownersList.html`.

## Design Considerations

No new visual design is required. Pagination links keep their existing
appearance and position; only the URLs they point to gain the active filter
parameters. The Owners results page continues to render exactly as today aside
from the link targets.

## Repository Standards

- **Strict TDD (Red-Green-Refactor)**: Write failing tests before implementation
  per `CLAUDE.md` and `docs/DEVELOPMENT.md`.
- **Web-layer testing**: Use `@WebMvcTest` with `@MockitoBean` and `MockMvc`,
  following `OwnerControllerTests` / `VetControllerTests` patterns.
- **Thymeleaf URL building**: Use the `@{...(param=...)}` link syntax (already
  used in `vetList.html`) rather than hand-concatenated query strings.
- **E2E proof**: Add Playwright coverage under `e2e-tests/tests/features/`
  following existing specs (e.g., `vet-specialty-filter.spec.ts`), and store
  proof screenshots under `docs/specs/06-spec-preserve-filters-pagination/06-proofs/img`.
- **Conventional commits** and the no-direct-commits-to-main / pre-commit
  workflow described in `docs/PRECOMMIT.md`.

## Technical Considerations

- **Owners controller**: `addPaginationModel` (in `OwnerController`) must receive
  and add the active `lastName`, `city`, and `telephone` values to the model so
  the view can build filter-aware links. Pass the normalized (non-null) filter
  values already computed in `processFindForm`.
- **Owners template**: Update `ownersList.html` pagination links from the
  hardcoded `@{'/owners?page=N'}` form to the parameterized
  `@{/owners(page=..., lastName=..., city=..., telephone=...)}` form, mirroring
  how `vetList.html` builds links with `specialty`.
- **Empty-parameter handling**: Prefer not emitting blank filter parameters
  (e.g., conditionally include a param only when its value is non-empty) so an
  unfiltered list keeps `?page=N` URLs clean. If a fully conditional approach is
  impractical in Thymeleaf, emitting empty params is acceptable since the
  controller already treats blank criteria as "no filter"; the chosen approach
  must be covered by tests.
- **Vets**: No production change expected; verify behavior with tests. If a gap
  is found, fix it within this spec's scope.
- **No new dependencies** are required.

## Security Considerations

No specific security considerations identified. The filter values are
non-sensitive search criteria already accepted by the existing `GET /owners`
and `GET /vets.html` endpoints, and they are rendered through Thymeleaf's
context-aware escaping in URL parameters. No credentials, tokens, or private
data are introduced. Proof screenshots show only sample/seed data.

## Success Metrics

1. **Filter persistence**: 100% of Owners pagination links (numbered, first,
   previous, next, last) include the active non-empty filter parameters.
2. **No filter reset**: Following any Owners pagination link while filtered
   returns the filtered result set, verified by an automated test and a
   Playwright journey.
3. **Vets regression locked**: An automated test proves Vets pagination preserves
   the `specialty` filter.
4. **Quality gates**: All existing and new tests pass; new-code coverage meets
   the project's ≥90% line standard.

## Open Questions

No open questions at this time.
