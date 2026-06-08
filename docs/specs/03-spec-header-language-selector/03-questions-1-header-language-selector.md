# 03 Questions Round 1 - Header Language Selector

Please answer each question below (select one or more options, or add your own notes). Feel free to add additional context under any question.

> Context discovered in the codebase (so you can decide with full information):
>
> - Locale switching already works via `?lang=xx`, handled by `LocaleChangeInterceptor` in `WebConfiguration.java`.
> - A `SessionLocaleResolver` (default English) already **persists the chosen language for the whole session** — so AC #3 ("persists across navigation") is satisfied automatically by the existing infrastructure; the selector just needs to set the language once.
> - Message bundles already exist for `en`, `es`, and `de` (and others), so EN/ES/DE need no new translation files.
> - The header lives in `src/main/resources/templates/fragments/layout.html` (Bootstrap navbar, right-aligned `ms-auto` nav list).
> - E2E tests use a Page Object Model under `e2e-tests/tests/` with feature specs in `e2e-tests/tests/features/`.

## 1. Selector UI style

How should the language selector appear in the header?

- [x] (A) Bootstrap dropdown menu (a single "Language" / globe button that expands to a list of languages)
- [ ] (B) Inline links shown side by side in the navbar (e.g., `EN | ES | DE`)
- [ ] (C) Native HTML `<select>` dropdown that submits on change
- [ ] (D) Other (describe)

**Recommended answer(s):** [(A)]

**Why these are recommended:**

- `(A)` matches the existing Bootstrap navbar styling already used in `layout.html`, scales cleanly if more languages are added later, and gives a stable, accessible element to target in the Playwright test.
- `(B)` is simple but clutters the navbar and looks worse as the list grows; still a fine choice if you prefer zero added interaction.
- `(C)` requires extra JS to navigate on change and is harder to style consistently with the Bootstrap header.

## 2. Behavior when a language is selected

When the user picks a language, what should happen to the current page?

- [x] (A) Stay on the current page, re-rendered in the new language (append `?lang=xx` to the current URL)
- [ ] (B) Redirect to the home page in the new language
- [ ] (C) Other (describe)

**Recommended answer(s):** [(A)]

**Why these are recommended:**

- `(A)` is the least surprising behavior and is the standard pattern for header language switchers; the user keeps their place.
- Because the `SessionLocaleResolver` persists the choice, subsequent navigation stays translated regardless, so `(A)` simply avoids an unnecessary jump to home.
- `(B)` is easier to implement (one fixed link) but loses the user's context on every switch.

## 3. Language list and labels

Which languages should the first version expose, and how should each be labeled?

- [x] (A) EN / ES / DE, labeled with native names ("English", "Español", "Deutsch")
- [ ] (B) EN / ES / DE, labeled with uppercase codes ("EN", "ES", "DE")
- [ ] (C) Expose all available bundles (EN, ES, DE, FA, KO, PT, RU, TR)
- [ ] (D) Other (describe)

**Recommended answer(s):** [(A)]

**Why these are recommended:**

- The issue explicitly says "keep initial language list small (e.g., EN/ES/DE)," so `(A)`/`(B)` honor that scope; `(C)` contradicts the issue's guidance.
- Native names `(A)` are the accessibility best practice — a user looking for their language recognizes "Español"/"Deutsch" more reliably than a two-letter code.
- `(B)` is more compact and pairs well if you chose inline links in Q1; choose it only if you prefer brevity over clarity.

## 4. Indicating the active language

Should the selector visually indicate which language is currently active?

- [x] (A) Yes — mark the current language as active/checked (and ideally show it on the dropdown button)
- [ ] (B) No — just provide the options, no active-state indicator
- [ ] (C) Other (describe)

**Recommended answer(s):** [(A)]

**Why these are recommended:**

- `(A)` gives clear feedback that the switch worked and is a natural, low-cost addition; it also gives the Playwright test a concrete thing to assert beyond translated text.
- `(B)` is marginally simpler but provides no confirmation of the current state.
