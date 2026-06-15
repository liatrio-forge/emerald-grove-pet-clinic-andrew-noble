# 09-tasks-delete-pet.md

Implementation task list for [09-spec-delete-pet.md](09-spec-delete-pet.md).

All work follows Strict TDD (Red-Green-Refactor): write the failing test(s) for
each parent task first, then the minimum production code to pass, then refactor.
Run `./mvnw test` (the pre-commit `maven-test-check` gate) before each commit, on
the `feat/delete-pet` branch.

## Relevant Files

| File | Why It Is Relevant |
| --- | --- |
| `src/main/java/org/springframework/samples/petclinic/owner/Owner.java` | Aggregate root; add `removePet(Pet)` and `orphanRemoval = true` on the `pets` mapping. |
| `src/main/java/org/springframework/samples/petclinic/owner/Pet.java` | Already cascades `visits` with `CascadeType.ALL`; confirms visit rows are deleted when the pet is removed (no change expected, verify). |
| `src/main/java/org/springframework/samples/petclinic/owner/PetController.java` | Add `GET`/`POST` `/owners/{ownerId}/pets/{petId}/delete` handlers. |
| `src/main/resources/templates/pets/confirmDeletePet.html` | New confirmation page (Delete/Cancel controls, visit-count warning). |
| `src/main/resources/templates/owners/ownerDetails.html` | Add the per-pet **Delete Pet** link in the existing action row. |
| `src/main/resources/messages/messages.properties` | Default bundle; add new keys (`deletePet`, `confirmDeletePet`, `deletePetWarning`, `deletePetVisitsWarning`, `cancel`). |
| `src/main/resources/messages/messages_{en,de,es,fa,ko,pt,ru,tr}.properties` | Locale bundles; add the same keys to keep `I18nPropertiesSyncTest` parity. |
| `src/test/java/org/springframework/samples/petclinic/owner/PetControllerTests.java` | Add web-layer tests for the GET confirm view and POST delete (success, with-visits, unknown id). |
| `src/test/java/org/springframework/samples/petclinic/owner/OwnerTests.java` | New (or existing) unit test for `Owner.removePet`. |
| `src/test/java/org/springframework/samples/petclinic/service/ClinicServiceTests.java` | Add `@DataJpaTest` integration test proving pet + visit rows are deleted (cascade/orphan removal). |
| `src/test/java/org/springframework/samples/petclinic/system/I18nPropertiesSyncTest.java` | Existing test that enforces locale-key parity; no edit, must stay green. |
| `e2e-tests/tests/features/pet-management.spec.ts` | Add the Playwright create→delete→verify-removed end-to-end proof. |
| `e2e-tests/tests/pages/pet-page.ts` / `owner-page.ts` | Page objects to extend with delete navigation/actions if helpful. |
| `docs/specs/09-spec-delete-pet/09-proofs/img/` | Destination for the confirmation-UI screenshot proof artifact. |

### Notes

- Java tests live under `src/test/java/...` mirroring the production package.
- Run a single test class with `./mvnw test -Dtest=ClassName`; full suite with
  `./mvnw test`.
- Flash messages (`message`/`error`) are set in the controller and rendered by the
  existing `#success-message`/`#error-message` banners on `ownerDetails.html`;
  these strings follow the existing convention of being set in Java (like
  "New Pet has been Added") and are not subject to the HTML i18n scan.
- All **visible HTML text** in `confirmDeletePet.html` and the new owner-page link
  must use `th:text="#{key}"` (the `I18nPropertiesSyncTest` fails on hard-coded
  HTML literals).
- The dynamic visit-count warning uses a parameterized key, e.g.
  `th:text="#{deletePetVisitsWarning(${visitCount})}"`.

## Tasks

### [x] 1.0 Domain + persistence: remove a pet and cascade-delete its visits

Add the `Owner.removePet` aggregate method and enable orphan removal on the
owner→pets mapping so that removing a pet from its owner and saving deletes the
pet row and (via the existing visit cascade) its visit rows. Demoable via unit
and `@DataJpaTest` integration tests proving the data is gone with no orphans.

#### 1.0 Proof Artifact(s)

- Test: `OwnerTests#shouldRemovePetFromOwner` (or equivalent) passes —
  demonstrates `removePet` removes the target pet from `owner.getPets()` while
  other pets remain.
- Test: `ClinicServiceTests` `@DataJpaTest` test passes — demonstrates that
  removing a pet that has visits and calling `owners.save(owner)` deletes both the
  pet row and its visit rows when the owner is reloaded (0 orphaned visit rows).
- CLI: `./mvnw test -Dtest=ClinicServiceTests,OwnerTests` returns BUILD SUCCESS —
  demonstrates the domain/persistence layer behaves as specified.

#### 1.0 Tasks

- [x] 1.1 (RED) Add `OwnerTests` (or a test in the owner package) asserting that
  after `owner.removePet(pet)`, `owner.getPets()` no longer contains `pet` but
  still contains the other pet(s). Run and confirm it fails to compile/pass.
