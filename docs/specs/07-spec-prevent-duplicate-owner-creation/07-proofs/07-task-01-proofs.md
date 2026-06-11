# Task 01 Proofs - Repository duplicate-lookup query

## Task Summary

This task proves the data layer can determine whether an owner already exists
matching first name + last name + telephone, comparing case-insensitively and
ignoring surrounding whitespace. This is the foundation for blocking duplicate
owner creation (issue #4).

## What This Task Proves

- A new `OwnerRepository.existsByNameAndTelephone(...)` method reports `true` for
  an existing owner even when the inputs differ in case and have surrounding
  whitespace.
- The same method reports `false` for a first/last/telephone combination that
  does not exist.
- The lookup runs as a parameterized JPQL query against the real (H2) database —
  no string concatenation, injection-safe.

## Evidence Summary

- `./mvnw test -Dtest=ClinicServiceTests` passes with **13 tests, 0 failures**
  (2 new duplicate-lookup tests added to the existing 11).
- The generated SQL uses bound parameters and `lower(trim(...))` on both the
  column and the parameter, confirming case-insensitive, whitespace-trimmed
  matching.

## Artifact: ClinicServiceTests passes (RED → GREEN)

**What it proves:** The duplicate-lookup rule works against a real database
including case/whitespace normalization, and the non-matching case correctly
reports no duplicate.

**Why it matters:** This is the core data-layer behavior every higher layer
(controller, UI) depends on; if the match rule is wrong, the whole feature is
wrong.

**RED evidence:** Before implementing the method, the tests failed to compile
(`cannot find symbol: method existsByNameAndTelephone`), confirming the tests
exercise behavior that did not yet exist.

**Command:**

```bash
./mvnw test -Dtest=ClinicServiceTests
```

**Result summary:** GREEN — all 13 tests pass after adding the repository method.

```text
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.598 s -- in org.springframework.samples.petclinic.service.ClinicServiceTests
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

The new tests:

```java
@Test
void shouldDetectDuplicateOwnerIgnoringCaseAndWhitespace() {
    // George Franklin / 6085551023 is seed owner #1; the lookup must match
    // despite differing case and surrounding whitespace on every field.
    boolean duplicate = this.owners.existsByNameAndTelephone("  george ", "FRANKLIN", " 6085551023 ");
    assertThat(duplicate).isTrue();
}

@Test
void shouldNotDetectDuplicateForNonMatchingOwner() {
    boolean duplicate = this.owners.existsByNameAndTelephone("Nonexistent", "Person", "0000000000");
    assertThat(duplicate).isFalse();
}
```

## Artifact: Parameterized, normalized query

**What it proves:** Matching is case-insensitive and whitespace-trimmed, and the
query is bound (injection-safe), not string-concatenated.

**Why it matters:** Confirms the spec's "case-insensitive, trimmed" requirement
and the security requirement to use bound parameters.

**Artifact path:** `src/main/java/org/springframework/samples/petclinic/owner/OwnerRepository.java`

**Result summary:** The method uses a `@Query` with named bound parameters and
`LOWER(TRIM(...))` on both sides of each comparison.

```java
@Query("""
        SELECT COUNT(o) > 0 FROM Owner o
        WHERE LOWER(TRIM(o.firstName)) = LOWER(TRIM(:firstName))
        AND LOWER(TRIM(o.lastName)) = LOWER(TRIM(:lastName))
        AND LOWER(TRIM(o.telephone)) = LOWER(TRIM(:telephone))
        """)
boolean existsByNameAndTelephone(@Param("firstName") String firstName, @Param("lastName") String lastName,
        @Param("telephone") String telephone);
```

Hibernate-generated SQL observed during the test run (bound `?` parameters,
`lower(trim(...))` on the column and the input):

```sql
select count(o1_0.id)>0 from owners o1_0
where lower(trim(BOTH from o1_0.first_name))=lower(trim(BOTH from ?))
and lower(trim(BOTH from o1_0.last_name))=lower(trim(BOTH from ?))
and lower(trim(BOTH from o1_0.telephone))=lower(trim(BOTH from ?))
```

## Reviewer Conclusion

The data layer correctly and safely detects duplicate owners by first name, last
name, and telephone with case-insensitive, whitespace-trimmed matching, proven by
two passing tests against a real database and the generated parameterized SQL.
This unblocks the controller-level rule in Task 2.0.
