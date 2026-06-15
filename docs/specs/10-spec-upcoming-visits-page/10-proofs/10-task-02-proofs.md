# Task 02 Proofs - Upcoming Visits page (controller + view)

## Task Summary

This task proves the read-only Upcoming Visits page is served at
`GET /visits/upcoming`, rendered with the shared layout, and driven by an
optional `days` query parameter (default 7). It computes an inclusive
today→today+days window, falls back to 7 for missing/invalid/non-positive
input, lists owner/pet/date/description per visit, and shows an empty-state when
nothing matches.

## What This Task Proves

- `GET /visits/upcoming` returns HTTP 200 and the `visits/upcomingVisits` view.
- The window defaults to 7 days when `days` is absent.
- A provided `days` value widens the window (verified via the dates passed to the
  repository).
- Missing, non-numeric, and non-positive `days` values fall back to 7 without
  error.
- The page lists owner, pet, date, and description, and renders an empty-state
  when there are no visits.

## Evidence Summary

- `UpcomingVisitsControllerTests` runs 6 tests, 0 failures.
- Live app: `/visits/upcoming` renders the page; after creating a today-dated
  visit it appears in the table; a 20-day-out visit is excluded at `days=7` and
  included at `days=30`.

## Artifact: UpcomingVisitsControllerTests passes

**What it proves:** All controller behaviors — view name, default/custom/invalid
`days`, model attributes, and the empty-state path — work as specified.

**Why it matters:** These are fast, isolated web-layer assertions covering every
branch of the `days` handling and the model contract the view depends on.

**Command:**

```bash
./mvnw test -Dtest=UpcomingVisitsControllerTests
```

**Result summary:** All 6 tests pass.

```text
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.667 s -- in org.springframework.samples.petclinic.owner.UpcomingVisitsControllerTests
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

| Test method | Behavior verified |
| --- | --- |
| `shouldReturnUpcomingVisitsView` | 200, view name, `upcomingVisits` + `days=7` model attrs |
| `shouldDefaultToSevenDayWindowWhenDaysAbsent` | Window is 7 days from today |
| `shouldUseProvidedDaysWindow` | `days=30` widens the window to 30 days |
| `shouldFallBackToDefaultWhenDaysIsNonNumeric` | `days=abc` → 7 |
| `shouldFallBackToDefaultWhenDaysIsNotPositive` | `days=0` and `days=-5` → 7 |
| `shouldRenderSuccessfullyWhenNoUpcomingVisits` | Empty list still returns 200 |

## Artifact: Live page renders and lists a created visit

**What it proves:** The page is reachable on the running application and shows a
real visit created within the window.

**Why it matters:** Confirms the controller, query, and template work together
against a live H2 database, not just under mocks.

**Commands:**

```bash
# create a visit dated today (2026-06-15) for owner 1 (George Franklin) / pet 1 (Leo)
curl -X POST http://localhost:8080/owners/1/pets/1/visits/new \
  --data-urlencode "date=2026-06-15" --data-urlencode "description=Annual checkup"   # -> 302

curl -s http://localhost:8080/visits/upcoming
```

**Result summary:** The page renders the Owner/Pet/Date/Description table with the
new visit. (Message keys show as `??upcomingVisits.*??` here because the i18n keys
are added in Task 3.0; the structure and data binding are correct.)

```html
<thead>
  <th>Owner</th>
  <th>Pet</th>
  <th>Date</th>
  <th>Description</th>
</thead>
...
<a href="/owners/1">George Franklin</a>
<td>Leo</td>
<td>Annual checkup</td>
```

## Artifact: `days` window filtering is honored live

**What it proves:** The `days` parameter actually controls which visits appear.

**Why it matters:** This is the page's core configurable behavior and a stated
acceptance criterion.

**Commands & result summary:** A visit dated 2026-07-05 (~20 days out) is absent
from the default 7-day window and present at `days=30`.

```text
--- days=7  (default): "Future dental" rows: 0   (correctly excluded)
--- days=30          : "Future dental" rows: 1   (correctly included)
```

## Reviewer Conclusion

The Upcoming Visits page is implemented and verified at both the web layer
(`@WebMvcTest`, all branches) and end-to-end against a live server: it renders,
lists visits with owner/pet/date/description, honors the `days` window, and
degrades gracefully. Remaining i18n strings are completed in Task 3.0.
