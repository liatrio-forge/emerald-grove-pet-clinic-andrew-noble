# Task 03 Proofs - Internationalization and navigation discoverability

## Task Summary

This task proves all user-facing strings on the Upcoming Visits page are
internationalized with parity across every locale, and that the page is
reachable from the shared navigation menu.

## What This Task Proves

- Three new message keys (`upcomingVisits`, `upcomingVisits.subtitle`,
  `upcomingVisits.none`) exist in the base file and all translated locales.
- `I18nPropertiesSyncTest` passes: no hardcoded HTML strings and full locale
  key parity.
- A navigation link to `/visits/upcoming` is present in the shared layout and
  highlights as active on the page.

## Evidence Summary

- `I18nPropertiesSyncTest` runs 2 tests, 0 failures.
- Live app: the nav link renders on every page; on `/visits/upcoming` the link is
  marked `active`; all page strings resolve (no `??key??` placeholders).

## Artifact: I18nPropertiesSyncTest passes

**What it proves:** New keys are present in all locales and no hardcoded strings
were introduced in the template.

**Why it matters:** This is the repository's enforced guard against partial
translations and non-internationalized UI text.

**Command:**

```bash
./mvnw test -Dtest=I18nPropertiesSyncTest
```

**Result summary:** Both the hardcoded-string scan and the locale-parity check
pass.

```text
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.076 s -- in org.springframework.samples.petclinic.system.I18nPropertiesSyncTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Artifact: Navigation link added to shared layout

**What it proves:** The page is discoverable from the main menu.

**Why it matters:** The acceptance criteria require the page to exist and be
reachable; a nav entry makes it usable without typing the URL.

**Diff (`src/main/resources/templates/fragments/layout.html`):**

```html
<li th:replace="~{::menuItem ('/visits/upcoming','upcomingVisits','upcoming visits','calendar',#{upcomingVisits})}">
  <span class="fa fa-calendar" aria-hidden="true"></span>
  <span th:text="#{upcomingVisits}">Upcoming Visits</span>
</li>
```

**Live result summary:** The link renders on the home page and is marked
`active` when on the Upcoming Visits page.

```html
<!-- on / -->
<a class="nav-link" href="/visits/upcoming" title="upcoming visits"><span>Upcoming Visits</span></a>
<!-- on /visits/upcoming -->
<a class="nav-link active" href="/visits/upcoming" title="upcoming visits"><span>Upcoming Visits</span></a>
```

## Artifact: Page strings resolve in the running app

**What it proves:** The i18n keys resolve to real text on the rendered page.

**Why it matters:** Confirms the keys are wired correctly end-to-end (compare to
Task 2 where the keys were not yet defined and rendered as `??...??`).

**Command & result summary:**

```bash
curl -s http://localhost:8080/visits/upcoming
```

```html
<h2>Upcoming Visits</h2>
<p class="liatrio-muted">Visits scheduled within the next 7 days.</p>
<p class="liatrio-muted">There are no upcoming visits in this window.</p>
```

> A rendered browser screenshot of the navigation bar and page is captured in
> Task 04 proofs (Playwright run).

## Reviewer Conclusion

The Upcoming Visits page is fully internationalized with locale parity enforced
by `I18nPropertiesSyncTest`, and it is reachable and active-highlighted via the
shared navigation menu.
