# 03-tasks-header-language-selector.md

Tasks derived from
[`03-spec-header-language-selector.md`](./03-spec-header-language-selector.md).

> Each parent task is a demoable vertical slice. Per repository standards
> (`AGENTS.md`, `README.md`), every slice follows **Strict TDD**: write the
> failing test first (RED), implement the minimum to pass (GREEN), then refactor.

## Relevant Files

| File | Why It Is Relevant |
| --- | --- |
| `src/main/resources/templates/fragments/layout.html` | Shared header/navbar template; the language-selector dropdown is added to the `ms-auto` nav list here so it appears on every page. |
| `src/main/resources/messages/messages.properties` | Base (English) message bundle; add the `language` toggle label key. |
| `src/main/resources/messages/messages_es.properties` | Spanish bundle; add the Spanish `language` label. |
| `src/main/resources/messages/messages_de.properties` | German bundle; add the German `language` label. |
| `src/test/java/org/springframework/samples/petclinic/system/LanguageSelectorViewTests.java` | New MockMvc view test verifying the selector renders, option links carry `?lang=`, Spanish renders under `?lang=es`, and the active language is marked. |
| `src/main/java/org/springframework/samples/petclinic/system/WebConfiguration.java` | Existing locale config (`LocaleChangeInterceptor` + `SessionLocaleResolver`); read-only reference — must NOT be modified. |
| `e2e-tests/tests/pages/base-page.ts` | Shared Page Object for the global header; add selector helpers (`openLanguageMenu`, `selectLanguage`, `activeLanguage`). |
| `e2e-tests/tests/features/language-selector.spec.ts` | New Playwright spec proving switch, translated text, session persistence across navigation, and active-state. |

### Notes

- Java tests run with `./mvnw test`; follow the existing `@WebMvcTest`/`MockMvc`
  patterns under `src/test/java/.../system/` and `.../owner/`. To render the
  shared layout with full i18n resolution, prefer
  `@SpringBootTest(webEnvironment = MOCK)` + `@AutoConfigureMockMvc` (or an
  equivalent slice that loads the message bundles).
- E2E tests run with `cd e2e-tests && npm test`; follow the Page Object Model and
  `@pages`/`@fixtures` aliases used by existing specs (e.g.
  `base-page-navigation.spec.ts`). Playwright auto-starts the app and writes
  artifacts to `e2e-tests/test-results/`.
- Native language names ("English", "Español", "Deutsch") are locale-invariant
  and may be literal in the template; only the dropdown toggle label
  ("Language") needs a translatable message key.
