# 07-spec-friendly-404-missing-owner-pet.md

## Introduction/Overview

When a user navigates to an owner or pet that does not exist (for example a
hand-typed or stale URL such as `/owners/999999`), the application currently
throws an `IllegalArgumentException` that surfaces as an HTTP **500 Internal
Server Error** with a generic error view. This is misleading (the server is
healthy; the resource simply is not there) and risks exposing internal
exception detail to end users. This feature makes missing owners and pets
return a user-friendly **404 Not Found** page that clearly explains the page
was not found and offers a link back to **Find Owners**, without leaking stack
traces or internal exception details.

## Goals

- Return HTTP **404** (not 500) when a requested owner or pet does not exist.
- Show a friendly, localized "page was not found" message instead of a raw
  exception view.
- Provide a clear link from the not-found page back to **Find Owners** so users
  can recover.
- Never expose stack traces or internal exception messages to end users.
- Add automated regression coverage (JUnit MVC + Playwright) proving the 404
  behavior and message.

## User Stories

- **As a clinic staff member**, I want a mistyped or outdated owner URL to show
  a clear "not found" page so that I understand the record does not exist rather
  than thinking the application crashed.
- **As a clinic staff member**, I want the same friendly handling when a pet
  under an owner does not exist so that broken pet links behave consistently.
- **As a clinic staff member who hit a dead link**, I want a link back to Find
  Owners so that I can immediately search for the correct record without
  navigating manually.
- **As a site operator**, I want internal exception details kept off the
  user-facing page so that we do not leak implementation details.

## Demoable Units of Work

### Unit 1: Missing owner returns a friendly 404

**Purpose:** Convert missing-owner lookups across the owner-facing controllers
from a 500 exception view into a friendly 404 page. Serves any user reaching a
non-existent owner URL.

**Functional Requirements:**

- The system shall respond with HTTP status `404` when a request targets an
  owner ID that does not exist (e.g. `/owners/{id}`, `/owners/{id}/edit`).
- The system shall render the shared friendly error view showing the localized
  "The requested page was not found." message for these 404 responses.
- The system shall not include stack traces or internal exception text in the
  rendered not-found page.

**Proof Artifacts:**

- Test: `OwnerControllerTests#testShowOwnerNotFoundReturns404` passes
  demonstrates a missing owner detail request returns 404 and the `error` view.
- Test: `OwnerControllerTests#testInitUpdateOwnerFormNotFoundReturns404` passes
  demonstrates a missing owner edit request returns 404 and the `error` view.
- Playwright: navigating to `/owners/999999` returns HTTP 404 and the not-found
  message is visible (`not-found.spec.ts`) demonstrates the end-to-end friendly
  404 behavior.
- Screenshot: `owner-not-found.png` demonstrates the rendered friendly page.

### Unit 2: Missing pet returns a friendly 404

**Purpose:** Apply the same friendly 404 handling when a pet under an owner does
not exist, including the pet edit and visit flows.

**Functional Requirements:**

- The system shall respond with HTTP status `404` when a request targets a pet
  ID that does not exist for the given owner (e.g.
  `/owners/{ownerId}/pets/{petId}/edit`,
  `/owners/{ownerId}/pets/{petId}/visits/new`).
- The system shall render the same friendly error view and localized not-found
  message used for missing owners.
- The system shall handle a missing owner in the pet/visit routes with the same
  404 behavior (an owner ID that does not exist on a pet route returns 404).

**Proof Artifacts:**

- Test: `PetControllerTests#testInitUpdateFormPetNotFoundReturns404` passes
  demonstrates a missing pet edit request returns 404 and the `error` view.
- Test: `PetControllerTests#testInitCreationFormOwnerNotFoundReturns404` passes
  demonstrates a missing owner on a pet route returns 404.
- Test: `VisitControllerTests#testInitNewVisitFormPetNotFoundReturns404` and
  `#testInitNewVisitFormOwnerNotFoundReturns404` pass demonstrate the visit
  routes return 404 for missing pet/owner.
- Playwright: navigating to `/owners/1/pets/999999/edit` returns HTTP 404 with
  the not-found message demonstrates end-to-end pet handling.

### Unit 3: Recovery link back to Find Owners

**Purpose:** Give users a one-click path off the not-found page back into the
owner search flow.

**Functional Requirements:**

- The system shall display a link on the friendly error page that navigates to
  the Find Owners page (`/owners/find`).
