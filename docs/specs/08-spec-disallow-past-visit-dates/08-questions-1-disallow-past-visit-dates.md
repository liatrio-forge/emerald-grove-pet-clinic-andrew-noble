# 08 Questions Round 1 - Disallow Past Visit Dates

Please answer each question below (select one or more options, or add your own
notes). Feel free to add additional context under any question.

## 1. Validation Implementation Approach

How should the "visit date must be today or later" rule be enforced? This repo
contains two established patterns: Bean Validation annotations on the entity
(e.g. `@NotBlank` already on `Visit.description`) and standalone `Validator`
classes (e.g. `PetValidator`).

- [x] (A) Add a Jakarta Bean Validation annotation (`@FutureOrPresent`) to the
      `Visit.date` field. The existing `@Valid` check in
      `VisitController.processNewVisitForm` already triggers it, so no controller
      change is needed.
- [ ] (B) Create a custom `VisitValidator` class (mirroring `PetValidator`) and
      wire it into the controller via `@InitBinder`.
- [ ] (C) Add a controller-level check in `processNewVisitForm` that rejects the
      `date` field on the `BindingResult` when it is before today.
- [ ] (D) Other (describe)

**Current best-practice context:** Jakarta Bean Validation (bundled with Spring
Boot 4) provides `@FutureOrPresent`, which means "now or in the future" evaluated
against the system clock and maps exactly to "today or later" for a `LocalDate`.
It is the standard, declarative way to express this constraint and sits next to
the `@NotBlank` already on the same entity.

**Recommended answer(s):** [(A)]

**Why these are recommended:**

- `(A)` is the smallest, most idiomatic change: one annotation, zero controller
  or template edits, and it reuses the `@Valid` path already in place — closest
  to the issue's "keep the rule simple" guidance.
- `(A)` is consistent with the existing `@NotBlank` on `Visit.description`, so the
  entity stays the single source of truth for its own field constraints.
- `(B)` adds a whole class and `@InitBinder` wiring for a single one-line rule —
  more machinery than the rule warrants, though it's the right pattern when
  validation is complex or cross-field (as `PetValidator` is).
- `(C)` works but spreads field-validation logic into the controller, diverging
  from how the rest of the visit/pet/owner forms declare field rules.

## 2. Validation Message Text and Internationalization

What message should the user see when they submit a past date, and how should it
be internationalized?

- [x] (A) Add a new message key (e.g. `visit.date.future` = "must be today or a
      future date") to `messages.properties` and all 8 locale bundles, and
      reference it from the constraint. Fully translated and consistent with the
      repo's i18n approach.
- [ ] (B) Add the new key to the default `messages.properties` only (and the
      English bundle), accepting that other locales fall back to the default
      English text for this one message.
- [ ] (C) Use the framework default message for `@FutureOrPresent` ("must be a
      date in the present or in the future") with no new key.
- [ ] (D) Other (describe / provide preferred wording)

**Current best-practice context:** The repo enforces locale-bundle key parity via
`I18nPropertiesSyncTest`, so any new key added to one bundle must be added to all
9 bundles or the test fails. The acceptance criteria call for "a clear validation
message."

**Recommended answer(s):** [(A)]

**Why these are recommended:**

- `(A)` gives a clear, purpose-written message and keeps `I18nPropertiesSyncTest`
  green by adding the key to every bundle — matching how the repo already handles
  custom messages (e.g. `telephone.invalid`).
- `(A)` lets us assert on a known, stable message string in both the JUnit and
  Playwright proofs.
- `(B)` would break `I18nPropertiesSyncTest` (key parity), so it is not viable
  without changing that test's contract.
- `(C)` avoids new keys but the default phrasing is generic and we'd be asserting
  on framework-owned text that could change between versions.
- For `(A)`, a suggested English string is **"must be today or a future date"**;
  edit here if you prefer different wording.
