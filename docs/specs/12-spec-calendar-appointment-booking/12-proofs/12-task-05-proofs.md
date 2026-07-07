# Task 05 Proofs - Internationalization Propagation & Documentation

## Task Summary

This task ensures every new user-facing string is present in the base bundle and all eight
locale files, and updates the documentation to describe the appointment/conflict/schedule
feature. Because the `maven-test-check` pre-commit hook runs the full suite (including
`I18nPropertiesSyncTest`) on every commit, the new keys were propagated to all locales in
the same commit that introduced them; this task verifies the end state and updates docs.

## What This Task Proves

- All new message keys resolve in every locale (no partial translations).
- No hard-coded UI strings were introduced (the i18n scanner passes).
- The README describes the new time-aware appointments, conflict detection, and schedule.
- The whole test suite is green.

## Evidence Summary

- `I18nPropertiesSyncTest` passes: keys are consistent across all locale files and no
  non-internationalized HTML literals were added.
- The full suite passes: 146 tests, 0 failures, 5 skipped (Docker-only MySQL/Postgres
  container tests).

## Artifact: New keys present in every locale

**What it proves:** Each new key exists in the base bundle and all eight locale files.

**Why it matters:** The sync test fails the build if any locale is missing a base key, so a
green build is proof of complete localization.

**Command:**

```bash
grep -c -E '^(veterinarian|appointmentTime|visit\.startTime\.required|visit\.vet\.required|visit\.conflict|schedule|schedule\.subtitle|schedule\.none|schedule\.prevDay|schedule\.nextDay)=' \
  src/main/resources/messages/messages*.properties
```

**Result summary:** The base file and all seven non-English locale files each contain the
10 new keys (English is served from the base bundle via fallback).

## Artifact: Full suite green

**What it proves:** Every unit, web-layer, and integration test passes with the feature in
place.

**Command:**

```bash
./mvnw test
```

**Result summary:** 146 tests, 0 failures, 5 skipped.

```text
[WARNING] Tests run: 146, Failures: 0, Errors: 0, Skipped: 5
[INFO] BUILD SUCCESS
```

## Artifact: Documentation updated

**What it proves:** The README's feature list now reflects time-aware appointments,
conflict detection, and the day schedule.

**Artifact path:** `README.md` (Application Features section)

**Result summary:** Visits are described as time-aware with an assigned vet; new
"Conflict Detection" and "Day Schedule" entries were added. `markdownlint` passes as part
of the pre-commit hook.

## Reviewer Conclusion

All new strings are fully localized across every supported language, no hard-coded UI text
was introduced, the documentation reflects the feature, and the entire test suite is green.
