# Task 05 Proofs - End-to-end delete flow and full-suite regression

## Task Summary

This task proves the complete delete flow works end-to-end in a real browser
(create a pet → confirm deletion → pet removed) and that enabling
`orphanRemoval` introduced no regressions in the Java test suite.

## What This Task Proves

- A user can add a pet, open its confirmation page, confirm, and see the pet
  disappear from the owner details page — verified in Chromium via Playwright.
- The confirmation UI renders correctly (heading, warning, Delete/Cancel) —
  captured as a screenshot.
- The full Maven test suite passes with the mapping change in place.

## Evidence Summary

- Playwright test `Pet Management › can delete a pet from an owner after
  confirming` passes (1 passed, ~16.5s).
- Confirmation screenshot saved at `09-proofs/img/confirm-delete-pet.png`.
- Full `./mvnw test`: 93 run, 0 failures, 0 errors, 5 skipped (MySQL/Postgres
  Testcontainers skipped — Docker not running), BUILD SUCCESS.

## Artifact: Playwright end-to-end test

**What it proves:** The full create → confirm → delete → verify-removed journey
works in a real browser.

**Why it matters:** This is the issue's required Proof/Demo and the strongest
evidence the feature works for a real user.

**Command:**

```bash
cd e2e-tests && npm test -- --grep "can delete a pet"
```

**Result summary:** The test added a uniquely-named pet, opened the confirmation
page, captured the screenshot, confirmed, and asserted the pet was no longer
present on the owner page.

```text
Running 1 test using 1 worker
[1/1] [chromium] › pet-management.spec.ts › Pet Management › can delete a pet from an owner after confirming
  1 passed (16.5s)
```

## Artifact: Confirmation UI screenshot

**What it proves:** The confirmation page as rendered in the browser.

**Why it matters:** Satisfies the issue's "Screenshot: confirmation UI" proof and
shows the visit-warning is correctly hidden for a pet with no visits.

**Artifact path:** `docs/specs/09-spec-delete-pet/09-proofs/img/confirm-delete-pet.png`

**Result summary:** Shows the "Delete Pet" heading, "Are you sure you want to
delete this pet?", the pet name, "This action cannot be undone.", and the
danger-styled Delete Pet button next to Cancel.

![Delete-pet confirmation page rendered in Chromium](img/confirm-delete-pet.png)

## Artifact: Full Java test suite (regression)

**What it proves:** No existing behavior broke after adding `orphanRemoval = true`
and the new endpoints.

**Why it matters:** The mapping change touches persistence shared by all
owner/pet flows; the suite is the regression guard called out in the audit FLAG.

**Command:**

```bash
./mvnw test
```

**Result summary:** BUILD SUCCESS; the 5 skipped tests are the
Docker-dependent MySQL/Postgres integration tests (no Docker in this environment),
not failures.

```text
[WARNING] Tests run: 93, Failures: 0, Errors: 0, Skipped: 5
[INFO] BUILD SUCCESS
```

## Reviewer Conclusion

The feature works end-to-end in a real browser and the full suite passes with no
regressions, completing the spec's proof obligations.
