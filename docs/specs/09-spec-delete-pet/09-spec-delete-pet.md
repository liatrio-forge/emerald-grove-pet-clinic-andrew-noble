# 09-spec-delete-pet.md

## Introduction/Overview

The owner details page lets a user add and edit pets, but there is no way to
remove a pet that was added by mistake or no longer belongs to the owner. This
feature adds a **Delete Pet** action that is guarded by an explicit confirmation
step: clicking the action opens a dedicated confirmation page, and only a
deliberate confirm submission deletes the pet. Deleting a pet also removes that
pet's visits (cascade), and after deletion the pet no longer appears on the
owner details page. The change reuses the existing Owner-aggregate, controller,
and Thymeleaf patterns and requires no schema migration.

## Goals

- Provide a **Delete Pet** action for each pet on the owner details page.
- Require a deliberate confirmation step before any pet is deleted (no
  single-click destructive action).
- On confirmation, permanently delete the pet and its visits, then return to the
  owner details page where the pet no longer appears.
- Preserve all existing owner/pet/visit behavior; deletion is purely additive.
- Meet the repository's coverage standards (>90% line coverage for new code,
  full branch coverage of the delete logic) via JUnit, a `@DataJpaTest`
  integration test, and a Playwright end-to-end proof.

## User Stories

- **As a clinic staff member**, I want to delete a pet that was added by mistake
  so that the owner's record only lists pets they actually have.
- **As a clinic staff member**, I want to be asked to confirm before a pet is
  deleted so that I do not remove a record accidentally with a single click.
- **As a clinic administrator**, I want a deleted pet's visit history removed
  along with it so that the data store does not retain orphaned visit rows.

## Demoable Units of Work

### Unit 1: Delete a pet through a confirmed server-side flow

**Purpose:** Add the confirmation page and the delete endpoint so that a user can
remove a pet via a deliberate two-step flow, and the pet (and its visits) are
gone afterward. Serves clinic staff correcting an owner's pet list and
administrators relying on a clean data store.

**Functional Requirements:**

- The system shall expose `GET /owners/{ownerId}/pets/{petId}/delete` that renders
  a dedicated confirmation view (`pets/confirmDeletePet`) showing the pet's name
  and, when the pet has visits, the number of visits that will also be deleted.
- The system shall expose `POST /owners/{ownerId}/pets/{petId}/delete` that
  removes the identified pet from its owner, persists the change, and redirects to
  `GET /owners/{ownerId}`.
- The system shall, on successful deletion, set a flash success `message`
  (e.g. "Pet has been deleted") so the owner details page confirms the outcome
  using the page's existing success-message banner.
- The system shall cascade the deletion to the pet's visits, so deleting a pet
  with visits removes the pet row and all of its visit rows.
- The system shall, when the `petId` does not belong to the owner (unknown or
  already-deleted pet), not modify any data, set a flash error message, and
  redirect to `GET /owners/{ownerId}` without raising an unhandled error.
- The user shall be able to cancel from the confirmation page via a link back to
  the owner details page that performs no deletion.

**Proof Artifacts:**

- Test: a `PetControllerTests` test for `GET .../delete` passes — demonstrates the
  confirmation view is returned with status 200 and the `pet` in the model.
- Test: a `PetControllerTests` test for `POST .../delete` (pet with no visits)
  passes — demonstrates the pet is removed (owner saved without that pet) and the
  response redirects to the owner page with a success flash message.
- Test: a `PetControllerTests` test for `POST .../delete` of a pet **with** visits
  passes — demonstrates deletion still succeeds (cascade path) and redirects.
- Test: a `PetControllerTests` test for `POST .../delete` with an unknown `petId`
  passes — demonstrates no data change, an error flash message, a redirect, and no
  unhandled exception.
- Test: an `OwnerTests`/unit test for `Owner.removePet(...)` passes — demonstrates
  the pet is removed from `owner.getPets()` while other pets remain.
- Test: a `ClinicServiceTests` (`@DataJpaTest`) test passes — demonstrates that
  removing a pet that has visits and saving the owner deletes both the pet row and
  its visit rows at the database level (verifies the JPA cascade/orphan-removal
  mapping).

### Unit 2: Surface the Delete action and confirmation UI

