# 11-tasks-export-owners-csv.md

Task list derived from
[`11-spec-export-owners-csv.md`](11-spec-export-owners-csv.md).

## Relevant Files

| File | Why It Is Relevant |
| --- | --- |
| `src/main/java/org/springframework/samples/petclinic/owner/OwnerCsvExportController.java` | New `@Controller` serving `GET /owners.csv`; reads `lastName`/`city`/`telephone`, calls `findByOptionalCriteria` unpaged, writes the CSV body and download headers. |
| `src/main/java/org/springframework/samples/petclinic/owner/OwnerRepository.java` | Existing repository; its `findByOptionalCriteria(lastName, city, telephone, Pageable)` query is reused with `Pageable.unpaged()` (no change expected). |
| `src/main/java/org/springframework/samples/petclinic/owner/Owner.java` | Existing entity; source of the exported `firstName`, `lastName`, `address`, `city`, `telephone` fields. |
| `src/test/java/org/springframework/samples/petclinic/owner/OwnerCsvExportControllerTests.java` | New `@WebMvcTest` covering filtering, unpaged retrieval, blank/no-match paths, content type, download header, header row, and RFC 4180 escaping. |
| `docs/specs/11-spec-export-owners-csv/11-proofs/11-proofs-export-owners-csv.md` | New proof document holding `curl` snippets (filtered/unfiltered export, response headers) and the `./mvnw test` result. |

### Notes

- Strict TDD: write each failing test first (RED), implement the minimum to pass
  (GREEN), then refactor. Never write production code before a failing test.
- Java tests run with `./mvnw test` (optionally `-Dtest=OwnerCsvExportControllerTests`);
  follow the Arrange-Act-Assert pattern with AssertJ/Hamcrest, matching
  `OwnerControllerTests`.
- Place the new test alongside the package under test
  (`org.springframework.samples.petclinic.owner`).
- Reuse `OwnerRepository.findByOptionalCriteria` with `Pageable.unpaged()`; do not
  add a duplicate search query. `ClinicServiceTests` already exercises this method
  with `Pageable.unpaged()`.
- Keep CSV generation dependency-free (hand-rolled, RFC 4180 compliant). If a
  helper class is extracted in REFACTOR, place it in the same package.
- Work on the existing feature branch; conventional commits; the pre-commit hook
  runs the full Maven suite and blocks direct commits to `main`.

## Tasks

### [x] 1.0 CSV export endpoint returns owners filtered by search criteria

Add a `GET /owners.csv` endpoint that reuses the existing
`OwnerRepository.findByOptionalCriteria` query, respects the optional `lastName`,
`city`, and `telephone` parameters (combined with AND), treats blank/missing
parameters as "no filter," and returns **every** matching owner (unpaged) rather
than a single page. This parent task covers the data-selection behavior of the
export; CSV formatting/header concerns are handled in 2.0.

#### 1.0 Proof Artifact(s)

- Test: `OwnerCsvExportControllerTests` (`@WebMvcTest`, `@MockitoBean
  OwnerRepository`) passes, demonstrating `GET /owners.csv` returns HTTP 200 and
  one data row per matching owner.
- Test: A test asserting `findByOptionalCriteria` is invoked with the supplied
  `lastName`/`city`/`telephone` (and an unpaged `Pageable`) passes, demonstrating
  the criteria are honored and pagination is not applied.
- Test: A test asserting a parameterless `GET /owners.csv` returns all owners, and
  a non-matching filter returns only the header row, passes — demonstrating the
  blank-filter and no-match paths.
- CLI: `./mvnw test -Dtest=OwnerCsvExportControllerTests` exits `0`.

#### 1.0 Tasks

- [x] 1.1 (RED) Create `OwnerCsvExportControllerTests` (`@WebMvcTest(OwnerCsvExportController.class)`,
  `@DisabledInNativeImage`, `@DisabledInAotMode`, `@MockitoBean OwnerRepository`)
  with a reusable owner fixture; assert `GET /owners.csv` returns HTTP 200.
- [x] 1.2 (RED) Add a test that stubs `findByOptionalCriteria(...)` to return two
  owners and asserts the response body contains one data row per owner (two data
  rows beneath the header).
- [x] 1.3 (RED) Add a test using Mockito `ArgumentCaptor` (or `eq`/`any` matchers)
  asserting the controller calls `findByOptionalCriteria` with the request's
  `lastName`/`city`/`telephone` values and an unpaged `Pageable`
  (`Pageable.isUnpaged()` is true).
- [x] 1.4 (RED) Add tests for the blank-filter path (parameterless `GET /owners.csv`
  passes `""`/`""`/`""` and returns all stubbed owners) and the no-match path
  (empty result returns HTTP 200 with only the header row, no data rows).
- [x] 1.5 (GREEN) Implement `OwnerCsvExportController` with `@GetMapping("/owners.csv")`,
  reading optional `lastName`/`city`/`telephone` params (defaulting null/absent to
  `""`), and calling `owners.findByOptionalCriteria(lastName, city, telephone,
  Pageable.unpaged())`; return the matching owners' rows (formatting handled in 2.0).
