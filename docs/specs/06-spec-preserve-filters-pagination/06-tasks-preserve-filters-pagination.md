# 06-tasks-preserve-filters-pagination.md

## Relevant Files

| File | Why It Is Relevant |
| --- | --- |
| `src/main/java/org/springframework/samples/petclinic/owner/OwnerController.java` | `addPaginationModel` must add the active `lastName`/`city`/`telephone` values to the model; `processFindForm` passes the normalized filter values into it. |
| `src/main/resources/templates/owners/ownersList.html` | Pagination links must change from hardcoded `@{'/owners?page=N'}` to filter-aware `@{/owners(page=..., lastName=..., city=..., telephone=...)}`, omitting empty params. |
| `src/test/java/org/springframework/samples/petclinic/owner/OwnerControllerTests.java` | Web-layer tests (`@WebMvcTest` + `MockMvc`) asserting filter values are on the model and rendered into pagination links, and that empty params are omitted. |
| `src/main/resources/templates/vets/vetList.html` | Already carries `specialty` on pagination links; reference for the Owners template change and target of the Vets regression assertion. |
| `src/main/java/org/springframework/samples/petclinic/vet/VetController.java` | Already adds `selectedSpecialty` to the model; no production change expected, covered by regression tests. |
| `src/test/java/org/springframework/samples/petclinic/vet/VetControllerTests.java` | Web-layer regression tests asserting Vets pagination links carry `specialty` and a later page stays specialty-filtered. |
| `e2e-tests/tests/features/preserve-filters-pagination.spec.ts` | New Playwright spec: apply an Owners filter, page forward/back, assert filtered results + URL params persist; capture proof screenshot. |
| `e2e-tests/tests/features/vet-specialty-filter.spec.ts` | Existing Vets filter spec; extend or reference to confirm specialty persists while paging. |
| `docs/specs/06-spec-preserve-filters-pagination/06-proofs/img/` | Destination for proof screenshots captured by the Playwright run. |

### Notes

- Java unit/web tests live under `src/test/java/...` mirroring the package of
  the class under test; run them with `./mvnw test` (also enforced by the
  `maven-test-check` pre-commit hook).
- Run a single test class with `./mvnw test -Dtest=OwnerControllerTests`.
- Follow Strict TDD (Red-Green-Refactor): write the failing test first, confirm
  it fails for the right reason, then implement the minimum to pass.
- Use the Arrange-Act-Assert pattern and descriptive test method names.
- E2E specs run from `e2e-tests/` via `npm test`; Playwright auto-starts the app.
- Commits must follow conventional-commit format and may not be made directly to
  `main` (use a feature branch).

## Tasks

### [x] 1.0 Owners list preserves search filters across pagination

#### 1.0 Proof Artifact(s)

- Test: `OwnerControllerTests` cases pass demonstrating the controller adds the
  active `lastName`, `city`, and `telephone` values to the model for the
  `owners/ownersList` view.
- Test: `OwnerControllerTests` case asserts the rendered `ownersList` HTML
  pagination links contain the active filter parameters (e.g.
  `lastName=Franklin`) demonstrating links are filter-aware.
- Test: `OwnerControllerTests` case asserts an unfiltered list renders clean
  `?page=N` pagination links (no empty `lastName=`/`city=`/`telephone=` params)
  demonstrating empty parameters are omitted.

#### 1.0 Tasks

- [x] 1.1 (RED) Add a failing `OwnerControllerTests` test asserting that a
  filtered multi-page search (e.g. `lastName=Franklin`) adds `lastName`, `city`,
  and `telephone` attributes to the model with the active values for the
  `owners/ownersList` view.
- [x] 1.2 (GREEN) Change `OwnerController.addPaginationModel` to accept the
  normalized `lastName`, `city`, and `telephone` values and add them to the
  model; update `processFindForm` to pass them. Confirm 1.1 passes.