- [x] 1.2 (RED) Add a `@DataJpaTest` test in `ClinicServiceTests`
  (`@Transactional`) that loads a sample owner whose pet has visits (e.g. owner 6,
  pet 7 in the sample data), asserts the pet currently has ≥1 visit, removes the
  pet via `owner.removePet(...)`, saves, reloads the owner, and asserts the pet is
  absent. Confirm it fails.
- [x] 1.3 (GREEN) Add `public void removePet(Pet pet)` to `Owner` that removes the
  pet from the internal `pets` list.
- [x] 1.4 (GREEN) Add `orphanRemoval = true` to the `@OneToMany` `pets` mapping on
  `Owner` so the removed pet row is deleted on save; rely on the existing
  `Pet.visits` `CascadeType.ALL` to delete its visit rows.
- [x] 1.5 (VERIFY) If feasible, assert at the DB level that no orphaned visit rows
  remain (e.g. via repository/`EntityManager` count) so the cascade is proven, not
  just the in-memory collection.
- [x] 1.6 Run `./mvnw test -Dtest=ClinicServiceTests,OwnerTests`; refactor for
  clarity while keeping green.

### [x] 2.0 Controller: confirmed delete flow (GET confirm + POST delete)

Add `GET /owners/{ownerId}/pets/{petId}/delete` (renders confirmation view with
pet + visit count) and `POST /owners/{ownerId}/pets/{petId}/delete` (removes the
pet, saves the owner, flashes a success message, redirects). Handle the unknown
`petId` path safely (no data change, error flash, redirect, no unhandled error).

#### 2.0 Proof Artifact(s)

- Test: `PetControllerTests#testInitDeletePetForm` passes — demonstrates `GET`
  returns status 200, view `pets/confirmDeletePet`, with `pet` in the model.
- Test: `PetControllerTests#testProcessDeletePetSuccess` passes — demonstrates
  `POST` for a pet with no visits removes it (owner saved without that pet),
  redirects to `/owners/{ownerId}`, and sets a success flash `message`.
- Test: `PetControllerTests#testProcessDeletePetWithVisits` passes — demonstrates
  deletion still succeeds for a pet that has visits (cascade path) and redirects.
- Test: `PetControllerTests#testProcessDeleteUnknownPet` passes — demonstrates an
  unknown `petId` causes no save, an error flash, a redirect, and no unhandled
  exception.
- CLI: `./mvnw test -Dtest=PetControllerTests` returns BUILD SUCCESS.

#### 2.0 Tasks

- [x] 2.1 (RED) Add `testInitDeletePetForm`: `GET .../pets/{petId}/delete` →
  `status().isOk()`, `view().name("pets/confirmDeletePet")`,
  `model().attributeExists("pet")`. Confirm it fails (404/no handler).
- [x] 2.2 (RED) Add `testProcessDeletePetSuccess`: `POST .../pets/{petId}/delete`
  → `status().is3xxRedirection()`, `view().name("redirect:/owners/{ownerId}")`,
  `flash().attributeExists("message")`; verify `owners.save(owner)` is called with
  the pet removed (use a captured/`given` owner with two pets). Confirm it fails.
- [x] 2.3 (RED) Add `testProcessDeletePetWithVisits`: same as success but the
  target pet has a visit in the mock owner; assert redirect + success message
  (cascade is exercised at the JPA layer, here we assert the flow still succeeds).
- [x] 2.4 (RED) Add `testProcessDeleteUnknownPet`: `POST` with a `petId` not on the
  owner → redirect to owner page, `flash().attributeExists("error")`, and verify
  `owners.save(...)` is NOT called for a removal. Confirm it fails.
- [x] 2.5 (GREEN) Add `@GetMapping("/pets/{petId}/delete")` returning
  `pets/confirmDeletePet`, exposing the pet and a `visitCount` model attribute
  (e.g. `pet.getVisits().size()`).
- [x] 2.6 (GREEN) Add `@PostMapping("/pets/{petId}/delete")`: look up the pet via
  `owner.getPet(petId)`; if null, add `error` flash and redirect; otherwise
  `owner.removePet(pet)`, `owners.save(owner)`, add success `message` flash, and
  redirect to `/owners/{ownerId}`.
- [x] 2.7 Run `./mvnw test -Dtest=PetControllerTests`; refactor while green.

### [x] 3.0 UI: Delete action and confirmation page

Add a **Delete Pet** link per pet on the owner details page and create the
`pets/confirmDeletePet.html` confirmation page with a danger-styled Delete
control, a Cancel link back to the owner, and a visit-count warning when visits
exist.

#### 3.0 Proof Artifact(s)

- Test: `PetControllerTests` assertion (from 2.5) that the confirmation model
  exposes the pet's visit count — demonstrates the "N visit(s) will also be
  deleted" warning can render.
