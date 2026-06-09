# 03-spec-header-language-selector.md

## Introduction/Overview

The application already supports multiple UI languages through Spring's
internationalization (i18n) infrastructure, but a user can only change the
language by manually editing the URL (e.g., `?lang=es`). This feature adds a
visible **language selector** to the global header so users can switch the UI
language by clicking, using the locale support that already exists. The primary
goal is to make existing translations discoverable and usable without requiring
any knowledge of URL parameters.

## Goals

- Expose a language selector in the global header that is visible on every page.
- Allow users to switch the UI between English, Spanish, and German by clicking.
- Re-render the current page in the chosen language without losing the user's place.
- Keep the selected language in effect for the rest of the session (using the
  existing session-based locale persistence).
- Visually indicate which language is currently active.

## User Stories

- **As a non-English-speaking visitor**, I want to pick my language from the
  header so that I can read the site in a language I understand without editing
  the URL.
- **As a returning user during a session**, I want my chosen language to stay
  applied as I navigate so that I don't have to re-select it on every page.
- **As any user**, I want to see which language is currently active so that I
  know whether my selection took effect.

## Demoable Units of Work

### Unit 1: Language selector in the header

**Purpose:** Add a clickable language selector to the global header so users on
any page can switch between English, Spanish, and German, with the current
language clearly indicated.

**Functional Requirements:**

- The system shall display a language selector in the global header
  (`src/main/resources/templates/fragments/layout.html`) so that it appears on
  every page that uses the shared layout.
- The selector shall be a Bootstrap dropdown, styled consistently with the
  existing navbar, that expands to show the available languages.
- The selector shall offer exactly three languages, labeled with their native
  names: "English", "Español", and "Deutsch".
- When the user selects a language, the system shall reload the current page
  with the corresponding `?lang=` parameter (`en`, `es`, or `de`) appended,
  so the user stays on the same page rendered in the new language.
- The system shall visually indicate the currently active language within the
  selector (e.g., an active/checked state on the matching option).
- The system shall use the existing `LocaleChangeInterceptor` and
  `SessionLocaleResolver` (in `WebConfiguration.java`) to apply and persist the
  language; no changes to the locale-resolution mechanism are required.
- The selector's own label/options shall use i18n message keys so the selector
  itself is translatable.

**Proof Artifacts:**

- Screenshot: the home page header showing the language dropdown expanded with
  English, Español, and Deutsch options — demonstrates the selector is present
  and lists the three languages.
- Screenshot: the same page rendered in English and again in Spanish (nav labels
  such as "Home"/"Inicio" and "Find owners"/"Buscar propietarios" differ) —
  demonstrates that selecting a language changes visible UI text.
- Screenshot: the selector showing the active language indicated after a switch
  — demonstrates the active-state requirement.

### Unit 2: End-to-end verification of language switching and persistence

**Purpose:** Prove, through an automated browser test, that switching language
updates visible text and that the choice persists across navigation within the
same session.

**Functional Requirements:**

- The system shall provide a Playwright E2E test under
  `e2e-tests/tests/features/` that opens a page, switches the language via the
  header selector, and asserts that visible UI text changes to the selected
  language.
- The test shall navigate to a different page after switching and assert that
  the UI remains in the selected language, demonstrating session persistence.
- The test shall assert that the selector reflects the active language after a
  switch.

**Proof Artifacts:**

- Test: `e2e-tests/tests/features/language-selector.spec.ts` passes —
  demonstrates that the selector switches language, that translated text
  appears, and that the language persists across navigation.
- Playwright HTML report entry (`e2e-tests/test-results/html-report/`) showing
  the new test passing — demonstrates end-to-end functionality in a real browser.

## Non-Goals (Out of Scope)

