# Task 02 Proofs - RFC 4180 CSV formatting, content type, and download headers

## Task Summary

This task proves the `/owners.csv` response is a well-formed, downloadable CSV
file: correct `text/csv` content type, a `Content-Disposition` attachment header,
a header row in the required column order, CRLF line terminators, and RFC 4180
escaping for fields containing commas, quotes, or newlines.

## What This Task Proves

- The response sets `Content-Type: text/csv` and
  `Content-Disposition: attachment; filename="owners.csv"` (browser downloads it).
- The first line is exactly `firstName,lastName,address,city,telephone`.
- Each owner row renders fields in column order, terminated by CRLF (`\r\n`).
- Fields containing a comma, double quote, or newline are quoted, with embedded
  quotes doubled, per RFC 4180.

## Evidence Summary

- `OwnerCsvExportControllerTests` now runs 9 tests (5 from Task 01 + 4 new
  formatting tests), all passing.
- The escaping test feeds an owner with a quoted/comma last name
  (`Smith "Jr", III`) and a newline in the address, and asserts the exact escaped
  output.

## Artifact: OwnerCsvExportControllerTests passes (9 tests)

**What it proves:** All formatting, header, and escaping behaviors are verified by
automated web-layer tests.

**Why it matters:** CSV correctness (especially escaping and line endings) is easy
to get subtly wrong; these tests lock the RFC 4180 behavior in place.

**Command:**

```bash
./mvnw test -Dtest=OwnerCsvExportControllerTests
```

**Result summary:** 9 tests passed, 0 failures, 0 errors; build succeeded.

```text
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.944 s -- in org.springframework.samples.petclinic.owner.OwnerCsvExportControllerTests
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Test-to-behavior mapping (new in Task 02)

| Test | Behavior proven |
| --- | --- |
| `exportSetsCsvContentTypeAndAttachmentDownloadHeader` | `text/csv` + `Content-Disposition: attachment; filename="owners.csv"` |
| `exportFirstLineIsHeaderRowInColumnOrder` | header row present, correct column order, CRLF after it |
| `exportRendersFieldsInColumnOrderTerminatedByCrlf` | full body equals header + one CRLF-terminated data row |
| `exportEscapesFieldsContainingCommasQuotesAndNewlines` | RFC 4180 quoting + doubled embedded quotes |

## Reviewer Conclusion

The endpoint emits a standards-compliant, downloadable CSV: correct headers, column
order, CRLF terminators, and safe escaping — all verified by passing automated
tests.