**Purpose:** Make the delete flow reachable and clear in the browser, with a
confirmation page that states the consequences and offers Cancel / Delete. Serves
clinic staff who need an obvious, safe way to remove a pet.

**Functional Requirements:**

- The system shall add a **Delete Pet** link for each pet on the owner details
  page (`owners/ownerDetails.html`), alongside the existing Edit Pet / Add Visit
  actions, linking to that pet's confirmation page.
- The system shall render a confirmation page (`pets/confirmDeletePet.html`) that
  states the pet being deleted and warns that the action cannot be undone, and
  when visits exist, that the visits will also be deleted.
- The system shall present, on the confirmation page, a **Delete Pet** control
  that submits the `POST` deletion and a **Cancel** control that returns to the
  owner details page without deleting.
- The system shall back all new user-facing strings with internationalized
  message keys, defined in the default `messages.properties` and in every locale
  bundle, preserving locale-key parity.

**Proof Artifacts:**

- Test: a `PetControllerTests` assertion that the confirmation model exposes the
  visit count — demonstrates the "N visit(s) will also be deleted" warning can
  render.
- Test: `I18nPropertiesSyncTest` passes with the new keys present in every bundle
  — demonstrates locale-key parity is maintained.
- Playwright: an end-to-end spec (in `e2e-tests/tests/features/`) that creates a
  pet, clicks **Delete Pet**, lands on the confirmation page, confirms, and
  asserts the pet no longer appears on the owner details page — demonstrates the
  full user-facing flow in a real browser (satisfies the issue's Proof/Demo).
- Screenshot: the confirmation page captured during the Playwright run and saved
  under `09-proofs/img/` — demonstrates the confirmation UI.

## Non-Goals (Out of Scope)

1. **Deleting an owner**: This spec covers deleting a pet only. Removing owners is
   not added.
2. **Deleting an individual visit**: There is no per-visit delete action; visits
   are removed only as a side effect of deleting their pet.
3. **Soft delete / undo / archive**: Deletion is permanent. No trash, restore, or
   audit-trail feature is introduced.
4. **Blocking deletion when visits exist**: Per the agreed design, pets with
   visits are deletable (visits cascade-delete); a "block if visits exist"
   behavior is explicitly out of scope.
5. **Bulk deletion**: Only one pet is deleted per confirmed action; multi-select
   deletion is not included.
6. **Authentication / authorization changes**: The app has no auth layer; no
   role-based restriction on who may delete is added here.
7. **REST/JSON API**: The action is server-rendered (HTML form POST + redirect),
   consistent with existing controllers; no JSON `DELETE` endpoint is added.

## Design Considerations

The owner details page (`owners/ownerDetails.html`) already renders, per pet, a
row of actions ("Edit Pet", "Add Visit"). A **Delete Pet** link is added to that
same row, linking to the confirmation page. The new confirmation page
(`pets/confirmDeletePet.html`) follows the existing layout fragment
(`fragments/layout`) and Bootstrap button styling used elsewhere (e.g. the
`btn btn-primary` actions on the owner page); the destructive confirm button uses
a danger style (`btn btn-danger`) to signal consequence, while Cancel is a
secondary/standard link back to the owner page. The success outcome reuses the
owner details page's existing `#success-message` banner (driven by the `message`
flash attribute), and the unknown-pet error path reuses the page's existing
`#error-message` banner (driven by an `error` flash attribute). All visible
strings use the standard i18n message-key approach.

## Repository Standards

- **Strict TDD (mandatory)**: Follow Red-Green-Refactor. Write the failing
  `PetControllerTests`, `OwnerTests`, and `ClinicServiceTests` assertions first,
  then add the controller endpoints, `Owner.removePet`, the mapping change, the
  template, and message keys; then refactor. No production code before a failing
  test.
- **Layered architecture**: Treat `Owner` as the aggregate root — delete by
  removing the pet from the owner and saving through `OwnerRepository` (no new
  repository), mirroring how creation/edit save through the owner.
- **Web-layer conventions**: Reuse the existing `PetController`
  `@RequestMapping("/owners/{ownerId}")` class, `@ModelAttribute("owner")` owner
  lookup, `RedirectAttributes` flash messages, and `redirect:/owners/{ownerId}`
  return pattern already used by create/edit.
- **HTTP semantics**: Destructive change occurs only on `POST`; `GET` renders the
  read-only confirmation page (no side effects).
