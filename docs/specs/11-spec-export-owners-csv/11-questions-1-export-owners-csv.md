# 11 Questions Round 1 - Export Owners Search Results as CSV

Please answer each question below (select one or more options, or add your own notes). Feel free to add additional context under any question.

## 1. Which search parameters should the CSV endpoint respect?

The existing `/owners` search (in `OwnerController.processFindForm`) filters by three
optional criteria combined with AND: `lastName` (starts-with), `city` (starts-with), and
`telephone` (exact). The issue text only names `lastName` explicitly.

- [x] (A) Respect all existing criteria: `lastName`, `city`, and `telephone`
- [ ] (B) Respect `lastName` only
- [ ] (C) Other (describe)

**Recommended answer(s):** (A)

**Why these are recommended:**

- `(A)` keeps the CSV export consistent with the on-screen search so the downloaded file
  matches exactly what the user is currently viewing/filtering. The issue's "e.g., `lastName`"
  reads as an example, not an exhaustive list.
- `(A)` reuses the existing `findByOptionalCriteria` repository method with no new query work.
- `(B)` would create a confusing mismatch where the export ignores city/telephone filters the
  user applied, producing more rows than expected.

## 2. Should the export include all matching owners, or only the current page?

The HTML owners list is paginated at 5 owners per page. A CSV export can either return every
owner matching the criteria or just the rows on the current page.

- [x] (A) Export ALL owners matching the criteria (ignore pagination)
- [ ] (B) Export only the current page (5 rows), honoring a `page` parameter
- [ ] (C) Other (describe)

**Recommended answer(s):** (A)

**Why these are recommended:**

- `(A)` matches the near-universal expectation of an "export" action: users want the complete
  result set in one file, not a 5-row slice.
- `(A)` makes the proof artifact (a `curl` snippet) cleaner and more meaningful.
- `(B)` would make the export far less useful and surprises users who expect a full dataset.
- Note: the PetClinic sample dataset is small (10 owners), so exporting all rows poses no
  performance concern here.

## 3. How should the owner's name be represented in the CSV columns?

The issue says "Keep columns minimal (name, address, city, telephone)." `name` could be one
combined column or split into first/last.

- [x] (A) Separate `firstName` and `lastName` columns (plus address, city, telephone)
- [ ] (B) Single combined `name` column ("First Last"), plus address, city, telephone
- [ ] (C) Other (describe)

**Recommended answer(s):** (A)

**Why these are recommended:**

- `(A)` mirrors the `Owner` domain model (which stores `firstName` and `lastName` separately)
  and is the more useful, machine-readable format for downstream spreadsheet/CRM use.
- `(A)` avoids lossy concatenation that consumers would have to split back apart.
- `(B)` is closer to the literal wording "name" and is acceptable if you prefer a single
  human-readable column; choose this if the export is meant purely for at-a-glance reading.

## 4. What filename should the download use, and should it force a download?

A CSV endpoint can either render inline in the browser or prompt a file download via a
`Content-Disposition` header.

- [x] (A) Force download with a fixed filename, e.g. `Content-Disposition: attachment; filename="owners.csv"`
- [ ] (B) Return `text/csv` inline with no `Content-Disposition` header (browser decides)
- [ ] (C) Other (describe)

**Recommended answer(s):** (A)

**Why these are recommended:**

- `(A)` gives a predictable filename and a clear download UX, and is friendly for the optional
  Playwright "download CSV" proof in the issue.
- `(B)` is simpler but leads to inconsistent browser behavior (some display CSV as text).
