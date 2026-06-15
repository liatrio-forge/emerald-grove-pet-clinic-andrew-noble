# 11-spec-export-owners-csv.md

## Introduction/Overview

The Emerald Grove Veterinary Clinic application lets staff search for owners by last
name, city, and telephone, then browse the results in a paginated HTML list. There is
currently no way to export those results for use in spreadsheets, mail merges, or other
systems. This feature adds a CSV export endpoint (`/owners.csv`) that returns all owners
matching the active search criteria as a downloadable `text/csv` file with a header row.
The primary goal is to give clinic staff a one-click way to extract owner search results
into a portable, machine-readable format.

## Goals

- Provide a `/owners.csv` endpoint that returns owner search results as RFC 4180-compliant
  CSV.
- Honor the same optional search criteria as the existing owners search (`lastName`,
  `city`, `telephone`), so the export matches what staff are filtering on.
- Return the complete matching result set (not a single page) so the export is a full
  dataset.
- Respond with the correct `text/csv` content type, a header row, and a
  `Content-Disposition` header that triggers a download named `owners.csv`.
- Implement the feature following the repository's strict TDD methodology with full test
  coverage.

## User Stories

- **As a clinic administrator**, I want to download my owner search results as a CSV file
  so that I can open them in a spreadsheet for reporting and record-keeping.
- **As a front-desk staff member**, I want the exported CSV to reflect the same filters I
  applied on screen (last name, city, telephone) so that the file contains exactly the
  owners I was looking at.
- **As a data-savvy user**, I want the CSV to have clearly labeled columns and properly
  escaped values so that I can reliably import it into other tools without manual cleanup.

## Demoable Units of Work

### Unit 1: CSV export endpoint returns filtered owners

**Purpose:** Deliver the core CSV export so staff can download owner search results that
respect the active search criteria.

**Functional Requirements:**

- The system shall expose an HTTP `GET` endpoint at `/owners.csv`.
- The system shall accept the same optional query parameters as the existing owners search:
  `lastName` (starts-with match), `city` (starts-with match), and `telephone` (exact
  match), combining them with AND.
- The system shall treat a missing or blank value for any criterion as "no filter on that
  field," so a parameterless request (`/owners.csv`) returns all owners.
- The system shall return every owner matching the criteria, ignoring the HTML list's
  page-size limit (no pagination applied to the export).
- The system shall reuse the existing `OwnerRepository.findByOptionalCriteria` query rather
  than introducing a duplicate search implementation.
- The system shall respond with HTTP `200 OK` and an empty body containing only the header
  row when no owners match the criteria.

**Proof Artifacts:**

- Test: `OwnerCsvExportControllerTests` (or additions to `OwnerControllerTests`) pass,
  demonstrating the endpoint returns matching owners and respects each criterion.
- CLI: `curl -s "http://localhost:8080/owners.csv?lastName=Davis"` output snippet in
  `11-proofs-export-owners-csv.md` demonstrates filtered results are returned as CSV.
- CLI: `curl -s "http://localhost:8080/owners.csv"` output snippet demonstrates all owners
  are exported when no filter is supplied.

### Unit 2: Correct CSV formatting, headers, and download behavior

**Purpose:** Ensure the response is a well-formed, safely-escaped CSV file that browsers and
tools treat as a download.

**Functional Requirements:**

- The system shall set the response `Content-Type` to `text/csv`.
- The system shall set a `Content-Disposition: attachment; filename="owners.csv"` header so
  the response is downloaded as a file.
- The system shall emit a header row with the columns, in order:
  `firstName,lastName,address,city,telephone`.
- The system shall emit one data row per owner, with field values in the same column order
  as the header.
- The system shall escape field values per RFC 4180: any value containing a comma, double
  quote, carriage return, or line feed shall be wrapped in double quotes, and embedded
  double quotes shall be doubled.
- The system shall separate rows with CRLF (`\r\n`) line endings, consistent with RFC 4180.

**Proof Artifacts:**

- Test: A controller test asserting the `Content-Type` is `text/csv` and the
  `Content-Disposition` header equals `attachment; filename="owners.csv"` demonstrates
  correct download behavior.
- Test: A test feeding an owner whose field contains a comma/quote asserts the value is
  properly quoted/escaped, demonstrating RFC 4180 compliance.
- Test: A test asserting the first line equals `firstName,lastName,address,city,telephone`
  demonstrates the header row is present and correctly ordered.

## Non-Goals (Out of Scope)

1. **No new UI/download button**: This spec only adds the endpoint. Adding a "Download CSV"
   link to the owners list page is out of scope (it can be a follow-up).
