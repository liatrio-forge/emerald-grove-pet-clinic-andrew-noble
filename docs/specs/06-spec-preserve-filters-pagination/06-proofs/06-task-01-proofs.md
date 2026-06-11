# Task 01 Proofs - Owners list preserves search filters across pagination

## Task Summary

This task makes the Owners results page carry the active search criteria
(`lastName`, `city`, `telephone`) on every pagination link, so paging through a
filtered search no longer resets to "all owners". Empty criteria are omitted so
an unfiltered list keeps clean `?page=N` links.

## What This Task Proves

- The controller exposes the active filter as a `filterParams` query-string
  suffix to the `owners/ownersList` view.
- Rendered pagination links include the active filter (e.g. `lastName=Franklin`).
- An unfiltered list renders clean `?page=N` links with no empty
  `lastName=`/`city=`/`telephone=` parameters.

## Evidence Summary

- Three targeted `OwnerControllerTests` cases pass, each asserting one of the
  behaviors above.
- The full Owner web-layer suite (22 tests) and the full project suite (79
  tests, 5 skipped) pass, confirming no regressions.

## Artifact: Controller exposes active filter to the view

**What it proves:** `processFindForm` adds a `filterParams` model attribute
containing the active, URL-encoded criteria for the results view.

**Why it matters:** The template can only build filter-aware links if the
controller passes the active filter to the view.

**Test:** `OwnerControllerTests#testProcessFindFormAddsActiveFilterAttributesToModel`

**Result summary:** Passing. Asserts `filterParams` contains `lastName=Franklin`,
`city=Madison`, and `telephone=6085551023` when those criteria are searched.

## Artifact: Pagination links include the active filter

**What it proves:** When a filtered search spans multiple pages, the rendered
`ownersList` HTML pagination links contain the active filter parameter.

**Why it matters:** This is the core behavior — paging keeps the user inside
their filtered result set.

**Test:** `OwnerControllerTests#testOwnersListPaginationLinksIncludeActiveFilter`

**Result summary:** Passing. The rendered response body contains
`lastName=Franklin` on the pagination links (e.g. `/owners?page=2&lastName=Franklin`).

## Artifact: Unfiltered links stay clean

**What it proves:** With no active filter, pagination links contain no empty
filter parameters.

**Why it matters:** Keeps shareable/bookmarkable URLs clean and avoids noise for
the common unfiltered case.

**Test:** `OwnerControllerTests#testOwnersListPaginationLinksOmitEmptyFilters`

**Result summary:** Passing. The rendered response body contains no `lastName=`,
`city=`, or `telephone=` parameters (links are `/owners?page=N`).

## Artifact: Targeted and full suite runs

**What it proves:** The three new behaviors pass together and the whole suite
remains green.

**Why it matters:** Demonstrates the implementation works and introduces no
regressions, satisfying the repository quality gate (`./mvnw test`).

**Command:**

```bash
./mvnw test -Dtest='OwnerControllerTests#testProcessFindFormAddsActiveFilterAttributesToModel+testOwnersListPaginationLinksIncludeActiveFilter+testOwnersListPaginationLinksOmitEmptyFilters'
```

**Result summary:** `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0` — BUILD SUCCESS.

```text
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in OwnerControllerTests
[INFO] BUILD SUCCESS
```

**Full suite:**

```bash
./mvnw test
```

**Result summary:** `Tests run: 79, Failures: 0, Errors: 0, Skipped: 5` — BUILD SUCCESS.

## Reviewer Conclusion

The Owners list now preserves the active search filter across all pagination
links and omits empty parameters when unfiltered, proven by three focused tests
and a fully green suite with no regressions.
