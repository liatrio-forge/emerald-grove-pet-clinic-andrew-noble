# Task 02 Proofs - Vets pagination specialty-filter regression coverage

## Task Summary

The Veterinarians list already preserves the `specialty` filter across
pagination (delivered in #12). This task locks that behavior in with explicit
regression tests so a future change cannot silently drop the filter. No
production change was required.

## What This Task Proves

- The `vets/vetList` pagination links carry the active `specialty` value when a
  filtered result set spans multiple pages.
- Requesting a later page with a `specialty` parameter returns the
  specialty-filtered result set (selected specialty and listed vets stay
  filtered).

## Evidence Summary

- New test `testPaginationLinksPreserveSpecialtyFilter` passes, asserting the
  rendered pagination HTML contains `specialty=surgery` (which only appears in
  the pagination link hrefs) and that the control actually renders
  (`totalPages == 2`).
- Pre-existing test `testFilteredResultsPaginateAndPreserveFilterAcrossPages`
  covers the later-page filtered-set requirement at the model level.
- The full `VetControllerTests` suite (11 tests) and the full project suite (80
  tests, 5 skipped) pass.

> Note: As planned, this is regression-only coverage of already-correct
> behavior, so the new test passes on first run rather than going RED first.

## Artifact: Pagination links carry the active specialty

**What it proves:** With 8 surgery vets producing 2 pages, the rendered
pagination links include `specialty=surgery`.

**Why it matters:** This is the link-level guarantee that paging keeps the user
in the filtered vet list.

**Test:** `VetControllerTests#testPaginationLinksPreserveSpecialtyFilter`

**Result summary:** Passing. Asserts `totalPages == 2` and that the response
body contains `specialty=surgery` (present only in pagination hrefs such as
`/vets.html?page=2&specialty=surgery`).

## Artifact: Later page preserves the filtered set

**What it proves:** Requesting `page=2` with `specialty=surgery` keeps
`selectedSpecialty=surgery` and returns only surgery vets.

**Why it matters:** Confirms the filter is preserved end-to-end at the model
level, not just in link text.

**Test:** `VetControllerTests#testFilteredResultsPaginateAndPreserveFilterAcrossPages`
(pre-existing)

**Result summary:** Passing. Asserts page-2 `selectedSpecialty` is `surgery` and
every listed vet has the `surgery` specialty.

## Artifact: Full suite run

**What it proves:** All Vet web-layer tests and the whole project suite pass.

**Why it matters:** Demonstrates the regression coverage is green with no
regressions elsewhere.

**Command:**

```bash
./mvnw test -Dtest=VetControllerTests
./mvnw test
```

**Result summary:** `VetControllerTests` — `Tests run: 11, Failures: 0`. Full
suite — `Tests run: 80, Failures: 0, Errors: 0, Skipped: 5` — BUILD SUCCESS.

## Reviewer Conclusion

The Vets specialty-filter preservation across pagination is now guarded by an
explicit link-level regression test plus the existing model-level test, both
green within a fully passing suite.
