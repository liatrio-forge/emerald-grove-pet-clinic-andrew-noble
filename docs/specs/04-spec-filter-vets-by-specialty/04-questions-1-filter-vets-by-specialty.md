# 04 Questions Round 1 - Filter Veterinarians by Specialty

Please answer each question below (check one or more options, or add your own
notes). Feel free to add additional context under any question.

## 1. Meaning of "None is handled sensibly"

GitHub issue #2 says: *"An 'All' option shows all vets; 'None' is handled
sensibly."* In the seed data, some vets (e.g., James Carter, Sharon Jenkins)
have **no** specialty assigned. We need to decide what "None" means for the
filter.

- [ ] (A) Add an explicit **"No specialty"** option to the filter that shows
      only vets who have zero specialties (in addition to "All" and each named
      specialty).
- [ ] (B) "None" just means "no filter applied" — i.e., the default state is
      "All", and there is no dedicated option for vets without a specialty.
- [x] (C) Both: include an "All" default **and** a separate "No specialty"
      option for vets with zero specialties.
- [ ] (D) Other (describe)

> **Answer:** (C) — accepted recommendation.

**Recommended answer(s):** [(C)]

**Why these are recommended:**

- `(C)` gives the clearest, most testable meaning to "handled sensibly": "All"
  is the obvious default, and "No specialty" makes the vets without specialties
  (Carter, Jenkins) reachable and demonstrable in a proof artifact.
- `(A)` is essentially `(C)` but risks ambiguity about whether "All" is still
  present; `(C)` states both explicitly.
- `(B)` is the smallest option but leaves vets-without-specialty undiscoverable
  by filter, which is a weaker interpretation of "handled sensibly".

## 2. Filter control style

How should the filter control look and behave on the Vet Directory page?

- [x] (A) A **dropdown `<select>`** of specialties that, when changed, reloads
      the page with a `?specialty=...` query parameter (submits via a small
      inline form / native submit).
- [ ] (B) A **row of clickable links/chips** (one per specialty, plus
      All / No specialty), each linking to the page with the appropriate
      `?specialty=...` parameter — matching the existing plain-link pagination
      style, no JavaScript needed.
- [ ] (C) Other (describe)

> **Answer:** (A) — accepted recommendation.

**Recommended answer(s):** [(A)]

**Why these are recommended:**

- `(A)` is the conventional pattern for "filter by a single value", scales
  cleanly if more specialties are added, and matches the Bootstrap-styled
  dropdown precedent set by the language selector in spec 03.
- `(B)` is the simplest (pure links, consistent with pagination) and is a fine
  choice if you prefer zero JavaScript; the tradeoff is it gets visually busy as
  the specialty count grows.
- Either option supports shareable query-param URLs, which the issue requests.

## 3. Query parameter naming

The issue prefers query-param support so filtered URLs can be shared. What
should the parameter look like?

- [x] (A) Specialty **name**, e.g., `/vets.html?specialty=surgery` (human
      readable, shareable).
- [ ] (B) Specialty **id**, e.g., `/vets.html?specialtyId=2`.
- [ ] (C) Other (describe)

> **Answer:** (A) — accepted recommendation.

**Recommended answer(s):** [(A)]

**Why these are recommended:**

- `(A)` produces self-describing, shareable URLs (the issue's stated goal) and
  is stable against id changes across environments/seed data.
- `(B)` is slightly simpler to look up but produces opaque URLs and couples
  shared links to database ids.

## 4. Interaction with existing pagination

The Vet Directory is paginated (5 per page). When a filter is applied, what
should happen to pagination?

- [x] (A) Applying or changing a filter **resets to page 1**, and pagination
      then operates over the filtered result set (page links carry the
      `?specialty=...` param so paging preserves the filter).
- [ ] (B) Keep the current page number when filtering (may land on an empty
      page if the filtered set is small).
- [ ] (C) Other (describe)

> **Answer:** (A) — accepted recommendation.

**Recommended answer(s):** [(A)]

**Why these are recommended:**

- `(A)` avoids the confusing "empty page" problem and is the standard behavior
  users expect when changing a filter.
- Carrying the filter param through pagination links keeps filtered URLs fully
  shareable, including the page.
