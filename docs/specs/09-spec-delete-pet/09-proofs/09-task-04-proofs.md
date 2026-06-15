# Task 04 Proofs - Internationalized message keys

## Task Summary

This task proves the new user-facing strings for the delete flow are externalized
as message keys and present in every translated locale bundle, preserving the
key-parity invariant enforced by `I18nPropertiesSyncTest`.

## What This Task Proves

- Five new keys exist in the base bundle: `deletePet`, `confirmDeletePet`,
  `deletePetWarning`, `deletePetVisitsWarning`, `cancel`.
- The same keys exist in all seven translated bundles (de, es, fa, ko, pt, ru,
  tr); `messages_en.properties` intentionally stays empty and falls back to base.
- `I18nPropertiesSyncTest` passes (both the parity check and the HTML
  non-internationalized-string scan).

## Evidence Summary

- Keys added to `messages.properties` (base) plus the 7 translated bundles.
- `deletePetVisitsWarning` uses a `{0}` placeholder for the visit count, consumed
  by Thymeleaf as `#{deletePetVisitsWarning(${visitCount})}`.
- The sync test passes, proving no locale is missing any base key.

## Artifact: i18n sync test run

**What it proves:** Every translated bundle contains all base keys and no
hard-coded HTML strings were introduced.

**Why it matters:** Partial translations or hard-coded UI text would break the
clinic's internationalization guarantees.

**Command:**

```bash
./mvnw test -Dtest=I18nPropertiesSyncTest
```

**Result summary:** Both i18n tests pass, BUILD SUCCESS.

```text
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in ...system.I18nPropertiesSyncTest
[INFO] BUILD SUCCESS
```

## Artifact: Added keys (base bundle)

**What it proves:** The English source strings are externalized.

**Why it matters:** Shows the canonical text the other locales translate.

**Artifact path:** `src/main/resources/messages/messages.properties`

**Result summary:** The five keys were appended to the base bundle and mirrored in
each translated bundle.

```properties
deletePet=Delete Pet
confirmDeletePet=Are you sure you want to delete this pet?
deletePetWarning=This action cannot be undone.
deletePetVisitsWarning=This will also permanently delete {0} visit(s).
cancel=Cancel
```

## Reviewer Conclusion

All new strings are internationalized with full locale-key parity, verified by the
automated sync test.