- [x] 1.6 (GREEN) Confirm `GET /owners.csv` routes correctly despite the `.csv`
  suffix (no content-negotiation/path-extension interference); if it does not
  resolve, map via `produces = "text/csv"` and adjust the test accordingly.
- [x] 1.7 (REFACTOR) Add Javadoc consistent with `OwnerController`, remove
  duplication, and confirm tasks 1.1–1.4 pass via
  `./mvnw test -Dtest=OwnerCsvExportControllerTests`.

### [ ] 2.0 RFC 4180 CSV formatting, content type, and download headers

Make the response a well-formed, safely-escaped CSV file: `Content-Type:
text/csv`, `Content-Disposition: attachment; filename="owners.csv"`, a header row
`firstName,lastName,address,city,telephone`, one row per owner in that column
order, RFC 4180 escaping (quote-wrap values containing `,` `"` CR or LF; double
embedded quotes), CRLF row terminators, and UTF-8 encoding.

#### 2.0 Proof Artifact(s)

- Test: A test asserting the response `Content-Type` is `text/csv` and
  `Content-Disposition` equals `attachment; filename="owners.csv"` passes,
  demonstrating download behavior.
- Test: A test asserting the first line equals
  `firstName,lastName,address,city,telephone` passes, demonstrating the header row
  and column order.
- Test: A test feeding an owner whose field contains a comma and a double quote
  asserts the value is correctly quoted/escaped per RFC 4180, passing.
- CLI: `./mvnw test -Dtest=OwnerCsvExportControllerTests` exits `0`.

#### 2.0 Tasks

- [ ] 2.1 (RED) Add a test asserting the response `Content-Type` is `text/csv`
  (e.g. `header().string("Content-Type", containsString("text/csv"))`) and that
  `Content-Disposition` equals `attachment; filename="owners.csv"`.
- [ ] 2.2 (RED) Add a test asserting the first line of the body equals
  `firstName,lastName,address,city,telephone` (header row present, correct order).
- [ ] 2.3 (RED) Add a test asserting a normal owner row renders fields in column
  order and that rows are terminated with CRLF (`\r\n`).
- [ ] 2.4 (RED) Add an escaping test: an owner whose field contains a comma, a
  double quote, and/or a newline is wrapped in double quotes with embedded quotes
  doubled (e.g. `"Smith ""Jr"", III"`), per RFC 4180.
- [ ] 2.5 (GREEN) Implement CSV serialization in the controller: emit the header
  row, then one CRLF-terminated row per owner with values in the order
  `firstName,lastName,address,city,telephone`, applying RFC 4180 escaping (quote
  values containing `,` `"` CR or LF; double embedded quotes); set `Content-Type:
  text/csv` (UTF-8) and the `Content-Disposition` attachment header.
- [ ] 2.6 (REFACTOR) Optionally extract the escaping/row-building logic into a small
  package-private helper for clarity; ensure no duplication, then confirm all 2.x
  tests pass via `./mvnw test -Dtest=OwnerCsvExportControllerTests`.

### [ ] 3.0 Proof artifacts and documentation

Capture reproducible evidence the feature works end to end: `curl` snippets for a
filtered export and an unfiltered export saved to a proof document, and confirm
the full Maven suite passes (the pre-commit gate). Optionally add the Playwright
"download CSV and verify content" check noted in the issue.

#### 3.0 Proof Artifact(s)

- CLI: `curl -s "http://localhost:8080/owners.csv?lastName=Davis"` snippet in
  `11-proofs/11-proofs-export-owners-csv.md` demonstrates a filtered CSV download
  with header + matching rows.
- CLI: `curl -sD - "http://localhost:8080/owners.csv" -o /dev/null` snippet
  showing `Content-Type: text/csv` and the `Content-Disposition` header
  demonstrates correct response headers.
- CLI: `./mvnw test` exits `0`, demonstrating the full suite (the pre-commit gate)
  passes with the new code.

#### 3.0 Tasks

- [ ] 3.1 Start the app (`./mvnw spring-boot:run`, H2 default seed data) and capture
  a `curl -s "http://localhost:8080/owners.csv?lastName=Davis"` snippet showing the
  header row plus the matching owner rows.
- [ ] 3.2 Capture a `curl -sD - "http://localhost:8080/owners.csv" -o /dev/null`
  snippet showing the `Content-Type: text/csv` and
  `Content-Disposition: attachment; filename="owners.csv"` response headers.
- [ ] 3.3 Save both snippets (sanitized, seed data only) into
  `docs/specs/11-spec-export-owners-csv/11-proofs/11-proofs-export-owners-csv.md`
  with the exact commands used.
- [ ] 3.4 Run the full `./mvnw test` suite, confirm it exits `0`, and record the
  summary line in the proof document.
