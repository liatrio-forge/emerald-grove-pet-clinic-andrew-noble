# Task 01 Proofs - Remove a pet and cascade-delete its visits

## Task Summary

This task proves the domain and persistence layer can remove a pet from its owner
and have the pet (and all of its visits) deleted from the database, with no
orphaned rows left behind. It adds `Owner.removePet(Pet)` and enables
`orphanRemoval = true` on the owner→pets mapping; the existing `CascadeType.ALL`
on `Pet.visits` removes the visit rows.

## What This Task Proves

- `Owner.removePet` removes the target pet from `owner.getPets()` while leaving
  other pets intact (unit-level).
- Removing a pet that has visits and saving the owner deletes both the pet row and
  its visit rows at the database level (integration-level, against H2).
- No orphaned visit rows remain after deletion.

## Evidence Summary

- `OwnerTests#shouldRemovePetFromOwner` passes (unit).
- `ClinicServiceTests#shouldDeletePetAndCascadeItsVisitsWhenRemovedFromOwner`
  passes (`@DataJpaTest`): visit count for the deleted pet goes from >0 to 0.
- Hibernate SQL during the test shows `delete from visits` (x2) followed by
  `delete from pets`, confirming the cascade.

## Artifact: Domain + persistence test run

**What it proves:** Both the unit behavior of `removePet` and the end-to-end
cascade deletion work.

**Why it matters:** This is the riskiest change (a JPA mapping change); proving it
at the DB level guards against orphaned data.

**Command:**

```bash
./mvnw test -Dtest=OwnerTests,ClinicServiceTests
```

**Result summary:** 15 tests pass (1 `OwnerTests` + 14 `ClinicServiceTests`),
BUILD SUCCESS.

```text
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in ...owner.OwnerTests
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0 -- in ...service.ClinicServiceTests
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Artifact: Cascade SQL trace

**What it proves:** Removing the pet from the owner and saving issues DELETEs for
the pet's visit rows and then the pet row.

**Why it matters:** It demonstrates the cascade is performed by the persistence
layer, not just the in-memory collection.

**Result summary:** The native count query `SELECT COUNT(*) FROM visits WHERE
pet_id = 7` returns a positive value before deletion and `0` after; the
intervening SQL deletes the two visits and the pet.

```text
Hibernate: SELECT COUNT(*) FROM visits WHERE pet_id = 7
Hibernate: delete from visits where id=?
Hibernate: delete from visits where id=?
Hibernate: delete from pets where id=?
Hibernate: SELECT COUNT(*) FROM visits WHERE pet_id = 7   -- returns 0
```

## Reviewer Conclusion

The aggregate method and `orphanRemoval` mapping correctly delete a pet and its
visits with zero orphaned rows, verified by both a unit test and a database-level
integration test.
