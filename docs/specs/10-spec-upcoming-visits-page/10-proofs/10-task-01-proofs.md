# Task 01 Proofs - Upcoming-visits query and read-only view model

## Task Summary

This task proves the data-access layer can retrieve all visits whose date falls
within an inclusive window, joined to their pet and owner, and return them as a
read-only `UpcomingVisit` view model ordered by date. Because `Visit` has no
association back to `Pet`/`Owner`, the query is rooted at `Owner` and traverses
`owner -> pets -> visits`, projecting each match through a JPQL constructor
expression.

## What This Task Proves

- `VisitRepository.findUpcomingVisits(start, end)` returns visits inside a known
  window and excludes visits outside it.
- The window is inclusive on both boundary dates.
- Results are ordered by visit date ascending.
- An empty window returns an empty list (no error / null).
- Each `UpcomingVisit` carries owner name, pet name, date, and description.

## Evidence Summary

- `UpcomingVisitsRepositoryTests` runs 4 tests, 0 failures, against the seeded
  H2 sample data (visits dated January 2013).
- The boundary, exclusion, and empty-window cases all pass, confirming the
  `BETWEEN :start AND :end` semantics and ordering.

## Artifact: UpcomingVisitsRepositoryTests passes

**What it proves:** The window query returns the correct visits, respects
inclusive boundaries, orders by date, and returns empty for an empty window.

**Why it matters:** This is the core data capability the Upcoming Visits page
depends on; verifying it against real seeded data (not mocks) confirms the JPQL
traversal and projection actually work.

**Command:**

```bash
./mvnw test -Dtest=UpcomingVisitsRepositoryTests
```

**Result summary:** All 4 tests pass.

```text
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.650 s -- in org.springframework.samples.petclinic.owner.UpcomingVisitsRepositoryTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Artifact: Test cases mapped to requirements

**What it proves:** Each functional requirement of the query has a dedicated
test.

**Why it matters:** Demonstrates traceability from the spec's data-access
requirements to executable verification.

| Test method | Requirement verified |
| --- | --- |
| `shouldReturnVisitsWithinInclusiveWindowOrderedByDate` | In-window results; owner/pet/date/description fields; date ordering |
| `shouldIncludeVisitsOnTheWindowBoundaries` | Inclusive start and end boundaries |
| `shouldExcludeVisitsOutsideTheWindow` | Visits outside window excluded |
| `shouldReturnEmptyListWhenNoVisitsInWindow` | Empty window returns empty list |

## Reviewer Conclusion

The repository query is implemented and verified end-to-end against seeded data:
it returns the right visits for a window, includes boundaries, excludes
out-of-window visits, orders by date, and degrades gracefully to an empty list.
