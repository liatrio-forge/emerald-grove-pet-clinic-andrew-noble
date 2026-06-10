# Task 02 Proofs - Controller: optional criteria, telephone validation, preserved behaviors

## Task Summary

This task proves `OwnerController.processFindForm` filters owners by the provided
last name / city / telephone (AND), validates telephone, and preserves the
existing zero/one/many-result and empty-search behaviors.

## What This Task Proves

- Search by telephone (exact) and by city (starts-with) works, and combined
  criteria narrow with AND.
- An invalid telephone is rejected with a `telephone` field error
  (`telephone.invalid`) and no search runs.
- The original last-name, empty-search, single-redirect, multi-list, and
  not-found behaviors are unchanged.

## Evidence Summary

- `OwnerControllerTests` passes (18 tests), including new telephone/city/combined
  and invalid-telephone cases, and the updated existing find-form cases.
- A live `curl` against the running app confirms telephone exact-match redirects,
  city filtering returns matching owners, and invalid telephone is rejected.

## Artifact: OwnerControllerTests web-layer suite

**What it proves:** The controller's binding, validation, and result handling are
correct, including the "search not run on invalid telephone" guarantee.

**Why it matters:** Primary regression guard for the find flow; runs in the
`maven-test-check` pre-commit hook.

**Command:**

```bash
./mvnw test -Dtest=OwnerControllerTests
```

**Result summary:** 18 tests pass, including `testProcessFindFormByTelephone`,
`testProcessFindFormByCity`, `testProcessFindFormByCityAndLastName`, and
`testProcessFindFormInvalidTelephoneRejected` (which also `verify`s the query is
never called).

```text
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0 -- in org.springframework.samples.petclinic.owner.OwnerControllerTests
[INFO] BUILD SUCCESS
```

## Artifact: Live curl of telephone and city search

**What it proves:** The search parameters work end-to-end against the running app.

**Why it matters:** Confirms real HTTP behavior (redirect on unique match,
filtered list, validation) beyond unit tests.

**Commands & result summary:**

- `?telephone=6085551023` (George Franklin's number) → **302 redirect** to the
  owner's details page.
- `?city=Madison` → **HTTP 200** list containing the four Madison owners
  (Franklin, McTavish, Escobito, Schroeder); non-Madison Davis is absent.
- `?telephone=abc` → re-renders the find form showing
  "Telephone must be a 10-digit number".

```text
$ curl -s -o /dev/null -w "HTTP %{http_code} -> %{redirect_url}\n" "http://localhost:8080/owners?telephone=6085551023"
HTTP 302 -> http://localhost:8080/owners/1

$ curl -s -o /dev/null -w "HTTP %{http_code}\n" "http://localhost:8080/owners?city=Madison"
HTTP 200
# Madison owners present: Franklin, McTavish, Escobito, Schroeder ; Davis absent

$ curl -s "http://localhost:8080/owners?telephone=abc" | grep -o "Telephone must be a 10-digit number"
Telephone must be a 10-digit number
```

## Artifact: Page-bounds hardening (code-review follow-up)

**What it proves:** A `page` value of 0 or negative is treated as the first page
instead of throwing `IllegalArgumentException` (HTTP 500).

**Why it matters:** A pre-existing edge case (carried over from the original
`findPaginatedForOwnersLastName`) was caught during code review; `Math.max(page, 1)`
now guards the `PageRequest`, consistent with the same fix applied in spec 04's
`VetController`.

**Command:**

```bash
./mvnw test -Dtest=OwnerControllerTests
```

**Result summary:** `testProcessFindFormPageBelowOneIsTreatedAsFirstPage` passes —
`/owners?page=0` returns HTTP 200 with `currentPage=1`; all 19 tests green.

```text
[INFO] Tests run: 19, Failures: 0, Errors: 0, Skipped: 0 -- in ...owner.OwnerControllerTests
[INFO] BUILD SUCCESS
```

## Reviewer Conclusion

The controller correctly filters by optional criteria, validates telephone before
searching, and preserves all prior behaviors — verified by web-layer tests and a
live request against the running app.
