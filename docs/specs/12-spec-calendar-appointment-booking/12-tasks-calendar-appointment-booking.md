# 12-tasks-calendar-appointment-booking.md

Task list for [12-spec-calendar-appointment-booking.md](12-spec-calendar-appointment-booking.md).

## Relevant Files

| File | Why It Is Relevant |
| --- | --- |
| `src/main/java/org/springframework/samples/petclinic/owner/Visit.java` | Add `startTime` (`LocalTime`), `@ManyToOne Vet` association, and the `APPOINTMENT_DURATION` constant. |
| `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java` | Provide the vet dropdown model attribute, validate vet+time, and invoke conflict detection on booking. |
| `src/main/java/org/springframework/samples/petclinic/owner/VisitRepository.java` | Add conflict queries (by vet+date, by pet+date) and the clinic-wide schedule query. |
| `src/main/java/org/springframework/samples/petclinic/owner/AppointmentConflictDetector.java` | **New.** Pure overlap rule (`startA < endB && startB < endA`) + repository-backed conflict check (critical business logic, unit-tested to 100% branch). |
| `src/main/java/org/springframework/samples/petclinic/owner/ScheduledAppointment.java` | **New.** DTO/view model projecting an appointment (owner id+name, pet, vet, date, start time, description) for the schedule page. |
| `src/main/java/org/springframework/samples/petclinic/owner/ScheduleController.java` | **New.** Read-only `GET /schedule` controller with resilient `date` handling. |
| `src/main/java/org/springframework/samples/petclinic/owner/VetFormatter.java` | **New.** `Formatter<Vet>` to bind the vet `<select>` value (id) to a `Vet`, mirroring `PetTypeFormatter`; empty selection → `null`. |
| `src/main/java/org/springframework/samples/petclinic/vet/VetRepository.java` | Source for the vet dropdown (`findAll()`, cached) and vet lookup used by `VetFormatter`. |
| `src/main/resources/templates/pets/createOrUpdateVisitForm.html` | Add the **Veterinarian** dropdown and **Time** input to the booking form. |
| `src/main/resources/templates/schedule/daySchedule.html` | **New.** Day schedule table, owner links, empty state, prev/next-day navigation. |
| `src/main/resources/templates/fragments/layout.html` | Add the "Schedule" nav-bar entry via the `menuItem` fragment. |
| `src/main/resources/db/{h2,hsqldb}/schema.sql` | Add `start_time` + `vet_id` columns, FK to `vets`, and index for the embedded DBs. |
| `src/main/resources/db/{postgres,mysql}/schema.sql` | Same schema changes for the persistent DB profiles. |
| `src/main/resources/db/{h2,hsqldb,postgres,mysql}/data.sql` | Backfill existing visit inserts with `start_time = 09:00` (vet_id NULL). |
| `src/main/resources/messages/messages.properties` (+ locale variants) | New keys: veterinarian, appointment time, required/conflict errors, schedule title/subtitle/empty-state, prev/next-day. |
| `src/test/java/org/springframework/samples/petclinic/owner/VisitControllerTests.java` | Extend with vet/time required, happy-path, and conflict-rejection cases. |
| `src/test/java/org/springframework/samples/petclinic/owner/AppointmentConflictDetectorTests.java` | **New.** Unit tests for overlap boundaries (partial vs back-to-back). |
| `src/test/java/org/springframework/samples/petclinic/owner/ScheduleControllerTests.java` | **New.** Web-layer tests for `/schedule` (default today, `date` param, fallback, empty state, ordering). |
| `src/test/java/org/springframework/samples/petclinic/owner/VetFormatterTests.java` | **New.** Tests for vet binding (valid id, empty → null). |
| `src/test/java/org/springframework/samples/petclinic/service/ClinicServiceTests.java` | Extend with `@DataJpaTest` coverage for new repository queries and persisting a vet+time appointment. |
| `src/test/java/org/springframework/samples/petclinic/system/I18nPropertiesSyncTest.java` | Existing guard ensuring new message keys are consistent across locale files. |

### Notes

- **Strict TDD (Red-Green-Refactor):** every sub-task that adds behavior is preceded by a failing test. Never write production code before a failing test (AGENTS.md).
- Tests use the established patterns: `@WebMvcTest` + `MockMvc` + `@MockitoBean` for controllers; `@DataJpaTest` for repositories; AAA structure; descriptive names (docs/TESTING.md).
- Run the suite with `./mvnw test`; run a single class with `./mvnw test -Dtest=ClassName`.
- All commits go on a feature branch (the `no-direct-commits-to-main` hook blocks `main`); the `maven-test-check` hook runs the full suite pre-commit, so keep it green.
- New entity fields are **nullable in the DB** (legacy rows) but **required for new bookings** via bean-validation (`@NotNull`) exercised on the `@Valid` form path.
- ≥90% line coverage on new code; **100% branch coverage** on `AppointmentConflictDetector` (critical logic).

