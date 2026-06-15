# Task 02 Proofs - Confirmed delete flow (GET confirm + POST delete)

## Task Summary

This task proves the web layer exposes a two-step, confirmed pet-deletion flow:
a `GET` that renders the confirmation page (with the pet and its visit count) and
a `POST` that removes the pet, persists the change, flashes a success message, and
redirects. The unknown-pet path is handled safely (no data change, error flash,
redirect, no unhandled exception).

## What This Task Proves

- `GET /owners/{ownerId}/pets/{petId}/delete` returns 200, the
  `pets/confirmDeletePet` view, and exposes `pet` and `visitCount`.
- `POST /owners/{ownerId}/pets/{petId}/delete` removes the pet (owner saved
  without it), redirects to the owner page, and sets a success flash `message`.
- Deleting a pet that has visits still succeeds (cascade handled at the JPA layer,
  proven in Task 01).
- An unknown `petId` causes no `save`, sets an error flash, and redirects — no
  crash.

## Evidence Summary

- 4 new `PetControllerTests` tests pass: `testInitDeletePetForm`,
  `testProcessDeletePetSuccess`, `testProcessDeletePetWithVisits`,
  `testProcessDeleteUnknownPet`.
- The success test captures the saved `Owner` and asserts the pet is gone.
- The unknown-pet test verifies `owners.save(...)` is never called.

## Artifact: Web-layer test run

**What it proves:** All four delete-flow paths behave as specified.

**Why it matters:** These are the controller contracts the UI and E2E depend on,
including the safety guard for stale/unknown ids.

**Command:**

```bash
./mvnw test -Dtest=PetControllerTests
```

**Result summary:** All `PetControllerTests` pass (16 total, including the 4 new
delete tests), BUILD SUCCESS.

```text
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Artifact: Controller endpoints

**What it proves:** The GET/POST handlers exist with the documented behavior.

**Why it matters:** Shows the implementation matches the tested contract.

**Artifact path:** `src/main/java/org/springframework/samples/petclinic/owner/PetController.java`

**Result summary:** `initDeletePetForm` returns `pets/confirmDeletePet` and adds
`visitCount`; `processDeletePet` guards on a null pet (error flash + redirect),
otherwise removes the pet, saves the owner, sets a success flash, and redirects.

## Reviewer Conclusion

The confirmed delete flow is fully covered at the web layer: the confirmation page
renders with the needed data, deletion persists and redirects with feedback, and
the unknown-pet path fails safe.
