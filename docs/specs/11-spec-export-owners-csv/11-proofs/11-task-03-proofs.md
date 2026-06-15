# Task 03 Proofs - End-to-end CSV export evidence (running app + full suite)

## Task Summary

This task proves the `/owners.csv` endpoint works end to end against the running
application with the default H2 seed data: a filtered export returns only matching
owners, an unfiltered export returns all owners, the response carries the correct
download headers, and the full Maven test suite passes.

## What This Task Proves

- A filtered request (`?lastName=Davis`) returns only the two seeded Davis owners.
- An unfiltered request returns all 10 seeded owners.
- The response headers are `Content-Type: text/csv` and
  `Content-Disposition: attachment; filename="owners.csv"`.
- The full `./mvnw test` suite passes with the new code (the pre-commit gate).

## Evidence Summary

- Live `curl` against `http://localhost:8080` (app started via `./mvnw
  spring-boot:run`, default `h2` profile) returns RFC 4180 CSV.
- Header inspection confirms the download behavior.
- The full test suite is green.

All data shown is the repository's built-in sample seed data — no real personal
data.

## Artifact: Filtered export (`?lastName=Davis`)

**What it proves:** The export honors the `lastName` search criterion (starts-with),
returning only matching owners beneath the header row.

**Why it matters:** This is the core user scenario from the issue — exporting the
results of an owner search.

**Command:**

```bash
curl -s "http://localhost:8080/owners.csv?lastName=Davis"
```

**Result summary:** Header row plus the two seeded "Davis" owners only.

```text
firstName,lastName,address,city,telephone
Betty,Davis,638 Cardinal Ave.,Sun Prairie,6085551749
Harold,Davis,563 Friendly St.,Windsor,6085553198
```

## Artifact: Unfiltered export (all owners)

**What it proves:** With no criteria, the export returns every owner (unpaged), not
just the first page of 5.

**Why it matters:** Confirms the "export all matches" decision — the file is a full
dataset, not a paginated slice.

**Command:**

```bash
curl -s "http://localhost:8080/owners.csv"
```

**Result summary:** Header row plus all 10 seeded owners (more than the 5-per-page
HTML list size, proving pagination is not applied).

```text
firstName,lastName,address,city,telephone
George,Franklin,110 W. Liberty St.,Madison,6085551023
Betty,Davis,638 Cardinal Ave.,Sun Prairie,6085551749
Eduardo,Rodriquez,2693 Commerce St.,McFarland,6085558763
Harold,Davis,563 Friendly St.,Windsor,6085553198
Peter,McTavish,2387 S. Fair Way,Madison,6085552765
Jean,Coleman,105 N. Lake St.,Monona,6085552654
Jeff,Black,1450 Oak Blvd.,Monona,6085555387
Maria,Escobito,345 Maple St.,Madison,6085557683
David,Schroeder,2749 Blackhawk Trail,Madison,6085559435
Carlos,Estaban,2335 Independence La.,Waunakee,6085555487
```

## Artifact: Response headers

**What it proves:** The response is typed as CSV and forces a download with a fixed
filename.

**Why it matters:** Confirms the download UX decision and the `text/csv` content
type from the acceptance criteria.

**Command:**

```bash
curl -sD - "http://localhost:8080/owners.csv" -o /dev/null
```

**Result summary:** HTTP 200 with `Content-Type: text/csv` and the attachment
`Content-Disposition`.

```text
HTTP/1.1 200
Content-Disposition: attachment; filename="owners.csv"
Content-Type: text/csv
```

## Artifact: Full Maven test suite

**What it proves:** The whole project's tests pass with the new endpoint in place.

**Why it matters:** This is the repository's pre-commit gate; a green suite is
required before the change can be committed/merged.

**Command:**

```bash
./mvnw test
```

**Result summary:** See the recorded summary line below (full build succeeded).

```text
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.576 s -- in org.springframework.samples.petclinic.PetClinicIntegrationTests
[WARNING] Tests run: 89, Failures: 0, Errors: 0, Skipped: 5
[INFO] BUILD SUCCESS
```

(The 5 skipped tests are the Docker/native-image integration tests that are
conditionally disabled in this environment; they are unrelated to this feature.)

## Reviewer Conclusion

Against the running application, the endpoint returns correctly filtered and
unfiltered CSV with the right download headers, and the full test suite passes —
demonstrating the feature works end to end.