## Tasks

### [x] 1.0 Time- & Vet-Aware Appointment Data Model + Multi-DB Migration

Extend the `Visit` entity with a `startTime` (`LocalTime`), a `@ManyToOne` `Vet`
association, and a single fixed `APPOINTMENT_DURATION` (30-minute) constant. Update
the schema and seed data for all four DB variants so the new columns exist, are
**nullable** at the DB level, and existing sample visits are backfilled with
`start_time = 09:00` and `vet_id = NULL`. (Spec Unit 1 — data foundation.)

#### 1.0 Proof Artifact(s)

- Test: a `@DataJpaTest` in `ClinicServiceTests` saving and reloading a `Visit` with a non-null `startTime` and an associated `Vet` passes — demonstrates the new columns and relationship persist.
- Test: existing `UpcomingVisitsRepositoryTests` and `ClinicServiceTests` still pass against backfilled seed data — demonstrates legacy/no-regression.
- Diff: `db/h2/schema.sql`, `db/hsqldb/schema.sql`, `db/postgres/schema.sql`, `db/mysql/schema.sql` show `start_time` + `vet_id` columns and FK; `data.sql` files show `09:00` backfill — demonstrates migration across all profiles.
- CLI: `./mvnw test` passes — demonstrates suite-wide green after the model change.

#### 1.0 Tasks

- [x] 1.1 Create a feature branch (e.g., `feat/12-calendar-appointment-booking`) off `main`.
- [x] 1.2 **(RED)** Add a failing `@DataJpaTest` (in `ClinicServiceTests`) that builds a `Visit` with `startTime = 09:30` and an associated `Vet`, saves it via the owner aggregate, reloads, and asserts both fields round-trip.
- [x] 1.3 **(GREEN)** Add the `startTime` field to `Visit` (`@Column(name = "start_time")`, `@DateTimeFormat(pattern = "HH:mm")`, `LocalTime`) with getter/setter; leave it `null` by default (do not default in the constructor) so it is required on new bookings.
- [x] 1.4 **(GREEN)** Add `@ManyToOne @JoinColumn(name = "vet_id")` `Vet vet` to `Visit` with getter/setter (nullable for legacy rows). Add a `public static final Duration APPOINTMENT_DURATION = Duration.ofMinutes(30);` constant and a derived `getEndTime()` helper (`startTime.plus(APPOINTMENT_DURATION)`).
- [x] 1.5 Update `db/h2/schema.sql` and `db/hsqldb/schema.sql`: add `start_time TIME` and `vet_id INTEGER` to `visits`, add `FK fk_visits_vets (vet_id) REFERENCES vets(id)`, and an index on `vet_id`.
- [x] 1.6 Update `db/postgres/schema.sql` and `db/mysql/schema.sql` with the equivalent column/FK/index DDL for those dialects.
- [x] 1.7 Update every `data.sql` (`h2`, `hsqldb`, `postgres`, `mysql`): rewrite existing `visits` inserts to include `start_time = '09:00'` (and `vet_id` NULL), matching each dialect's insert syntax.
- [x] 1.8 **(REFACTOR/VERIFY)** Run `./mvnw test`; confirm new test passes and all existing visit/upcoming-visit tests remain green. Commit.

### [x] 2.0 Booking Form with Veterinarian Dropdown, Start-Time Input, and Required-Field Validation

Extend the booking flow so new appointments capture a **required** veterinarian
(dropdown from `VetRepository.findAll()`) and a **required** start time, while
preserving the existing `@FutureOrPresent` date rule. On missing/invalid vet or time,
re-render the form with localized field errors and preserved input. (Spec Unit 1 — UI.)

#### 2.0 Proof Artifact(s)

- Test: `VisitControllerTests` cases for "vet required" and "start time required" return the form with a field error and save nothing — demonstrates validation.
- Test: `VisitControllerTests` happy-path posts vet + date + time + description and redirects to `/owners/{ownerId}` — demonstrates successful booking.
- Test: `VetFormatterTests` (valid id → `Vet`; empty → `null`) passes — demonstrates binding.
- Test: existing past-date rejection test still passes — demonstrates no regression to date validation.
- Screenshot: booking form rendering the **Veterinarian** dropdown and **Time** input — demonstrates the UI exists.

