# 07-tasks-friendly-404-missing-owner-pet.md

Tasks for implementing
[`07-spec-friendly-404-missing-owner-pet.md`](07-spec-friendly-404-missing-owner-pet.md).

## Relevant Files

| File | Why It Is Relevant |
| --- | --- |
| `src/main/java/org/springframework/samples/petclinic/system/NotFoundException.java` | New domain exception representing a missing resource (owner/pet). |
| `src/main/java/org/springframework/samples/petclinic/system/GlobalExceptionHandler.java` | New `@ControllerAdvice` mapping `NotFoundException` to a 404 `error` view. |
| `src/main/java/org/springframework/samples/petclinic/owner/OwnerController.java` | Throws not-found for missing owner in `findOwner` and `showOwner`. |
| `src/main/java/org/springframework/samples/petclinic/owner/PetController.java` | Throws not-found for missing owner and missing pet (`findOwner`, `findPet`). |
| `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java` | Throws not-found for missing owner/pet in `loadPetWithVisit`. |
| `src/main/resources/templates/error.html` | Friendly error page; gains a localized "Find Owners" recovery link. |
| `src/test/java/org/springframework/samples/petclinic/owner/OwnerControllerTests.java` | MockMvc tests asserting 404 + `error` view for missing owner. |
| `src/test/java/org/springframework/samples/petclinic/owner/PetControllerTests.java` | MockMvc tests asserting 404 + `error` view for missing pet/owner. |
| `src/test/java/org/springframework/samples/petclinic/owner/VisitControllerTests.java` | MockMvc tests asserting 404 + `error` view on visit routes. |
| `src/test/java/org/springframework/samples/petclinic/system/I18nPropertiesSyncTest.java` | Existing guard; must stay green after the `error.html` change (run, not edited). |
| `e2e-tests/tests/features/not-found.spec.ts` | New Playwright spec: owner 404, pet 404, recovery link, screenshot. |

### Notes

- **Strict TDD**: every code sub-task is preceded by a failing-test sub-task
  (RED), then minimal code to pass (GREEN), then refactor while green.
- Java web-layer tests use `@WebMvcTest` + MockMvc with `@MockitoBean`
  repositories, mirroring existing `*ControllerTests`.
- Run a single Java test class with
  `./mvnw test -Dtest=OwnerControllerTests`; run the whole suite with
  `./mvnw test`.
- Run Playwright from `e2e-tests/` with `npx playwright test not-found.spec.ts`
  (the config auto-starts the app via Maven, or reuses a running instance).
- Reuse existing i18n keys (`error.404`, `findOwners`); do **not** add new
  message keys (keeps `I18nPropertiesSyncTest` green with no translation work).
- Apply `./mvnw spring-javaformat:apply` before committing; the pre-commit
  `maven-test-check` hook runs the full suite, and `no-direct-commits-to-main`
  requires a feature branch.

## Tasks

### [x] 1.0 Missing owner returns a friendly 404 (exception + global handler + owner controller)

#### 1.0 Proof Artifact(s)

- Test: `./mvnw test -Dtest=OwnerControllerTests` passes, including
  `testShowOwnerNotFoundReturns404` and `testInitUpdateOwnerFormNotFoundReturns404`,
  demonstrates missing-owner requests return HTTP 404 and the `error` view
  (satisfies Unit 1 functional requirements).
- Diff: new `system/NotFoundException.java` and `system/GlobalExceptionHandler.java`
  show a domain not-found exception mapped to a 404 `error` view with no raw
  exception message passed to the model (demonstrates no internal-detail leakage).
