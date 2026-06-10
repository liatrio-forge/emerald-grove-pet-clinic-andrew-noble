# Task 04 Proofs - End-to-end verification (Playwright)

## Task Summary

This task proves, in a real browser, that a newly created owner can be found by
telephone and by city, and that an invalid telephone is rejected — without
regressing the existing owner flows.

## What This Task Proves

- Creating an owner then searching by their telephone reaches their details page.
- Searching by the owner's (unique) city reaches the same owner.
- An invalid telephone shows the validation message and stays on the find form.
- Existing owner management E2E flows still pass.

## Evidence Summary

- The new `find-owners-search.spec.ts` (2 tests) passes in Chromium.
- The existing `owner-management.spec.ts` (4 tests) still passes.

## Artifact: find-owners-search.spec.ts run

**What it proves:** Create-then-find-by-telephone, find-by-city, and the
invalid-telephone validation message all work through the real UI and routing.

**Why it matters:** Acceptance-level proof of the user-facing behavior in issue #3.

**Command:**

```bash
cd e2e-tests && npm test -- --grep "Find Owners Search"
```

**Result summary:** Both tests pass.

```text
Running 2 tests using 2 workers
  2 passed (15.0s)
```

## Artifact: Owner Management regression

**What it proves:** The controller/template/repository changes did not break the
existing owner journeys (search, add, edit, validation, mobile).

**Why it matters:** Confirms the feature is additive and safe to merge.

**Command:**

```bash
cd e2e-tests && npm test -- --grep "Owner Management"
```

**Result summary:** All existing owner tests still pass.

```text
Running 4 tests using 4 workers
  4 passed (15.1s)
```

## Reviewer Conclusion

The find-by-telephone and find-by-city flows work end-to-end in a real browser,
including the invalid-telephone path, with no regression to existing owner
management. The Playwright HTML report is available under
`e2e-tests/test-results/html-report/`.
