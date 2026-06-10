# Task 01 Proofs - Repository combined search query (data layer)

## Task Summary

This task proves the `OwnerRepository` can filter owners by last name, city, and
telephone together (AND), with blank criteria ignored, while preserving the
existing last-name query.

## What This Task Proves

- A new `findByOptionalCriteria(lastName, city, telephone, pageable)` query filters
  by city starts-with, telephone exact match, and last name together.
- Blank parameters are ignored, so an all-blank call returns every owner.
- The original `findByLastNameStartingWith` behavior is unchanged.

## Evidence Summary

- `ClinicServiceTests` passes (11 tests), including the new
  `shouldFindOwnersByCityAndTelephone` and the preserved `shouldFindOwnersByLastName`.

## Artifact: ClinicServiceTests data-layer suite

**What it proves:** The combined query returns the correct owners against the H2
seed data for city, telephone, combined, and all-blank cases.

**Why it matters:** This is the authoritative proof that filtering is correct at
the data-access layer, independent of the web layer.

**Command:**

```bash
./mvnw test -Dtest=ClinicServiceTests
```

**Result summary:** 11 tests pass. `shouldFindOwnersByCityAndTelephone` asserts
Madison → 4 owners, telephone `6085551023` → only Franklin, `Franklin`+`Madison`
→ Franklin, and all-blank → all 10 owners. `shouldFindOwnersByLastName` still
passes.

```text
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0 -- in org.springframework.samples.petclinic.service.ClinicServiceTests
[INFO] BUILD SUCCESS
```

## Reviewer Conclusion

The combined optional-criteria query is correct and the original last-name query
is preserved, both verified by `@DataJpaTest` integration tests against seed data.