- The link label shall use the existing localized `findOwners` message so it is
  translated consistently with the rest of the UI.
- The user shall be able to click the link and arrive on the Find Owners search
  page.

**Proof Artifacts:**

- Playwright: on `/owners/999999`, a "Find Owners" link inside the error card is
  visible and clicking it lands on `/owners/find` (`not-found.spec.ts`)
  demonstrates the recovery path works.
- Code: `error.html` contains a `th:href="@{/owners/find}"` link using
  `#{findOwners}` demonstrates the localized recovery link exists.

## Non-Goals (Out of Scope)

1. **Redesigning the error page**: No new visual design for the error page
   beyond adding the recovery link; the existing `error.html` layout is reused.
2. **Changing non-404 error handling**: The existing 500 behavior (e.g. the
   `/oups` crash demo) is unchanged.
3. **Vet/specialty not-found handling**: Only owner and pet (and the visit flow
   that depends on them) are in scope; vet routes are not modified.
4. **New i18n message keys / translations**: Reuse existing keys (`error.404`,
   `findOwners`); no new translation work.
5. **Custom error response bodies for API/JSON clients**: This feature targets
   the server-rendered HTML pages only.

## Design Considerations

The friendly not-found page reuses the existing `templates/error.html`, which
already branches on `${status}` to show the localized `error.404` message
("The requested page was not found."). A recovery link styled as a button
(`btn btn-primary`) is added to the error card, labeled via the existing
`#{findOwners}` message and pointing at `/owners/find`. No new templates or
visual redesign are introduced.

## Repository Standards

- **Strict TDD**: Write failing tests first (RED), then minimum code to pass
  (GREEN), then refactor — per `CLAUDE.md`.
- **Web layer testing**: Use `@WebMvcTest` + MockMvc with `@MockitoBean`
  repositories, following the existing `*ControllerTests` patterns.
- **E2E testing**: Add Playwright specs under `e2e-tests/tests/features/`
  following existing spec/page-object conventions.
- **i18n discipline**: Satisfy `I18nPropertiesSyncTest` — no hardcoded literal
  text in HTML (use `th:text`/`#{}`), and keep all `messages_*.properties` files
  in sync (achieved here by reusing existing keys).
- **Formatting**: Code must pass `spring-javaformat:validate`.
- **Version control**: Conventional commits; feature branch + PR (no direct
  commits to `main`).

## Technical Considerations

- Introduce a dedicated `NotFoundException` (a `RuntimeException`) to represent
  "resource not found", replacing the `IllegalArgumentException` thrown for
  missing owners/pets in `OwnerController`, `PetController`, and
  `VisitController`.
- Handle it centrally with a `@ControllerAdvice` global exception handler that
  uses `@ExceptionHandler(NotFoundException.class)` + `@ResponseStatus(NOT_FOUND)`
  and returns a `ModelAndView` for the `error` view with `status = 404`. This is
  the established Spring MVC convention for mapping a domain exception to a 404
  page and keeps view selection explicit and unit-testable via MockMvc
  (`view().name("error")`).
- The handler must not pass the raw exception message to the model (avoid
  exposing internal detail); the user-facing text comes from the localized
  `error.404` key.
- `PetController#findPet` currently returns `null` for a missing pet (which
  later breaks template rendering); it must instead throw `NotFoundException`
  when the pet is absent.
- Place the new exception and handler in the existing
  `org.springframework.samples.petclinic.system` package alongside other
  cross-cutting system components.

## Security Considerations

- **No internal detail leakage**: The not-found page must not render stack
  traces or internal exception messages. The handler intentionally renders only
  the localized, generic 404 message.
- No credentials, tokens, or sensitive data are involved in this feature.
- Proof artifacts (screenshots) contain only the public not-found page and are
  safe to attach to the PR.

## Success Metrics

1. **Correct status**: Requests for non-existent owners/pets return HTTP `404`
   (verified by JUnit MVC tests and Playwright response assertions).
2. **Friendly messaging**: The localized "page was not found" message renders on
   the not-found page (verified by Playwright visibility assertion).
3. **Recovery**: The "Find Owners" link on the error page navigates to
   `/owners/find` (verified by Playwright).
4. **No regressions**: The full Java test suite and `I18nPropertiesSyncTest`
   remain green; `spring-javaformat:validate` passes.

## Open Questions

No open questions at this time.
