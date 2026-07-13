# Task 01 Proofs - Time- & Vet-Aware Appointment Data Model + Multi-DB Migration

## Task Summary

This task extends the `Visit` entity into a time-aware, vet-associated appointment and
migrates all four database profiles (h2, hsqldb, mysql, postgres) so the new columns
exist, are nullable for legacy rows, and existing seed visits are backfilled with a
`09:00` start time and a `NULL` vet. It is the schema/data foundation for conflict
detection (Task 3) and the day schedule (Task 4).

## What This Task Proves

- A `Visit` now persists a `startTime` (`LocalTime`) and an associated `Vet`, and both
  round-trip through the database via the owner aggregate.
- A fixed `APPOINTMENT_DURATION` (30 min) and a derived `getEndTime()` are available for
  later overlap math.
- The migration is applied consistently across all four DB profiles, with new columns
  nullable and legacy seed rows backfilled to `09:00` / `NULL` vet.
- No regressions: the Upcoming Visits query and the existing visit/owner tests still pass
  against the backfilled seed data.

## Evidence Summary

- New `@DataJpaTest` `shouldPersistVisitWithStartTimeAndVet` passes: a visit saved with
  `startTime = 09:30` and a `Vet` reloads with `startTime = 09:30`, `endTime = 10:00`, and
  the same vet id.
- `ClinicServiceTests` (15), `VisitControllerTests` (6), and `UpcomingVisitsRepositoryTests`
  (4) all pass — 25 tests, 0 failures.

## Artifact: Round-trip persistence test

**What it proves:** The new `start_time` column and `vet_id` relationship persist and
reload correctly through the `Owner -> Pet -> Visit` aggregate.

**Why it matters:** This is the core proof that the schema change and JPA mapping work
end-to-end, not just at compile time.

**Command:**

```bash
./mvnw test -Dtest="ClinicServiceTests,UpcomingVisitsRepositoryTests,VisitControllerTests"
```

**Result summary:** All three classes pass (25 tests, 0 failures). The Hibernate insert
`insert into visits (visit_date,description,start_time,vet_id,id) values (?,?,?,?,default)`
confirms the new columns are written; reload selects include `start_time` and the joined
`vets` columns.

```text
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in UpcomingVisitsRepositoryTests
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0 -- in VisitControllerTests
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0 -- in ClinicServiceTests
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Artifact: Multi-DB schema + seed migration

**What it proves:** Every DB profile gained `start_time TIME` + `vet_id` (nullable) with a
FK to `vets` and an index, and every seed insert was rewritten with `09:00` / `NULL` vet.

**Why it matters:** The app supports h2/hsqldb/mysql/postgres; a partial migration would
break startup on the un-migrated profiles.

**Artifact paths:**

- `src/main/resources/db/{h2,hsqldb,mysql,postgres}/schema.sql`
- `src/main/resources/db/{h2,hsqldb,mysql,postgres}/data.sql`

**Result summary:** H2/hsqldb add columns + `fk_visits_vets` + `visits_vet_id` index;
mysql adds `INDEX(vet_id)` + FK; postgres adds a `REFERENCES vets(id)` column + index.
Seed inserts now read e.g. `INSERT INTO visits VALUES (default, 7, '2013-01-01', '09:00', NULL, 'rabies shot');`.

## Reviewer Conclusion

The appointment model is now time- and vet-aware, the fixed duration and end-time helper
exist for downstream conflict logic, and all four DB profiles migrate consistently with no
regressions to existing behavior.