- Test: `not-found.spec.ts` check (d) asserts the not-found page body excludes
  the raw exception text (demonstrates Unit 1 FR "shall not include stack traces
  or internal exception text" — see Task 4.1).

#### 1.0 Tasks

- [x] 1.1 (RED) In `OwnerControllerTests`, add `testShowOwnerNotFoundReturns404`
  (`GET /owners/999999`) and `testInitUpdateOwnerFormNotFoundReturns404`
  (`GET /owners/999999/edit`), each asserting `status().isNotFound()` and
  `view().name("error")`. Run `./mvnw test -Dtest=OwnerControllerTests` and
  confirm they fail (currently 500).
- [x] 1.2 (GREEN) Create `system/NotFoundException.java` as a `RuntimeException`
  with a message constructor.
- [x] 1.3 (GREEN) Create `system/GlobalExceptionHandler.java` annotated
  `@ControllerAdvice` with an `@ExceptionHandler(NotFoundException.class)` method
  annotated `@ResponseStatus(HttpStatus.NOT_FOUND)` that returns
  `ModelAndView("error")` with `status = 404` and does **not** add the exception
  message to the model.
- [x] 1.4 (GREEN) In `OwnerController`, replace the `IllegalArgumentException`
  thrown for a missing owner in `findOwner(...)` and `showOwner(...)` with
  `NotFoundException`.
- [x] 1.5 Run `./mvnw test -Dtest=OwnerControllerTests`; confirm the new tests
  pass and existing tests stay green. Run `./mvnw spring-javaformat:apply`.

### [ ] 2.0 Missing pet (and missing owner on pet/visit routes) returns a friendly 404

#### 2.0 Proof Artifact(s)

- Test: `./mvnw test -Dtest=PetControllerTests` passes, including
  `testInitUpdateFormPetNotFoundReturns404` and
  `testInitCreationFormOwnerNotFoundReturns404`, demonstrates missing pet/owner
  on pet routes return HTTP 404 and the `error` view.
- Test: `./mvnw test -Dtest=VisitControllerTests` passes, including
  `testInitNewVisitFormPetNotFoundReturns404` and
  `testInitNewVisitFormOwnerNotFoundReturns404`, demonstrates the visit routes
  return HTTP 404 for missing pet/owner.

#### 2.0 Tasks

- [ ] 2.1 (RED) In `PetControllerTests`, add
  `testInitCreationFormOwnerNotFoundReturns404`
  (`GET /owners/999999/pets/new`) and `testInitUpdateFormPetNotFoundReturns404`
  (`GET /owners/1/pets/999999/edit`), each asserting `status().isNotFound()` and
  `view().name("error")`. Confirm they fail.
- [ ] 2.2 (RED) In `VisitControllerTests`, add
  `testInitNewVisitFormOwnerNotFoundReturns404`
  (`GET /owners/999999/pets/1/visits/new`) and
  `testInitNewVisitFormPetNotFoundReturns404`
  (`GET /owners/1/pets/999999/visits/new`), each asserting
  `status().isNotFound()` and `view().name("error")`. Confirm they fail.
- [ ] 2.3 (GREEN) In `PetController`, replace `IllegalArgumentException` with
  `NotFoundException` in `findOwner(...)` and `findPet(...)`, and make
  `findPet(...)` throw `NotFoundException` when `owner.getPet(petId)` returns
  `null` instead of returning `null`.
- [ ] 2.4 (GREEN) In `VisitController#loadPetWithVisit(...)`, replace both
  `IllegalArgumentException` throws (missing owner and missing pet) with
  `NotFoundException`.
- [ ] 2.5 Run `./mvnw test -Dtest=PetControllerTests` and
  `./mvnw test -Dtest=VisitControllerTests`; confirm new tests pass and existing
  tests stay green. Run `./mvnw spring-javaformat:apply`.

### [ ] 3.0 Recovery link back to Find Owners on the friendly error page

#### 3.0 Proof Artifact(s)

- Diff: `templates/error.html` contains a link with `th:href="@{/owners/find}"`
  and `th:text="#{findOwners}"` (demonstrates a localized recovery link with no
  hardcoded literal text).
- Test: `./mvnw test -Dtest=I18nPropertiesSyncTest` passes (demonstrates the new
  markup introduces no untranslated strings and all locale files remain in sync).

#### 3.0 Tasks

- [ ] 3.1 In `templates/error.html`, add a recovery link inside the error card
  after the message paragraph:
  `<a class="btn btn-primary" th:href="@{/owners/find}" th:text="#{findOwners}">Find Owners</a>`.
- [ ] 3.2 Run `./mvnw test -Dtest=I18nPropertiesSyncTest` to confirm no
  hardcoded-string or locale-sync violations were introduced.

### [ ] 4.0 End-to-end Playwright proof and full regression gate

#### 4.0 Proof Artifact(s)

- Test: `cd e2e-tests && npx playwright test not-found.spec.ts` passes (3 tests)
  demonstrates end-to-end 404 status, visible not-found message, and a working
  Find Owners recovery link.
- Screenshot: `e2e-tests/test-results/.../owner-not-found.png` demonstrates the
  rendered friendly not-found page.
- CLI: `./mvnw test` reports BUILD SUCCESS with 0 failures (demonstrates no
  regressions), and `./mvnw spring-javaformat:validate` passes (demonstrates
  formatting compliance).

#### 4.0 Tasks

- [ ] 4.1 Create `e2e-tests/tests/features/not-found.spec.ts` with four tests:
  (a) `GET /owners/999999` → response status 404 and the
  "The requested page was not found." text is visible, capturing
  `owner-not-found.png`; (b) `GET /owners/1/pets/999999/edit` → status 404 and
  the not-found message visible; (c) on `/owners/999999`, the "Find Owners" link
  scoped to `.liatrio-error-card` is visible and clicking it lands on
  `/owners/find`; (d) **(no internal-detail leakage)** on `/owners/999999`, the
  page body does **not** contain the raw exception text — assert
  `await expect(page.locator('body')).not.toContainText(/Owner not found with id/i)`
  (maps to Unit 1 FR "shall not include stack traces or internal exception text").
- [ ] 4.2 Run `cd e2e-tests && npx playwright test not-found.spec.ts`; confirm
  all 3 tests pass and the screenshot artifact is produced.
- [ ] 4.3 Run the full regression gate: `./mvnw test` (expect BUILD SUCCESS, 0
  failures) and `./mvnw spring-javaformat:validate` (expect pass). Confirm
  `CrashControllerTests` and `CrashControllerIntegrationTests` (exercised by the
  full suite) still pass — verifying the new `@ControllerAdvice` only handles
  `NotFoundException` and non-404 errors (e.g. `/oups`) still return 500.
