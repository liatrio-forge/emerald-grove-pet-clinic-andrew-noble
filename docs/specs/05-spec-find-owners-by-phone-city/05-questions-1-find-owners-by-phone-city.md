# 05 Questions Round 1 - Find Owners by Telephone and City

Please answer each question below (check one or more options, or add your own
notes). Feel free to add additional context under any question.

## 1. How should multiple search criteria combine?

When a user fills in more than one of last name / telephone / city, how should
the results be filtered?

- [x] (A) **AND** — return owners matching *all* provided (non-empty) criteria
      (e.g., last name "Davis" **and** city "Madison").
- [ ] (B) **OR** — return owners matching *any* provided criterion.
- [ ] (C) Other (describe)

**Recommended answer(s):** [(A)]

**Why these are recommended:**

- `(A)` is the conventional "narrow my search" behavior users expect from a
  multi-field search form, and it makes results predictable and easy to test.
- `(B)` would *broaden* results as the user adds fields, which is surprising and
  rarely what a "find this owner" workflow wants.

## 2. What match semantics should each field use?

How should each field match against stored owner data?

- [x] (A) Last name **starts-with** (unchanged); **city** starts-with;
      **telephone** exact full-number match.
- [ ] (B) Last name starts-with (unchanged); city **contains** (substring);
      telephone exact full-number match.
- [ ] (C) All three **starts-with**.
- [ ] (D) Other (describe)

**Current best-practice context:** The `Owner.telephone` field is validated as
exactly 10 digits (`@Pattern("\\d{10}")`). Treating telephone search as an exact
match aligns with that constraint and with Q3 (rejecting malformed telephone
input), whereas partial telephone matching would conflict with validating it as
a complete number.

**Recommended answer(s):** [(A)]

**Why these are recommended:**

- `(A)` keeps the existing last-name behavior intact (a stated requirement) and
  treats city the same way (starts-with), which is consistent and case-insensitive
  like the current last-name search.
- Telephone **exact** match pairs naturally with validating it as a full 10-digit
  number; partial telephone search (B/C for telephone) would make the
  "reject invalid telephone" requirement contradictory.
- `(B)` (city contains) is a reasonable alternative if you expect partial city
  searches, but it's less consistent with the last-name convention.

## 3. How should invalid telephone input be handled?

The acceptance criteria require rejecting invalid telephone input with a clear
message. When should that fire, and what counts as invalid?

- [x] (A) Telephone is **optional**; validate **only when non-empty**, and
      reject anything that is not exactly 10 digits with a clear message
      (reuse the existing `telephone.invalid` message). Empty telephone =
      "don't filter by telephone".
- [ ] (B) Same as (A) but also enforce a specific format mask / allow separators
      (e.g., strip spaces and dashes before validating).
- [ ] (C) Other (describe)

**Recommended answer(s):** [(A)]

**Why these are recommended:**

- `(A)` matches the issue ("optional inputs", "invalid telephone input is
  rejected") and reuses the existing 10-digit rule and `telephone.invalid`
  message bundle key, keeping validation consistent with owner creation.
- `(B)` adds normalization complexity (separator stripping) that the current data
  model doesn't use; can be a later enhancement if desired.

## 4. What should an empty search (no criteria) do?

If the user submits with all fields blank:

- [x] (A) Preserve current behavior — return **all** owners (paginated), exactly
      as the parameterless `/owners` search does today.
- [ ] (B) Show a validation prompt asking for at least one criterion.
- [ ] (C) Other (describe)

**Recommended answer(s):** [(A)]

**Why these are recommended:**

- `(A)` preserves existing behavior (a stated requirement: "keep the default
  behavior intact") and avoids changing the current parameterless-GET contract
  that other flows/tests may rely on.
- `(B)` is a defensible UX choice but changes existing behavior and adds scope.
