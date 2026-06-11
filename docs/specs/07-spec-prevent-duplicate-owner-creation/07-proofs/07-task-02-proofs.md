# Task 02 Proofs - Controller duplicate-detection rule and UI error

## Task Summary

This task proves the owner creation flow blocks a duplicate owner (same first
name + last name + telephone), surfaces a clear field-level error on `lastName`,
preserves the user's input, and does not persist a second record — all wired into
`OwnerController.processCreationForm` and reusing the existing `duplicate`
message.

## What This Task Proves

- A duplicate submission re-renders the creation form (HTTP 200, not a redirect)
  with a `lastName` field error whose code is `duplicate`.
- No owner is saved on the duplicate path (`save` is never called).
- The non-duplicate happy path still redirects and saves (existing behavior
  preserved).
- The duplicate check runs only after standard field validation.
- The `duplicate` message is internationalized: present in the base bundle and
  every translated bundle (English resolves via fallback), and the i18n sync gate
  passes.

## Evidence Summary

- `./mvnw test -Dtest=OwnerControllerTests` passes with **23 tests, 0 failures**
  (1 new duplicate-path test; existing happy-path test unchanged and green).
- `./mvnw test -Dtest=I18nPropertiesSyncTest` passes with **2 tests, 0 failures**.
- The controller places the duplicate check immediately after the
  `result.hasErrors()` guard and returns the form view without saving.

## Artifact: OwnerControllerTests passes (RED → GREEN)

**What it proves:** The duplicate submission is blocked on the correct field with
the correct error code and no save occurs, while the happy path is preserved.

**Why it matters:** This is the user-facing behavior the issue requires — the
duplicate is blocked, the error is actionable, and no phantom record is created.

**RED evidence:** Before the controller change, the new test failed because the
controller saved and redirected:

```text
java.lang.AssertionError: Status expected:<200> but was:<302>
  at OwnerControllerTests.testProcessCreationFormRejectsDuplicate(OwnerControllerTests.java:137)
```

**Command:**

```bash
./mvnw test -Dtest=OwnerControllerTests
```

**Result summary:** GREEN — all 23 tests pass after wiring the duplicate check.

```text
[INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 6.583 s -- in org.springframework.samples.petclinic.owner.OwnerControllerTests
[INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

The new test:

```java
@Test
void testProcessCreationFormRejectsDuplicate() throws Exception {
    given(this.owners.existsByNameAndTelephone(anyString(), anyString(), anyString())).willReturn(true);

    mockMvc
        .perform(post("/owners/new").param("firstName", "George")
            .param("lastName", "Franklin")
            .param("address", "110 W. Liberty St.")
            .param("city", "Madison")
            .param("telephone", "6085551023"))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("owner", "lastName"))
        .andExpect(model().attributeHasFieldErrorCode("owner", "lastName", "duplicate"))
        .andExpect(view().name("owners/createOrUpdateOwnerForm"));

    verify(this.owners, never()).save(any(Owner.class));
}
```

## Artifact: Duplicate check placed after validation, no save

**What it proves:** The check runs only after `@Valid` field validation and
returns the form view without persisting on a duplicate.

**Why it matters:** Confirms the spec requirement that invalid input yields normal
validation errors (not a misleading duplicate error) and that duplicates are not
saved.

**Artifact path:** `src/main/java/org/springframework/samples/petclinic/owner/OwnerController.java`

**Result summary:** The duplicate guard sits directly after `result.hasErrors()`,
rejects `lastName` with code `duplicate`, and returns the form view.

```java
if (result.hasErrors()) {
    redirectAttributes.addFlashAttribute("error", "There was an error in creating the owner.");
    return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
}

// block creating an owner that duplicates an existing one on first name,
// last name, and telephone (matched case-insensitively and trimmed)
if (this.owners.existsByNameAndTelephone(owner.getFirstName(), owner.getLastName(), owner.getTelephone())) {
    result.rejectValue("lastName", "duplicate", "is already in use");
    redirectAttributes.addFlashAttribute("error", "An owner with the same name and telephone already exists.");
    return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
}

this.owners.save(owner);
```

## Artifact: i18n message parity

**What it proves:** The `duplicate` message resolves for all locales and the
repository's i18n sync gate passes.

**Why it matters:** The repo enforces no partial translations; reusing `duplicate`
keeps the error internationalized without new keys.

**Command:**

```bash
./mvnw test -Dtest=I18nPropertiesSyncTest
```

**Result summary:** PASS. `messages.properties` (base/English) defines
`duplicate=is already in use`; de/es/fa/ko/pt/ru/tr all define a translation.
`messages_en.properties` is intentionally excluded from the sync check (English
uses base-bundle fallback, per the test's own logic).

```text
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in org.springframework.samples.petclinic.system.I18nPropertiesSyncTest
[INFO] BUILD SUCCESS
```

## Artifact: Field error rendering (no template change)

**What it proves:** The creation form displays the `lastName` error inline and
preserves submitted input.

**Why it matters:** Confirms the actionable, input-preserving UX requirement is
met by the existing shared fragment without new UI code.

**Artifact path:** `src/main/resources/templates/fragments/inputField.html`

**Result summary:** The shared `inputField` fragment renders
`th:errors="*{lastName}"` (showing "is already in use") and binds the value via
`th:field`, so the rejected field shows the error and retains the user's input.

## Reviewer Conclusion

The controller now blocks duplicate owner creation after validation, surfaces an
internationalized field error on `lastName`, preserves input, and never saves a
duplicate — all proven by a RED→GREEN controller test, a passing i18n gate, and
the existing field-error rendering fragment. The end-to-end browser proof follows
in Task 3.0.