- **Testing patterns**: `@WebMvcTest` + `MockMvc` + `@MockitoBean OwnerRepository`
  for controller tests (mirroring existing `PetControllerTests`), `@DataJpaTest`
  for the cascade integration test (mirroring `ClinicServiceTests`), and the
  existing Playwright page-object/fixture structure for E2E. Use Arrange-Act-Assert
  and descriptive test names.
- **i18n**: Add new keys to the default `messages.properties` and all locale
  bundles (de, es, fa, ko, pt, ru, tr, en); `I18nPropertiesSyncTest` enforces key
  parity.
- **Coverage**: >90% line coverage for new code; the delete success path, cascade
  path, and unknown-pet path must each be explicitly tested.
- **Commits**: Conventional commit messages on a feature branch (`feat/delete-pet`);
  no direct commits to `main` (enforced by pre-commit hook). Pre-commit runs the
  full Maven test suite.

## Technical Considerations

- **Cascade/orphan removal**: `Owner.pets` is `@OneToMany(cascade = ALL)` with
  `@JoinColumn(name = "owner_id")` and is not currently configured for orphan
  removal. Add `orphanRemoval = true` to that mapping so that removing a pet from
  the collection and saving the owner deletes the pet row. `Pet.visits` is
  `@OneToMany(cascade = ALL)`, so the pet's removal cascades to delete its visit
  rows. Verify this end-to-end with the `@DataJpaTest` integration test against H2.
- **Domain method**: Add `Owner.removePet(Pet pet)` that removes the pet from the
  internal `pets` list; the controller resolves the pet via the existing
  `owner.getPet(petId)` lookup before removing.
- **Controller lookup/guard**: In the `POST` handler, look up the pet with
  `owner.getPet(petId)`; if `null`, skip deletion, add an `error` flash attribute,
  and redirect — avoiding an unhandled exception for stale/unknown ids.
- **No new dependencies**: Uses existing Spring MVC, Spring Data JPA, Thymeleaf,
  and Bean Validation already on the classpath.
- **No schema change**: `orphanRemoval` is a JPA runtime behavior; it does not
  alter the H2/MySQL/PostgreSQL schema or require a migration.
- **Regression caution**: Adding `orphanRemoval = true` changes how removed pets
  are persisted; confirm existing owner/pet create-edit tests still pass (pets are
  only ever added today, so no current flow removes a pet, but this must be
  verified by running the full suite).

## Security Considerations

- No credentials, API keys, or tokens are involved.
- Deletion is a permanent, destructive action; it is gated behind an explicit
  confirmation page and only executes via `POST`, so it cannot be triggered by a
  safe `GET` navigation, link prefetch, or crawler.
- Data is modified via JPA through parameterized operations; this change adds no
  string-concatenated queries and introduces no injection surface.
- The confirmation page and messages reveal only the pet name and visit count for
  the owner being viewed; no sensitive data is exposed.
- Playwright proof artifacts (screenshots/traces) contain only synthetic test data
  and are stored under the existing `e2e-tests/test-results/` location (plus a
  curated confirmation screenshot under `09-proofs/img/`); no secrets are
  committed.

## Success Metrics

1. **Confirmed deletion works**: Confirming deletion removes the pet and redirects
   to the owner page where the pet is absent — verified by JUnit and Playwright
   (target: 100% of confirmed deletions remove the pet).
2. **Confirmation required**: The pet is deleted only via the `POST` confirm
   submission; visiting the `GET` confirmation page performs no deletion — verified
   by controller tests (target: 0 deletions from `GET`).
3. **Cascade correct**: Deleting a pet with visits removes its visit rows with no
   orphans left — verified by the `@DataJpaTest` test (target: 0 orphaned visit
   rows).
4. **Safe on unknown id**: A `POST` for an unknown/already-deleted `petId` makes no
   data change and returns a redirect with an error message, no unhandled error —
   verified by a controller test.
5. **i18n parity**: `I18nPropertiesSyncTest` passes with the new keys in all
   bundles (target: 100% key parity).
6. **No regressions**: The full Maven test suite passes (pre-commit gate),
   confirming existing owner/pet/visit behavior is unchanged.
7. **Coverage**: New code meets >90% line coverage with success, cascade, and
   unknown-id paths explicitly exercised.

## Open Questions

No open questions at this time.