#### 2.0 Tasks

- [x] 2.1 **(RED)** Add/adjust `VisitControllerTests`: (a) posting without `vetId` returns the form with a `vet` field error; (b) posting without `startTime` returns the form with a `startTime` field error; (c) happy-path with vet+date+time+description redirects. Update the existing success test to include the new required params.
- [x] 2.2 **(RED)** Add `VetFormatterTests`: parsing a valid vet id returns the matching `Vet`; parsing an empty string returns `null`; printing a `Vet` yields its id.
- [x] 2.3 **(GREEN)** Implement `VetFormatter implements Formatter<Vet>` using `VetRepository` (mirror `PetTypeFormatter`); handle empty input → `null`. Register it (e.g., in the existing `WebConfiguration`/`addFormatters`).
- [x] 2.4 **(GREEN)** Add `@NotNull` to `Visit.startTime` and `Visit.vet` (validated on the `@Valid` form path only; DB stays nullable for legacy rows). Wire the messages to keys `visit.startTime.required` / `visit.vet.required`.
- [x] 2.5 **(GREEN)** Add `@ModelAttribute("vets")` to `VisitController` returning `vets.findAll()` (inject `VetRepository`); ensure the existing `disallowedFields("id")` init-binder still applies.
- [x] 2.6 **(GREEN)** Update `pets/createOrUpdateVisitForm.html`: add a `selectField` Veterinarian dropdown (bound to `visit.vet`, options from `vets`, with an empty default option) and an `inputField` Time input (`type="time"`, bound to `visit.startTime`).
- [x] 2.7 **(GREEN)** Add English message keys: `veterinarian`, `appointmentTime`, `visit.startTime.required`, `visit.vet.required`.
- [x] 2.8 **(REFACTOR/VERIFY)** Run `./mvnw test`; start the app, capture a screenshot of the booking form with the new fields. Commit.

### [x] 3.0 Conflict Detection — No Double-Booking (Vet & Pet)

Add server-side overlap detection using the half-open rule
`startA < endB && startB < endA` (each `end = start + APPOINTMENT_DURATION`). Add
repository queries to find same-day appointments for a vet and for a pet, and reject
overlapping bookings with a localized error. Legacy null-vet rows must not produce
false vet-conflicts. (Spec Unit 2.)

#### 3.0 Proof Artifact(s)

- Test: `VisitControllerTests` rejects an overlapping **same-vet** booking (no save, form error) — demonstrates vet double-booking blocked.
- Test: `VisitControllerTests` rejects an overlapping **same-pet** booking — demonstrates pet double-booking blocked.
- Test: `AppointmentConflictDetectorTests` — back-to-back (09:30 after 09:00–09:30) **allowed**; partial overlap (09:15) **rejected**; equal start **rejected** — demonstrates the overlap rule (100% branch coverage).
- Test: `@DataJpaTest` for "find by vet+date" / "find by pet+date" returns expected rows — demonstrates query support.
- Screenshot: booking form showing the conflict error message — demonstrates user-facing behavior.

#### 3.0 Tasks

- [x] 3.1 **(RED)** Add `AppointmentConflictDetectorTests` covering the pure overlap rule: partial overlap → conflict; identical start → conflict; back-to-back (end == start) → no conflict; fully separate → no conflict.
- [x] 3.2 **(GREEN)** Implement `AppointmentConflictDetector` with a pure `overlaps(LocalTime startA, LocalTime startB)` method using `APPOINTMENT_DURATION` and the half-open `[start, end)` rule.
- [x] 3.3 **(RED)** Add a `@DataJpaTest` (in `ClinicServiceTests`) asserting `findByVetAndDate` and `findByPetAndDate` return the expected same-day appointments.
- [x] 3.4 **(GREEN)** Add JPQL queries to `VisitRepository` returning appointments (with `startTime`) for a given `vetId`+`date` and a given `petId`+`date`.
- [x] 3.5 **(RED)** Add `VisitControllerTests`: overlapping same-vet booking rejected with error + no save; overlapping same-pet booking rejected; a booking whose only same-time peer has a NULL vet is **not** flagged as a vet conflict.
- [x] 3.6 **(GREEN)** Inject `AppointmentConflictDetector` + repository queries into `VisitController.processNewVisitForm`; on conflict, `result.rejectValue("startTime", "visit.conflict", ...)` (or a global error) and re-render without saving. Apply the vet check only when a vet is assigned.
- [x] 3.7 **(GREEN)** Add English message key `visit.conflict` (e.g., "This time conflicts with an existing appointment.").
- [x] 3.8 **(REFACTOR/VERIFY)** Run `./mvnw test`; verify branch coverage on the detector; capture a screenshot of the conflict error. Commit.

