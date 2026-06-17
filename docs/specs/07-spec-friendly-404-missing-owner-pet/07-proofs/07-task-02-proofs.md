# Task 02 Proofs - Missing pet (and owner) on pet/visit routes returns a friendly 404

## Task Summary

This task extends the friendly 404 handling to the pet and visit routes. A
missing pet (or a missing owner reached through a pet/visit URL) now returns
HTTP 404 and the shared `error` view.

> **Post-rebase reconciliation note:** This branch was rebased onto an updated
> `main` that merged a *delete-pet* feature (#17). That feature intentionally
> treats an unknown pet on `/pets/{petId}/delete` as a soft redirect + flash
> message (not a 404), and it relies on the shared `@ModelAttribute findPet`
> returning `null` for a missing pet. To avoid changing that merged behavior,
> the missing-pet 404 is enforced in the **edit handler**
> (`initUpdateForm`) rather than by making `findPet` throw. The user-facing
> behavior and the proof tests below (edit route → 404) are unchanged.

## What This Task Proves

- A missing owner on a pet route (`GET /owners/{id}/pets/new`) returns HTTP 404
  and the `error` view.
- A missing pet on the edit route (`GET /owners/{id}/pets/{petId}/edit`) returns
  HTTP 404 and the `error` view.
- The visit routes (`GET /owners/{id}/pets/{petId}/visits/new`) return HTTP 404
  for both a missing owner and a missing pet.

## Evidence Summary

- Four new MockMvc tests (2 in `PetControllerTests`, 2 in `VisitControllerTests`)
  failed first (RED, 500 / broken render) then passed after wiring the
  controllers to throw `NotFoundException` (GREEN).
- The full `PetControllerTests` and `VisitControllerTests` classes pass (17
  tests total), confirming no regression.

## Artifact: RED — new pet/visit tests fail before implementation

**What it proves:** The tests exercise real behavior; before the change the
routes did not return 404.

**Why it matters:** Confirms the strict TDD RED phase.

**Command:**

```bash
./mvnw test -Dtest="PetControllerTests#testInitCreationFormOwnerNotFoundReturns404+testInitUpdateFormPetNotFoundReturns404,VisitControllerTests#testInitNewVisitFormOwnerNotFoundReturns404+testInitNewVisitFormPetNotFoundReturns404"
```

**Result summary:** All 4 tests errored (missing owner → `IllegalArgumentException`
→ 500; missing pet → null pet → template processing error).

```text
[ERROR] Tests run: 2 ... PetControllerTests
[ERROR] Tests run: 2 ... VisitControllerTests
[ERROR] Tests run: 4, Failures: 0, Errors: 4, Skipped: 0
[INFO] BUILD FAILURE
```

## Artifact: GREEN — PetControllerTests and VisitControllerTests pass

**What it proves:** After wiring `PetController` (missing pet enforced in
`initUpdateForm`; see reconciliation note) and `VisitController` to
`NotFoundException`, all new tests pass and existing tests remain green.

**Why it matters:** Demonstrates pet/visit 404 handling works with no
regression.

**Command:**

```bash
./mvnw test -Dtest="PetControllerTests,VisitControllerTests"
```

**Result summary:** 17 tests run, 0 failures, 0 errors.

```text
[INFO] Tests run: 0 ... PetControllerTests (parent)
[INFO] Tests run: 5 ... VisitControllerTests
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Reviewer Conclusion

Missing pet and missing owner on the pet/visit routes now return a friendly 404
via the same `NotFoundException` + `GlobalExceptionHandler` path, backed by
automated tests, and the prior null-pet rendering bug is removed.
