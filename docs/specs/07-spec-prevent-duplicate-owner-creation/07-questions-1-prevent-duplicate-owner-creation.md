# 07 Questions Round 1 - Prevent Duplicate Owner Creation

Please answer each question below (select one or more options, or add your own
notes). Feel free to add additional context under any question. When you're
done, save the file and let me know.

> **Answers (confirmed 2026-06-11):** The user accepted all recommendations —
> **Q1: (A)** first + last + telephone, **Q2: (A)** case-insensitive & trimmed,
> **Q3: (A)** creation only, **Q4: (A)** field-level error on `lastName` reusing
> the existing `duplicate` message, **Q5: (A)** application-level check, no schema change.

## 1. Duplicate Definition Rule

What combination of fields makes two owners "the same" for the purpose of
blocking creation?

- [ ] (A) First name + last name + telephone all match
- [ ] (B) Telephone alone matches (phone number is treated as a unique identifier)
- [ ] (C) First name + last name + address all match
- [ ] (D) First name + last name + telephone + city + address (full record) match
- [ ] (E) Other (describe)

**Current best-practice context:** The issue suggests "same first/last/telephone"
as an example. A small, explicit composite key (first + last + telephone) is
specific enough to avoid blocking genuinely different people who happen to share
a phone (e.g., family members rarely share both name and phone) while still
catching accidental re-submissions of the same person.

**Recommended answer(s):** [(A)]

**Why these are recommended:**

- `(A)` matches the example in the issue and is the smallest explicit rule that
  reliably identifies an accidental duplicate of the same person.
- `(B)` is simpler but too aggressive — households and businesses can legitimately
  share one phone number, so it would block valid distinct owners.
- `(C)` and `(D)` are more permissive (harder to trigger), letting more near-duplicates
  through; `(D)` in particular almost never matches because any typo in any field
  defeats it.

## 2. Case and Whitespace Handling

When comparing the fields chosen in Q1, how strictly should values be matched?

- [ ] (A) Case-insensitive and trimmed of leading/trailing whitespace (e.g. "  john " == "John")
- [ ] (B) Exact match only (case-sensitive, whitespace-sensitive)
- [ ] (C) Other (describe)

**Current best-practice context:** Human-entered names vary in capitalization and
stray spaces. Normalizing before comparison catches "John Smith" vs "john smith"
as the same person, which is almost always the user's intent for duplicate
detection.

**Recommended answer(s):** [(A)]

**Why these are recommended:**

- `(A)` catches the realistic ways the same person gets re-entered (different
  capitalization, accidental spaces) and reflects what users mean by "duplicate."
- `(B)` is easy to implement but lets obvious duplicates ("JOHN" vs "John") slip
  through, undermining the feature's purpose.

## 3. Does the Rule Apply to Edits Too, or Only Creation?

The issue is titled "duplicate owner *creation*." Should the duplicate check run
only when creating a new owner, or also when editing an existing owner?

- [ ] (A) Creation only (`POST /owners/new`)
- [ ] (B) Creation and edit (`POST /owners/new` and `POST /owners/{id}/edit`), where edit
      ignores the owner's own record when checking
- [ ] (C) Other (describe)

**Current best-practice context:** Scoping to creation keeps this spec a single
focused slice that directly matches the issue title and acceptance criteria.
Edit-time checking is valuable but adds the "exclude self" edge case and broader
test surface.

**Recommended answer(s):** [(A)]

**Why these are recommended:**

- `(A)` exactly matches the issue ("duplicate owner creation") and keeps the
  demoable unit small and reviewable.
- `(B)` is a reasonable future enhancement but expands scope and edge cases; if
  you want it, we can capture it as a follow-up spec rather than growing this one.

## 4. How Should the Error Be Surfaced in the UI?

When a duplicate is detected, how should the form respond?

- [ ] (A) Re-render the creation form with a field-level error on `lastName` (using the
      existing `duplicate` = "is already in use" message), preserving the user's input
- [ ] (B) Re-render the form with a single form-level banner error (e.g. a red message
      above the form) and preserve input
- [ ] (C) Redirect to the existing matching owner's detail page with an informational message
- [ ] (D) Other (describe)

**Current best-practice context:** The repo already defines a reusable `duplicate`
message ("is already in use") and the input-field fragment renders Bean
Validation / `BindingResult` field errors inline. Reusing the existing
`BindingResult` + field-error pattern (as `processCreationForm` already does for
validation failures) keeps UX consistent and input preserved.

**Recommended answer(s):** [(A)]

**Why these are recommended:**

- `(A)` reuses the existing field-error rendering pattern and the existing
  `duplicate` message key, giving a clear, actionable, input-preserving error with
  minimal new UI work — and is straightforward to assert in both JUnit (`BindingResult`)
  and Playwright (visible field error).
- `(B)` is also clean but introduces a new form-level error display not currently used
  on this form.
- `(C)` (redirect to existing owner) is a nice "we found them" UX but changes the flow,
  is harder to assert as a "blocked creation" error, and is closer to a different feature.

## 5. Database-Level Constraint?

Should we also add a database unique constraint as a backstop, or rely solely on
the application-level check?

- [ ] (A) Application-level check only (controller/service), no schema change
- [ ] (B) Application-level check + a DB unique constraint on the chosen fields as a backstop
- [ ] (C) Other (describe)

**Current best-practice context:** A DB constraint guarantees integrity even under
concurrent requests, but requires schema migrations across H2/MySQL/PostgreSQL
init scripts and complicates case-insensitive matching (Q2). The issue's proof
artifacts call for a controller/service test, implying an app-level rule.

**Recommended answer(s):** [(A)]

**Why these are recommended:**

- `(A)` matches the issue's stated proof (controller/service test), avoids
  multi-database schema migrations, and cleanly supports case-insensitive matching
  in application code.
- `(B)` adds real robustness but expands scope to schema changes across three DB
  profiles and makes case-insensitive uniqueness awkward; better as a follow-up if
  concurrency integrity becomes a concern.
