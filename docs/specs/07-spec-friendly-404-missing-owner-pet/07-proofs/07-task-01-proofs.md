# Task 01 Proofs - Missing owner returns a friendly 404

## Task Summary

This task proves that requests for a non-existent owner now return HTTP 404 and
render the shared friendly `error` view instead of throwing an
`IllegalArgumentException` that surfaced as HTTP 500. It introduces a dedicated
`NotFoundException` and a `@ControllerAdvice` (`GlobalExceptionHandler`) that
maps that exception to a 404 error page without exposing internal exception
detail.

## What This Task Proves

- A missing owner detail request (`GET /owners/{id}`) returns HTTP 404 and the
  `error` view (Unit 1 FR: 404 status + friendly view).
- A missing owner edit request (`GET /owners/{id}/edit`) returns HTTP 404 and
  the `error` view.
- The handler renders only a status marker; it does not pass the raw exception
  message to the model (Unit 1 security FR: no internal-detail leakage).

## Evidence Summary

- The two new MockMvc tests assert `status().isNotFound()` + `view().name("error")`
  and failed first (RED, 500) then passed after the implementation (GREEN).
- The full `OwnerControllerTests` class (24 tests) passes, confirming no
  regression to existing owner behavior.

## Artifact: RED — new tests fail before implementation

**What it proves:** The tests genuinely exercise the new behavior (strict TDD
RED phase) — before the code change the requests returned 500, not 404.

**Why it matters:** Confirms the tests are meaningful rather than vacuously
passing.

**Command:**

```bash
./mvnw test -Dtest="OwnerControllerTests#testShowOwnerNotFoundReturns404+testInitUpdateOwnerFormNotFoundReturns404"
```

**Result summary:** Both tests errored because the controller threw
`IllegalArgumentException` (resolved as HTTP 500) instead of returning 404.

```text
[ERROR] Tests run: 2, Failures: 0, Errors: 2, Skipped: 0 -- in OwnerControllerTests
[ERROR] BUILD FAILURE
```

## Artifact: GREEN — full OwnerControllerTests passes

**What it proves:** After adding `NotFoundException`, `GlobalExceptionHandler`,
and wiring `OwnerController`, the new 404 tests pass and all existing owner
tests remain green.

**Why it matters:** Demonstrates the feature works and introduces no regression.

**Command:**

```bash
./mvnw test -Dtest=OwnerControllerTests
```

**Result summary:** 24 tests run, 0 failures, 0 errors.

```text
[INFO] Tests run: 24, Failures: 0, Errors: 0, Skipped: 0 -- in OwnerControllerTests
[INFO] BUILD SUCCESS
```

## Artifact: No internal-detail leakage in the handler

**What it proves:** `GlobalExceptionHandler` adds only a `status` marker to the
model and never the exception message, so stack traces / internal text cannot
reach the rendered page.

**Why it matters:** Satisfies the spec's security requirement that internal
exception details are not exposed to users.

**Artifact path:** `src/main/java/org/springframework/samples/petclinic/system/GlobalExceptionHandler.java`

**Result summary:** The handler returns `new ModelAndView("error")` with
`status = 404` only; the `NotFoundException` message is not added to the model.

```java
@ExceptionHandler(NotFoundException.class)
@ResponseStatus(HttpStatus.NOT_FOUND)
public ModelAndView handleNotFound() {
    ModelAndView mav = new ModelAndView("error");
    mav.addObject("status", HttpStatus.NOT_FOUND.value());
    return mav;
}
```

## Reviewer Conclusion

Missing-owner requests now return a friendly 404 backed by automated tests, and
the handler is implemented so no internal exception detail is exposed. The
end-to-end browser proof and the no-leakage assertion are completed in Task 04.