1. **Adding or editing translations**: The EN/ES/DE message bundles already
   exist; this feature does not add new languages or change existing
   translation strings (beyond adding keys for the selector's own labels).
2. **Cross-session / persistent preference storage**: Persisting the language
   beyond the current session (e.g., via cookies, user profile, or database) is
   out of scope; persistence relies on the existing session resolver only.
3. **Exposing all available bundles**: Languages other than EN/ES/DE
   (fa, ko, pt, ru, tr) will not be shown in the selector in this version.
4. **Changing the locale-resolution mechanism**: The `SessionLocaleResolver`,
   `LocaleChangeInterceptor`, and `lang` parameter name remain unchanged.
5. **Auto-detecting browser language**: Defaulting to the browser's
   `Accept-Language` header is out of scope; the default remains English.

## Design Considerations

- The selector lives in the right-aligned navbar list (`ms-auto`) of
  `fragments/layout.html`, alongside the existing Home / Find owners / Vets /
  Error items, styled as a Bootstrap dropdown to match the current dark navbar.
- Each option links to the current page URL with the appropriate `?lang=`
  parameter so the user stays in context.
- The active language is indicated using Bootstrap's active/`disabled` styling
  (or an equivalent visual marker) on the matching dropdown item, and the
  dropdown toggle may show the current language.
- The selector and its options should be keyboard-accessible and use the
  navbar's existing responsive (collapsible) behavior on small screens.

## Repository Standards

- **i18n**: Reuse the existing message-bundle pattern under
  `src/main/resources/messages/` (`messages.properties`, `messages_es.properties`,
  `messages_de.properties`). Add any new keys (e.g., a "Language" label) to all
  relevant bundles, mirroring the existing key style.
- **Templating**: Follow existing Thymeleaf conventions in `layout.html`
  (`th:text="#{key}"`, `th:href="@{...}"`, fragment-based markup).
- **E2E tests**: Follow the established Page Object Model under
  `e2e-tests/tests/` (pages in `tests/pages/`, feature specs in
  `tests/features/*.spec.ts`), consistent with existing specs such as
  `base-page-navigation.spec.ts`.
- **Commits**: Use Conventional Commits, and perform work on a feature branch
  (direct commits to `main` are blocked by the `no-direct-commits-to-main`
  pre-commit hook).
- **TDD**: Per `CLAUDE.md`, write the failing E2E test (and any unit/MVC test)
  before the implementation, following Red-Green-Refactor.

## Technical Considerations

- The Spring i18n plumbing already exists: `LocaleChangeInterceptor`
  (param `lang`) and `SessionLocaleResolver` (default English) are configured in
  `WebConfiguration.java`. The selector only needs to issue a request carrying
  the `lang` parameter; persistence across the session is automatic.
- Building each option's link as "current page + `?lang=xx`" must preserve any
  existing query parameters and path. Use Thymeleaf URL building that targets
  the current request URI rather than a hard-coded path so the user stays on the
  same page.
- The current locale is available in templates via `${#locale.language}` (already
  used as `th:lang` on the `<html>` element), which can drive the active-state
  indicator.
- No new runtime dependencies are required; Bootstrap (for the dropdown) and
  Thymeleaf are already in use.
- Adding a header element changes shared markup, so a quick check of existing
  controller/MVC tests and E2E specs that assert on header/navbar content is
  needed to avoid regressions.

## Security Considerations

- The `lang` value is consumed by Spring's `LocaleChangeInterceptor`, which maps
  it to a `Locale`; the selector should only ever emit the known values `en`,
  `es`, and `de`, avoiding reflection of arbitrary user input into the page.
- No credentials, tokens, or sensitive data are involved.
- Proof artifacts are screenshots and test reports containing only public UI;
  nothing sensitive should be committed. Follow the repository's existing
  handling of `e2e-tests/test-results/` artifacts.

## Success Metrics

1. **Visibility**: The language selector renders in the header on 100% of pages
   that use the shared layout (verified on home, find-owners, and vets pages).
2. **Functional switching**: Selecting Spanish or German changes visible nav
   labels to the corresponding language (e.g., "Home" → "Inicio" / "Startseite").
3. **Persistence**: After switching language, navigating to another page keeps
   the UI in the selected language within the same session.
4. **Automated proof**: The new Playwright spec passes in CI, covering switch,
   translated text, active indicator, and cross-navigation persistence.

## Open Questions

No open questions at this time.
