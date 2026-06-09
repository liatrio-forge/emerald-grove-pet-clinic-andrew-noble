# Task 03 Proofs - End-to-end browser verification (Playwright)

## Task Summary

This task proves, through a real browser, that the specialty filter works
end-to-end: applying it narrows the list and updates the URL, the filtered view
is reproducible by navigating directly to a shared `?specialty=` URL, and the
"No specialty" option works — all without regressing the existing Vet Directory
test.

## What This Task Proves

- Selecting "surgery" in the dropdown shows only surgery vets and updates the
  URL to include `specialty=surgery`.
- Navigating directly to `/vets.html?specialty=surgery` reproduces the same
  filtered list (the filter is shareable via the query parameter).
- Selecting "No specialty" shows only vets with no specialty.
- The existing `vet-directory.spec.ts` still passes (no regression).

## Evidence Summary

- The new `vet-specialty-filter.spec.ts` (3 tests) passes in Chromium.
- The existing `vet-directory.spec.ts` (1 test) still passes.

## Artifact: vet-specialty-filter.spec.ts run

**What it proves:** The dropdown filter, URL update, shareable direct-URL
navigation, and No-specialty option all work in a real browser.

**Why it matters:** This is the acceptance-level proof of the user-facing
behavior described in the spec, exercised through the actual UI and routing.

**Command:**

```bash
cd e2e-tests && npm test -- --grep "Vet Specialty Filter"
```

**Result summary:** All 3 tests pass — dropdown filter + URL assertion, direct
shared-URL navigation, and the No-specialty option.

```text
Running 3 tests using 3 workers
  [1/3] Vet Specialty Filter › filters the list to a named specialty via the dropdown and reflects it in the URL
  [2/3] Vet Specialty Filter › reproduces the same filtered list when navigating directly to a shared URL
  [3/3] Vet Specialty Filter › shows vets with no specialty when the "No specialty" option is selected
  3 passed (15.1s)
```

## Artifact: Existing Vet Directory regression check

**What it proves:** The template and controller changes did not break the
pre-existing Vet Directory browse test.

**Why it matters:** Confirms the feature is additive and safe to merge.

**Command:**

```bash
cd e2e-tests && npm test -- --grep "Vet Directory"
```

**Result summary:** The existing spec still passes.

```text
Running 1 test using 1 worker
  [1/1] Vet Directory › can browse veterinarian list and view specialties
  1 passed (15.0s)
```

## Reviewer Conclusion

The specialty filter works end-to-end in a real browser — including shareable
filtered URLs and the No-specialty case — with no regression to the existing
Vet Directory journey. The Playwright HTML report is available under
`e2e-tests/test-results/html-report/`.