### [x] 4.0 Clinic-Wide Day Schedule Page

Add a read-only `GET /schedule` route listing all appointments for a single date,
defaulting to today and accepting an optional `date=yyyy-MM-dd` (invalid/missing →
today). Project appointments clinic-wide ordered by start time then vet, and render a
template with empty state, prev/next-day navigation, and a nav-bar entry. (Spec Unit 3.)

#### 4.0 Proof Artifact(s)

- Test: `ScheduleControllerTests` — `GET /schedule` returns the schedule view with today's appointments by default — demonstrates the route.
- Test: `ScheduleControllerTests` — `?date=YYYY-MM-DD` returns that day's appointments ordered by start time; invalid/missing date falls back to today — demonstrates parameter handling.
- Test: `@DataJpaTest` for the clinic-wide "find by date" query returns rows across vets/pets ordered by time — demonstrates the query.
- Screenshot: schedule page with multiple appointments ordered by time + prev/next-day controls — demonstrates the day view.
- Screenshot: schedule page empty-state for a day with no appointments — demonstrates the empty state.

#### 4.0 Tasks

- [x] 4.1 **(GREEN)** Create the `ScheduledAppointment` DTO (ownerId, ownerFirstName, ownerLastName, petName, vetFirstName, vetLastName, date, startTime, description) with a constructor suitable for a JPQL constructor expression.
- [x] 4.2 **(RED)** Add a `@DataJpaTest` (in `ClinicServiceTests`) asserting the clinic-wide "find appointments by date" query returns rows for that date ordered by `startTime` then vet.
- [x] 4.3 **(GREEN)** Add the JPQL constructor-expression query `findScheduledAppointmentsByDate(LocalDate date)` to `VisitRepository` (root at `Owner`, traverse `pets`→`visits`, left-join `vet`).
- [x] 4.4 **(RED)** Add `ScheduleControllerTests`: default (no param) → today's view; `?date=` valid → that day; `?date=` invalid/blank → today fallback; empty result → empty-state model; ordering preserved.
- [x] 4.5 **(GREEN)** Implement `ScheduleController` with `GET /schedule`, resilient date parsing (mirror the Upcoming Visits param pattern), and model attributes for the day, prev/next dates, and the appointment list.
- [x] 4.6 **(GREEN)** Create `schedule/daySchedule.html` using the layout + Liatrio table styling: columns start time / pet / owner (linked) / vet / description, an empty-state message, and prev/next-day links + a date picker.
- [x] 4.7 **(GREEN)** Add a "Schedule" `menuItem` entry to `fragments/layout.html` nav, consistent with the Upcoming Visits entry.
- [x] 4.8 **(GREEN)** Add English message keys: `schedule`, `schedule.subtitle`, `schedule.none`, `schedule.prevDay`, `schedule.nextDay`, plus column headers as needed.
- [x] 4.9 **(REFACTOR/VERIFY)** Run `./mvnw test`; capture screenshots of the populated and empty schedule pages. Commit.

### [ ] 5.0 Internationalization Propagation & Documentation

Add all new message keys to the base `messages.properties` and propagate to the other
locale files following the repo convention; update docs if needed. (Cross-cutting;
supports the Spec i18n success metric.)

#### 5.0 Proof Artifact(s)

- Test: `I18nPropertiesSyncTest` passes (keys present/consistent across locale files) — demonstrates i18n completeness.
- Diff: `messages.properties` (+ locale variants) show the new keys — demonstrates localization.
- CLI: `./mvnw test` passes — demonstrates final suite-wide green.

#### 5.0 Tasks

- [ ] 5.1 **(RED)** Run `./mvnw test -Dtest=I18nPropertiesSyncTest` to surface any missing/inconsistent keys across locale files for the new strings.
- [ ] 5.2 **(GREEN)** Add the new keys (veterinarian, appointment time, required/conflict errors, schedule title/subtitle/empty-state, prev/next-day) to each locale file per the existing convention (translated or English-fallback consistent with how prior features handled it).
- [ ] 5.3 **(GREEN)** Update docs as needed (e.g., note the appointment/conflict feature where the visit feature is described) and ensure markdownlint passes.
- [ ] 5.4 **(VERIFY)** Run the full `./mvnw test` suite; confirm green. Final commit and open PR.
