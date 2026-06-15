# Task 02 Proofs - Controller rejects past dates and preserves the happy path

## Task Summary

This task proves that the new-visit web flow rejects a past date by re-rendering
the form with a field-level error on `date`, preserves the user's submitted
input on that re-render, and still redirects for valid (today/future) dates. No
controller code change was required — the behavior emerges from the
`@FutureOrPresent` constraint added to `Visit.date` in Task 1, enforced by the
controller's existing `@Valid` + `result.hasErrors()` path.

## What This Task Proves

- A past-date POST returns HTTP 200 and the `pets/createOrUpdateVisitForm` view
  (no redirect), with a field error bound to `visit.date`.
- The re-rendered form preserves the submitted `description` and the rejected
  `date` value, so the user does not lose their input.
- A valid submission (no `date` param → entity default of today) still redirects
  to `redirect:/owners/{ownerId}`, confirming existing behavior is preserved.

## Evidence Summary

- `VisitControllerTests` runs 4 tests with 0 failures, including the new
  `testProcessNewVisitFormPastDateRejected` and the unchanged happy-path test.
- The RED state for the underlying rule was already demonstrated in Task 1 at the
  validator level (a `Visit` with a past date produced no violation before the
  annotation existed), so Task 2 verifies the controller surfaces that rule
  without any controller change.

## Artifact: VisitControllerTests pass (reject + happy path + preserved input)

**What it proves:** The controller blocks past dates, surfaces the error on the
correct field, preserves entered values, and still redirects valid submissions.

**Why it matters:** This is the web-layer evidence that the validation rule
produces the correct user-facing flow end-to-end through Spring MVC.

**Command:**

```bash
./mvnw test -Dtest=VisitControllerTests
```

**Result summary:** 4 tests run, 0 failures, 0 errors — BUILD SUCCESS.

```text
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in ...owner.VisitControllerTests
[INFO] BUILD SUCCESS
```

## Artifact: The new past-date controller test

**What it proves:** The exact assertions covering rejection, the field error on
`date`, and input preservation.

**Why it matters:** It documents the precise behavioral contract a reviewer can
read and re-run.

**Artifact path:** `src/test/java/org/springframework/samples/petclinic/owner/VisitControllerTests.java`

**Result summary:** The test posts a date of `today - 1 day` plus a description,
then asserts the form view, a field error on `date`, and that the bound `visit`
retains both submitted values.

```java
@Test
void testProcessNewVisitFormPastDateRejected() throws Exception {
    String pastDate = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
    String description = "Annual checkup";

    mockMvc
        .perform(post("/owners/{ownerId}/pets/{petId}/visits/new", TEST_OWNER_ID, TEST_PET_ID)
            .param("date", pastDate)
            .param("description", description))
        .andExpect(status().isOk())
        .andExpect(view().name("pets/createOrUpdateVisitForm"))
        .andExpect(model().attributeHasFieldErrors("visit", "date"))
        .andExpect(model().attribute("visit", hasProperty("description", is(description))))
        .andExpect(model().attribute("visit", hasProperty("date", is(LocalDate.now().minusDays(1)))));
}
```

## Reviewer Conclusion

The new-visit controller flow correctly rejects past dates with a preserved-input
form error and still accepts valid dates, verified by a green `VisitControllerTests`
suite — with zero controller code changes, confirming the rule lives cleanly on
the entity.