- Screenshot: the rendered `pets/confirmDeletePet.html` page saved under
  `09-proofs/img/` — demonstrates the confirmation UI (Delete/Cancel + warning).
- Diff: `owners/ownerDetails.html` showing the new Delete Pet link in the per-pet
  action row — demonstrates the action is reachable from the owner page.

#### 3.0 Tasks

- [x] 3.1 Create `templates/pets/confirmDeletePet.html` using the
  `fragments/layout` layout: a heading and confirmation question
  (`th:text="#{confirmDeletePet}"`), the "cannot be undone" warning
  (`#{deletePetWarning}`), a `th:if`-guarded visit-count warning
  (`#{deletePetVisitsWarning(${visitCount})}`) shown only when `visitCount > 0`, a
  `POST` form to `.../pets/{petId}/delete` with a `btn btn-danger` submit
  (`#{deletePet}`), and a `btn`/link Cancel (`#{cancel}`) back to
  `/owners/{ownerId}`.
- [x] 3.2 Edit `owners/ownerDetails.html`: add a **Delete Pet** link
  (`th:text="#{deletePet}"`) in the existing per-pet action row (next to Edit Pet /
  Add Visit), linking to `@{__${owner.id}__/pets/__${pet.id}__/delete}`.
- [x] 3.3 Run the app (`./mvnw spring-boot:run`) or the Playwright run (Task 5) and
  capture a screenshot of the confirmation page into
  `docs/specs/09-spec-delete-pet/09-proofs/img/`.
- [x] 3.4 Confirm no hard-coded HTML strings were introduced (run
  `./mvnw test -Dtest=I18nPropertiesSyncTest` — covered fully in Task 4).

### [x] 4.0 Internationalization: add and sync message keys

Add the new user-facing message keys to the default `messages.properties` and
every locale bundle, preserving key parity.

#### 4.0 Proof Artifact(s)

- Test: `I18nPropertiesSyncTest` passes — demonstrates the new keys exist in every
  locale bundle with no parity gaps and no hard-coded HTML literals.
- Diff: the message bundle files showing the added keys — demonstrates the strings
  are externalized and translatable, consistent with existing keys.
- CLI: `./mvnw test -Dtest=I18nPropertiesSyncTest` returns BUILD SUCCESS.

#### 4.0 Tasks

- [x] 4.1 (RED) After adding the template/link (Task 3), run
  `./mvnw test -Dtest=I18nPropertiesSyncTest` and confirm it fails for missing
  keys (or that the keys are absent), establishing the gap.
- [x] 4.2 (GREEN) Add the new keys to the default `messages.properties` with
  English text: `deletePet=Delete Pet`, `confirmDeletePet=Are you sure you want to
  delete this pet?`, `deletePetWarning=This action cannot be undone.`,
  `deletePetVisitsWarning=This will also permanently delete {0} visit(s).`,
  `cancel=Cancel`.
- [x] 4.3 (GREEN) Add the same keys to all locale bundles
  (`messages_en, _de, _es, _fa, _ko, _pt, _ru, _tr`) with appropriate
  translations (English fallback acceptable where a translation is unavailable,
  matching how existing keys are handled).
- [x] 4.4 Run `./mvnw test -Dtest=I18nPropertiesSyncTest`; confirm green.

### [x] 5.0 End-to-end proof (Playwright) and full-suite regression

Add a Playwright spec that creates a pet, deletes it through the confirmation
flow, and asserts the pet is gone from the owner details page; capture the
confirmation screenshot. Confirm the full Maven suite still passes (no regression
from the `orphanRemoval` change).

#### 5.0 Proof Artifact(s)

- Playwright: a test in `e2e-tests/tests/features/pet-management.spec.ts` passes —
  demonstrates create → Delete Pet → confirm → pet absent from owner details,
  end-to-end in a real browser.
- Screenshot/Artifact: Playwright-captured confirmation page and post-delete owner
  page under `e2e-tests/test-results/` — demonstrates the user-facing outcome.
- CLI: `./mvnw test` returns BUILD SUCCESS — demonstrates the full Java suite
  passes with no regressions introduced by the mapping change.

#### 5.0 Tasks

- [x] 5.1 Add a `Pet Management` test that: opens an owner, adds a uniquely-named
  pet, clicks its **Delete Pet** link, asserts the confirmation page is shown
  (screenshot here), clicks the confirm **Delete Pet** button, and asserts the pet
  name is no longer visible on the owner details page (and a success banner shows).
- [x] 5.2 (Optional) Add a delete helper/navigation to `pet-page.ts`/`owner-page.ts`
  if it keeps the spec readable, following the existing page-object pattern.
- [x] 5.3 Run the E2E suite (`cd e2e-tests && npm test -- --grep "Pet Management"`)
  and confirm pass; copy the confirmation screenshot into `09-proofs/img/` if not
  already captured in Task 3.
- [x] 5.4 Run the full Java suite `./mvnw test` and confirm BUILD SUCCESS (no
  regressions from `orphanRemoval`); this is also the pre-commit gate.