- [x] 1.3 (RED) Add a failing `OwnerControllerTests` test asserting the rendered
  `ownersList` HTML pagination links include the active filter parameter (e.g.
  the response body contains `lastName=Franklin`) on numbered/next/last links.
- [x] 1.4 (GREEN) Update `ownersList.html` pagination links to the parameterized
  `@{/owners(page=..., lastName=..., city=..., telephone=...)}` form so active
  filters are carried. Confirm 1.3 passes.
- [x] 1.5 (RED) Add a failing `OwnerControllerTests` test asserting that an
  unfiltered multi-page list renders clean `?page=N` links with no empty
  `lastName=`/`city=`/`telephone=` query parameters.
- [x] 1.6 (GREEN) Make the template omit blank filter parameters (conditional
  inclusion) so unfiltered URLs stay clean. Confirm 1.5 passes.
- [x] 1.7 (REFACTOR) Remove duplication in the template pagination links (e.g.
  factor the shared param list) and tidy the controller signature; ensure all
  `OwnerControllerTests` still pass and existing tests are green.

### [x] 2.0 Vets pagination specialty-filter regression coverage

#### 2.0 Proof Artifact(s)

- Test: `VetControllerTests` case passes demonstrating the `vets/vetList`
  pagination links carry the active `specialty` value across pages.
- Test: `VetControllerTests` case passes demonstrating that requesting a later
  page with a `specialty` parameter returns the specialty-filtered result set.

#### 2.0 Tasks

- [x] 2.1 (RED) Add a failing `VetControllerTests` test that requests
  `/vets.html` with a `specialty` filter producing multiple pages and asserts
  the rendered pagination links contain `specialty=<value>`.
- [x] 2.2 (RED/GREEN) Add a `VetControllerTests` test that requests a later page
  (e.g. `page=2`) with a `specialty` parameter and asserts the model's
  `selectedSpecialty` and `listVets` reflect the specialty-filtered set. Confirm
  it passes against existing production code (regression); if a gap is found,
  implement the minimum fix in `VetController`/`vetList.html`.
- [x] 2.3 (REFACTOR) Ensure the new tests use shared fixtures/AAA structure and
  all `VetControllerTests` pass.

### [ ] 3.0 End-to-end browser proof for filtered pagination

#### 3.0 Proof Artifact(s)

- Playwright: `preserve-filters-pagination.spec.ts` applies an Owners filter
  spanning multiple pages, pages forward and back, and asserts the result set
  stays filtered and the URL carries the filter parameter.
- Screenshot: `docs/specs/06-spec-preserve-filters-pagination/06-proofs/img`
  image of the filtered Owners results page on a later page (with the filtered
  URL visible) demonstrates filter parameters persist in pagination links.
- Playwright/Screenshot: a check that the Vets specialty filter persists while
  paging demonstrates the existing behavior remains intact.

#### 3.0 Tasks

- [ ] 3.1 Identify or seed enough owners with a shared filterable value (e.g. a
  last name prefix or city) to produce >1 page of results (page size is 5);
  document the chosen filter and expected page count.
- [ ] 3.2 Create `e2e-tests/tests/features/preserve-filters-pagination.spec.ts`
  that: searches owners by the chosen filter, verifies multiple pages, clicks
  next/previous (and a numbered/last link), and asserts (a) the URL retains the
  filter parameter and (b) the listed rows remain within the filtered set.
- [ ] 3.3 In the same spec, capture a proof screenshot of a later filtered page
  into `docs/specs/06-spec-preserve-filters-pagination/06-proofs/img/`.
- [ ] 3.4 Add a Vets check (extend `vet-specialty-filter.spec.ts` or add a case)
  that selects a specialty, pages forward, and asserts the `specialty` parameter
  and filtered list persist; capture a screenshot if the specialty produces >1
  page, otherwise assert link parameters directly.
- [ ] 3.5 Run the E2E suite (`cd e2e-tests && npm test`) and confirm the new
  spec(s) pass; verify the screenshot artifacts exist under `06-proofs/img/`.
