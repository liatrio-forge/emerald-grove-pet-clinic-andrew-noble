# Task 01 Proofs - Controller-side specialty filtering and model data

## Task Summary

This task proves that `VetController` accepts an optional `specialty` request
parameter on `GET /vets.html`, filters the veterinarian list accordingly
(named specialty, the `none` sentinel, "All", and invalid values), resets
pagination correctly, and exposes the dropdown's option data and active
selection to the view.

## What This Task Proves

- A named specialty (`?specialty=surgery`) returns only vets with that specialty.
- The `?specialty=none` sentinel returns only vets with zero specialties.
- No parameter (All) returns every vet; an invalid value returns an empty list
  while the dropdown falls back to "All".
- The model exposes the available specialty options (`specialties`) and the
  active `selectedSpecialty`, and a filtered set of >5 vets paginates correctly
  while preserving the filter on page 2.

## Evidence Summary

- `VetControllerTests` (9 tests) passes, covering every filtering rule, the
  edge cases, the model attributes, and multi-page pagination with the filter.
- A live `curl` against `/vets.html?specialty=radiology` returns HTTP 200 with
  only the two radiology vets and the dropdown marking `radiology` selected.

## Artifact: VetControllerTests suite

**What it proves:** The controller's filtering logic and model data are correct
for all cases in the spec, including the invalid-value edge case and
filtered multi-page pagination.

**Why it matters:** These web-layer tests are the primary regression guard for
the filtering behavior and are run by the `maven-test-check` pre-commit hook.

**Command:**

```bash
./mvnw test -Dtest=VetControllerTests
```

**Result summary:** All 9 tests pass (0 failures), including
`testFilterByNamedSpecialtyShowsOnlyMatchingVets`,
`testFilterByNoneShowsOnlyVetsWithoutSpecialties`, `testNoFilterShowsAllVets`,
`testInvalidSpecialtyShowsEmptyListAndFallsBackToAll`,
`testModelExposesAvailableSpecialtyOptionsSortedAndDistinct`, and
`testFilteredResultsPaginateAndPreserveFilterAcrossPages`.

```text
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0 -- in org.springframework.samples.petclinic.vet.VetControllerTests
[INFO] BUILD SUCCESS
```

## Artifact: Live curl of the radiology filter

**What it proves:** The `specialty` parameter works end-to-end against the
running application, not just in unit tests.

**Why it matters:** Confirms the filtered HTML a user/shared-link would receive
contains only matching vets and reflects the active selection.

**Command:**

```bash
curl -s -o /dev/null -w "HTTP %{http_code}\n" "http://localhost:8080/vets.html?specialty=radiology"
curl -s "http://localhost:8080/vets.html?specialty=radiology"   # names + specialty cells extracted below
curl -s "http://localhost:8080/vets.html?specialty=radiology" | grep -oE '<option value="radiology"[^>]*>'
```

**Result summary:** Returns HTTP 200; the table lists only Helen Leary and
Henry Stevens (both radiology); the dropdown marks `radiology` as selected.

```text
HTTP 200

# Vet name + specialty cells in the filtered response:
Name Specialties Helen Leary radiology Henry Stevens radiology

# Dropdown active option:
<option value="radiology" selected="selected">
```

## Artifact: Page-bounds hardening (code-review follow-up)

**What it proves:** A `page` value of 0 or negative is treated as the first page
instead of throwing `IllegalArgumentException` (HTTP 500).

**Why it matters:** A pre-existing edge case (carried over from the original
`findPaginated`) was caught during code review; `Math.max(page, 1)` now guards
the `PageRequest` so hand-typed/crawled `?page=0` URLs render page 1.

**Command:**

```bash
./mvnw test -Dtest=VetControllerTests
```

**Result summary:** `testPageBelowOneIsTreatedAsFirstPage` passes —
`/vets.html?page=0` returns HTTP 200 with `currentPage=1`; all 10 tests green.

```text
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0 -- in ...vet.VetControllerTests
[INFO] BUILD SUCCESS
```

## Reviewer Conclusion

The controller correctly implements specialty filtering with all required edge
cases and pagination behavior, verified by both automated web-layer tests and a
live request against the running app.
