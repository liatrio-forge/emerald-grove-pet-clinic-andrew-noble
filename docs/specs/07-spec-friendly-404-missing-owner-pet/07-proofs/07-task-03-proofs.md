# Task 03 Proofs - Recovery link back to Find Owners

## Task Summary

This task adds a recovery link to the friendly error page so users who land on a
not-found page can return to the owner search in one click. The link reuses the
existing localized `findOwners` message and points at `/owners/find`, so it
introduces no new translation keys and keeps the i18n sync guard green.

## What This Task Proves

- The error page contains a localized link to `/owners/find` (Unit 3 FR:
  recovery link present and localized).
- The new markup introduces no hardcoded literal text and no locale-file drift
  (`I18nPropertiesSyncTest` stays green).

## Evidence Summary

- `error.html` now contains a Bootstrap-styled anchor using
  `th:href="@{/owners/find}"` and `th:text="#{findOwners}"`.
- `I18nPropertiesSyncTest` (which fails on hardcoded HTML strings and on
  out-of-sync locale files) passes.

## Artifact: error.html recovery link markup

**What it proves:** A localized recovery link exists on the friendly error page.

**Why it matters:** Satisfies the spec requirement for a link back to Find
Owners, using i18n rather than hardcoded text.

**Artifact path:** `src/main/resources/templates/error.html`

**Result summary:** The link uses the existing `findOwners` message key and the
`/owners/find` route.

```html
<a class="btn btn-primary" th:href="@{/owners/find}" th:text="#{findOwners}">Find Owners</a>
```

## Artifact: i18n sync guard passes

**What it proves:** The new markup is fully internationalized and all
`messages_*.properties` files remain in sync (no new keys were added).

**Why it matters:** The repository enforces no untranslated strings; this
confirms the change complies.

**Command:**

```bash
./mvnw test -Dtest=I18nPropertiesSyncTest
```

**Result summary:** 2 tests run, 0 failures, 0 errors.

```text
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in I18nPropertiesSyncTest
[INFO] BUILD SUCCESS
```

## Reviewer Conclusion

The friendly error page now offers a localized one-click path back to Find
Owners without adding any translation keys, and the i18n guard confirms
compliance.