- Do not modify `WebConfiguration.java`; the selector must reuse the existing
  `lang` parameter mechanism (spec Non-Goal #4).
- Work occurs on a feature branch (direct commits to `main` are blocked); use
  Conventional Commits.

## Tasks

### [ ] 1.0 Render the language selector in the header with switching and active-state

#### 1.0 Proof Artifact(s)

- Test: `LanguageSelectorViewTests` (MockMvc) asserting the rendered home page
  HTML contains a header language dropdown with three options whose links carry
  `?lang=en`, `?lang=es`, `?lang=de` and the native labels "English", "Español",
  "Deutsch" — passes, demonstrating the selector renders on the shared layout.
- Test: `LanguageSelectorViewTests` requesting `/?lang=es` asserts a Spanish nav
  label ("Inicio") is present and the Spanish option is marked active —
  demonstrates switching re-renders the current page and the active indicator.
- Screenshot: `e2e-tests/test-results/artifacts/lang-selector-expanded.png` of
  the home header with the dropdown expanded showing "English / Español /
  Deutsch" — demonstrates the selector is visible with the three native-name
  options.

#### 1.0 Tasks

- [ ] 1.1 (RED) Create `LanguageSelectorViewTests` with a test that GETs `/` and
  asserts the response HTML contains a language dropdown in the navbar with
  three option links carrying `?lang=en`, `?lang=es`, `?lang=de`. Run it and
  confirm it fails because the selector does not exist yet.
- [ ] 1.2 (RED) Add a test method that GETs `/?lang=es`, asserts a Spanish nav
  label (e.g. "Inicio") is rendered, and asserts the Spanish option has an
  `active` marker. Confirm it fails for the right reason.
- [ ] 1.3 Add a `language` label key to `messages.properties` (`Language`),
  `messages_es.properties` (`Idioma`), and `messages_de.properties` (`Sprache`),
  matching existing key style.
- [ ] 1.4 (GREEN) Add a Bootstrap dropdown to the `ms-auto` nav list in
  `fragments/layout.html`: a toggle labeled `#{language}` (showing the current
  language), and three `dropdown-item` links to the current request URI with
  `?lang=en|es|de` appended (preserving the existing path), labeled with the
  native names. Mark the item matching `${#locale.language}` as `active`.
- [ ] 1.5 (GREEN/REFACTOR) Run `LanguageSelectorViewTests` until green; tidy the
  template markup (accessibility attributes, consistent indentation) while
  keeping tests green.
- [ ] 1.6 Capture the expanded-dropdown screenshot for the proof artifact (manual
  run or a small Playwright snippet) and save it under
  `e2e-tests/test-results/artifacts/`.

### [ ] 2.0 Prove language switching and session persistence end-to-end (Playwright)

#### 2.0 Proof Artifact(s)

- Test: `e2e-tests/tests/features/language-selector.spec.ts` passes — switches
  language via the header selector, asserts visible nav text changes to the
  selected language, navigates to another page and asserts the UI stays in that
  language (session persistence), and asserts the selector reflects the active
  language.
- Playwright report: an entry in `e2e-tests/test-results/html-report/` showing
  `language-selector.spec.ts` passing — demonstrates end-to-end behavior in a
  real browser.
- Screenshot: `e2e-tests/test-results/artifacts/home-en.png` and `home-es.png`
  of the same page in English and Spanish — demonstrates visible UI text changes
  with the selection.

#### 2.0 Tasks

- [ ] 2.1 Extend the global header Page Object (`base-page.ts`) with
  `openLanguageMenu()`, `selectLanguage(name)`, and an `activeLanguage()` locator
  targeting the new dropdown.
- [ ] 2.2 (RED) Create `language-selector.spec.ts`: open the home page, switch to
  "Español", and assert nav text becomes Spanish (e.g. "Inicio" / "Buscar
  propietarios"). Run it (`npm test -- --grep "language"`) and verify the
  assertion drives behavior (fails if the template/POM is incomplete).
- [ ] 2.3 Add a persistence assertion: after switching to Spanish, navigate to
  Find Owners and then Veterinarians via the header and assert the nav/page text
  remains Spanish, and that `activeLanguage()` reports Spanish.
- [ ] 2.4 Capture `home-en.png` and `home-es.png` screenshots within the spec for
  the proof artifacts.
- [ ] 2.5 Run the spec to green and confirm the Playwright HTML report records it.

### [ ] 3.0 Guard against header regressions and finalize verification

#### 3.0 Proof Artifact(s)

- CLI: `./mvnw test` completes with `BUILD SUCCESS` — demonstrates the full Java
  suite (including existing navbar/controller/integration tests) still passes
  after the shared-layout change.
- CLI: `cd e2e-tests && npm test` completes with all specs passing —
  demonstrates the E2E suite (existing + new) is green.
- Diff: changes to `fragments/layout.html` and the `messages*.properties`
  bundles — demonstrates the implementation surface and that EN/ES/DE selector
  labels were added consistently.

#### 3.0 Tasks

- [ ] 3.1 Run `./mvnw test`; if any existing test that asserts on navbar/header
  content breaks due to the new element, update that assertion (not the
  requirement) and document why.
- [ ] 3.2 Run `cd e2e-tests && npm test`; confirm all specs (existing navigation
  specs + new `language-selector.spec.ts`) pass.
- [ ] 3.3 Confirm new Java code meets the ≥90% line-coverage standard via the
  JaCoCo report from `./mvnw test` (the change is template-driven, so verify no
  new uncovered Java was introduced).
- [ ] 3.4 Stage proof screenshots (sanitized, no secrets), commit on the feature
  branch with a Conventional Commit message, and confirm pre-commit hooks pass
  (`maven-test-check`, `markdownlint`, `no-direct-commits-to-main`).