2. **No new search criteria**: The export uses only the existing `lastName`, `city`, and
   `telephone` filters; no additional filtering or sorting options are introduced.
3. **No pet/visit data**: The export contains owner fields only (first name, last name,
   address, city, telephone) and does not include pets, visits, or owner IDs.
4. **No alternate export formats**: Excel (`.xlsx`), JSON, or PDF exports are not included.
5. **No authentication/authorization changes**: Access control for the endpoint matches the
   rest of the application (which is currently unauthenticated).
6. **No streaming/large-dataset optimization**: The export builds the full response in
   memory, which is acceptable for the application's small dataset.

## Design Considerations

No specific UI design requirements. This is a server-side endpoint with no rendered page.
The only user-visible behavior is the browser's file-download prompt when the endpoint is
accessed directly.

## Repository Standards

- **Strict TDD (Red-Green-Refactor)**: Write failing tests before implementation, per
  `CLAUDE.md` and `docs/DEVELOPMENT.md`. Never write production code before a failing test.
- **Coverage**: Meet the project's minimum 90% line coverage for new code and cover edge
  cases (blank criteria, no matches, escaping).
- **Web-layer testing**: Use `@WebMvcTest` with `MockMvc` and `@MockitoBean` for the
  controller test, following the patterns in `OwnerControllerTests`.
- **Code style**: Follow existing Spring MVC conventions in `OwnerController` (constructor
  injection, `@GetMapping`, package-private controller class) and the repository's
  Checkstyle configuration.
- **Conventional commits**: Use conventional commit messages (e.g., `feat:`, `test:`).
- **Commits via feature branch**: Work proceeds on a feature branch; direct commits to
  `main` are blocked by a pre-commit hook.

## Technical Considerations

- **Reuse existing query**: Implement the export by calling
  `OwnerRepository.findByOptionalCriteria(lastName, city, telephone, pageable)`. To return
  all matches without pagination, either request an unpaged result (e.g.,
  `Pageable.unpaged()`) or a sufficiently large page; prefer `Pageable.unpaged()` so no
  arbitrary cap is introduced. Confirm the query behaves correctly with `Pageable.unpaged()`
  during the GREEN phase; if not, add an unpaged repository method following Spring Data
  conventions.
- **Telephone validation**: The HTML search rejects a non-10-digit telephone with a
  validation error and re-renders the form. Because the CSV endpoint has no form to
  re-render, treat a provided telephone as an exact-match filter value; a non-matching or
  malformed telephone simply yields zero data rows (header only). Document this behavior.
- **CSV generation**: A small hand-rolled CSV writer is sufficient and avoids adding a new
  dependency; it must implement RFC 4180 escaping (quote-wrap values containing `,`, `"`,
  `\r`, or `\n`; double embedded quotes; CRLF row terminators). If a CSV library is
  preferred, it must be justified against the "minimal dependencies" preference during task
  planning.
- **Endpoint mapping**: Map `GET /owners.csv`. Verify the path `.csv` suffix does not
  collide with Spring's content-negotiation/path-extension handling; if necessary, produce
  the response via `produces = "text/csv"` and return the body explicitly (e.g.,
  `ResponseEntity<String>` or writing to the `HttpServletResponse`).
- **Character encoding**: Use UTF-8 for the response body.

## Security Considerations

- **No new secrets**: The feature introduces no API keys, tokens, or credentials.
- **CSV injection (formula injection)**: Owner-supplied fields exported to CSV could begin
  with `=`, `+`, `-`, or `@`, which spreadsheet programs may interpret as formulas. Note
  this risk in task planning; given the trusted internal dataset and "minimal" scope, RFC
  4180 quoting is the baseline requirement, and formula-injection neutralization (prefixing
  risky cells) may be deferred but should be explicitly acknowledged.
- **Data exposure**: The export returns the same owner data already visible in the HTML
  list, so it introduces no new data-exposure surface beyond making it downloadable.
- **Proof artifacts**: `curl` output committed to proof docs contains only sample/seed owner
  data already present in the repository; no real personal data should be committed.

## Success Metrics

1. **Functional correctness**: `GET /owners.csv` (with and without filters) returns the
   correct set of owners as CSV, verified by passing automated tests.
2. **Format compliance**: Response has `Content-Type: text/csv`, a header row, the
   `Content-Disposition` attachment header, and RFC 4180-compliant escaping, verified by
   tests.
3. **Coverage**: New code meets ≥90% line coverage with the full Maven test suite passing.
4. **Demoability**: A `curl` snippet in the proof doc reproduces a filtered CSV download.

## Open Questions

No open questions at this time.
