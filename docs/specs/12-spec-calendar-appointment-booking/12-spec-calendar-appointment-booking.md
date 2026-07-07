# 12-spec-calendar-appointment-booking.md

## Introduction/Overview

The clinic currently records a "visit" as a **date only** (`LocalDate`) tied to a pet, with no time of day and no assigned veterinarian. This makes it impossible to see a day's schedule or to prevent double-booking. This feature turns visits into **time-aware appointments** with an **assigned veterinarian**, adds **server-side conflict detection** so the same vet (or the same pet) cannot be booked into overlapping time slots, and provides a **read-only clinic-wide day schedule** view so staff can see all appointments for a given day at a glance.

The primary goal is a complete, demoable booking-with-conflict-detection slice that fits the existing server-rendered Spring Boot + Thymeleaf architecture without introducing a JavaScript calendar library.

## Goals

1. Allow staff to book an appointment with a **specific start time** and an **assigned veterinarian** (fixed 30-minute duration).
2. **Prevent double-booking**: reject a new appointment when it overlaps an existing appointment for the **same vet** or the **same pet** on the same day.
3. Provide a **read-only, clinic-wide day schedule** page showing all appointments for a chosen date, grouped/ordered so overlaps and gaps are obvious.
4. Preserve all existing visit behavior (Previous Visits, Upcoming Visits, "no past dates" validation) and existing seed/sample data with **zero regressions**.
5. Maintain project quality bars: strict TDD, ≥90% line coverage on new code, i18n for all new user-facing strings.

## User Stories

- **As a clinic receptionist**, I want to choose a veterinarian and a start time when booking an appointment so that the appointment is assigned to a real provider at a real time, not just a date.
- **As a clinic receptionist**, I want the system to stop me from booking a vet (or a pet) into a slot that is already taken so that I don't create scheduling conflicts.
- **As a veterinarian or clinic manager**, I want to view all appointments for a given day on one page so that I can understand the day's workload and spot conflicts or open slots.
- **As a clinic manager**, I want existing historical visits to keep working so that past records and the Upcoming Visits page are not broken by the change.

## Demoable Units of Work

### Unit 1: Time- and Vet-Aware Appointments

**Purpose:** Extend the appointment model and booking form so an appointment captures a start time and an assigned veterinarian, while keeping existing visits and features intact. This is the schema/data foundation for conflict detection and the schedule view.

**Functional Requirements:**

- The system shall add a `startTime` (`LocalTime`) field to the appointment (`Visit`) entity.
- The system shall add a required **veterinarian** association (`@ManyToOne` to `Vet`) to the appointment for **new** bookings.
- The system shall treat every appointment as a **fixed 30-minute** duration; the effective end time is `startTime + 30 minutes` (duration is a single named constant, not user input).
- The booking form (`/owners/{ownerId}/pets/{petId}/visits/new`) shall display a **start-time input** and a **vet dropdown** populated from existing veterinarians (`VetRepository.findAll()`).
- The system shall reject a new booking with a clear, field-level validation error when the start time is missing or the vet is not selected, re-rendering the form with the user's input preserved.
- The system shall retain existing validation that the appointment **date** is today or in the future (`@FutureOrPresent`).
- The system shall keep **existing/legacy visits** (which have no vet and no time) readable and listable: legacy rows are backfilled with a default start time of **09:00** and a **null** vet, and `vet`/`startTime` remain nullable at the database level to accommodate them.
- The system shall continue to display Previous Visits and the Upcoming Visits page without regression.

**Proof Artifacts:**

- Test: `VisitControllerTests` passes including new cases for "vet required" and "start time required" — demonstrates new fields are captured and validated.
- Test: `ClinicServiceTests` / repository test confirming an appointment persists with `startTime` and an associated `Vet` — demonstrates schema + persistence work.
- Screenshot: the booking form showing the new **Time** input and **Veterinarian** dropdown — demonstrates the UI exists.
- Test: existing `UpcomingVisits*` and visit tests still pass — demonstrates no regression on legacy data.

### Unit 2: Conflict Detection (No Double-Booking)

**Purpose:** Enforce that a new appointment cannot overlap an existing appointment for the same veterinarian or the same pet, returning a clear error instead of silently creating a conflict.

**Functional Requirements:**

- The system shall define two appointments as **overlapping** when they share the same date and their time ranges intersect, using the rule `startA < endB AND startB < endA` (with each `end = start + 30 minutes`).
- The system shall reject a new booking when it overlaps an existing appointment for the **same veterinarian** on that date.
- The system shall reject a new booking when it overlaps an existing appointment for the **same pet** on that date.
- On a detected conflict, the system shall **re-render the booking form** with a clear, localized validation error message (e.g., a global/`startTime` error) and preserve the user's input; no appointment is saved.
- The system shall provide repository query support to find existing appointments for a given vet on a given date and for a given pet on a given date, to drive conflict checks efficiently (no full-table scans).
- The system shall perform conflict detection on the server (it shall not rely on client-side checks).
- Conflict detection shall apply only when a vet is assigned; legacy null-vet rows shall not produce false vet-conflicts (pet-conflict still applies via the pet association).

**Proof Artifacts:**

- Test: `VisitControllerTests` case posting an overlapping vet appointment returns the form with a validation error and saves nothing — demonstrates vet double-booking is blocked.
- Test: `VisitControllerTests` case posting an overlapping pet appointment is rejected — demonstrates pet double-booking is blocked.
- Test: boundary cases pass — an appointment starting exactly when another ends (e.g., 09:30 after a 09:00–09:30) is **allowed**; a partial overlap (09:15) is **rejected** — demonstrates the overlap rule is correct.
- Test: repository test for "find appointments by vet and date" / "by pet and date" returns expected rows — demonstrates query support.
- Screenshot: booking form displaying the conflict error message — demonstrates the user-facing behavior.

### Unit 3: Clinic-Wide Day Schedule View

**Purpose:** Give staff a single read-only page that shows all appointments for a chosen day, so the day's bookings (and conflicts/openings) are visible at a glance.

**Functional Requirements:**

- The system shall expose a new read-only route (e.g., `GET /schedule`) that displays all appointments for a single **date**.
- The system shall accept an optional `date` query parameter (`yyyy-MM-dd`); when absent or invalid, it shall default to **today** (mirroring the resilient-parameter pattern used by the Upcoming Visits page).
- The page shall list, for each appointment on that date: **start time**, **pet name**, **owner** (linked to the owner detail page), assigned **veterinarian**, and **description**.
- The page shall order appointments by **start time**, then veterinarian, so the day reads top-to-bottom chronologically.
- The page shall show a clear **empty state** message when there are no appointments on the selected date.
- The page shall provide simple navigation to the **previous day** and **next day** (and/or a date picker) so a user can change the displayed date.
- The page shall be reachable from the main navigation bar and shall use the existing Thymeleaf layout and Liatrio table styling.

**Proof Artifacts:**

- Test: `ScheduleController` web-layer test confirms `GET /schedule` returns the schedule view with appointments for today by default — demonstrates the route works.
- Test: `GET /schedule?date=YYYY-MM-DD` returns appointments for that date, ordered by start time; invalid/missing date falls back to today — demonstrates parameter handling.
- Test: repository test for "find appointments by date (clinic-wide)" returns rows across all vets/pets ordered by time — demonstrates the query.
- Screenshot: the day schedule page showing multiple appointments ordered by time, including the prev/next-day navigation — demonstrates the calendar/day view.
- Screenshot: the empty-state schedule page for a day with no appointments — demonstrates the empty state.

## Non-Goals (Out of Scope)

1. **Drag-and-drop / interactive JS calendar**: no FullCalendar or similar library; the schedule view is server-rendered and read-only.
2. **Rescheduling or editing an existing appointment's time/vet** after creation (including drag-to-move).
3. **Appointment cancellation or status lifecycle** (booked/completed/cancelled/no-show).
4. **Recurring/repeating appointments.**
5. **Reminders/notifications** (email, SMS, or in-app).
6. **Clinic business-hours validation** (rejecting bookings outside open hours) and **exam-room / resource capacity** modeling.
7. **"Next available slot" suggestion** on conflict (the system rejects with an error; it does not propose alternatives).
8. **Selectable / variable appointment durations** (duration is a fixed 30 minutes for this slice).
9. **Backfilling a real vet onto legacy visits** (legacy rows keep a null vet; only new bookings require one).

## Design Considerations

- Reuse the existing Thymeleaf fragment-based approach: `fragments/layout.html`, `fragments/inputField.html`, and `fragments/selectField.html` (for the vet dropdown), and the Liatrio table styling (`.liatrio-section`, `.liatrio-table-card`, `.liatrio-table`, `.liatrio-muted`) already used by the Upcoming Visits page.
- The booking form (`pets/createOrUpdateVisitForm.html`) gains a **Time** input (HTML5 `type="time"`) and a **Veterinarian** `<select>`; layout should match the existing Date/Description fields.
- The day schedule (`schedule/daySchedule.html` or similar) should resemble the Upcoming Visits list: a titled section with a table, an empty state, and prev/next-day controls. A simple chronological table is acceptable for this slice (a visual time-grid is a nice-to-have, not required).
- Add the new "Schedule" entry to the navigation bar consistent with the existing "Upcoming Visits" entry.
- All new user-facing strings must be added to `messages.properties` (and ideally the other locale files) following the existing i18n pattern.

## Repository Standards

- **Strict TDD (Red-Green-Refactor)** as mandated by `CLAUDE.md`: write failing tests first for each requirement, then minimal implementation, then refactor.
- **Layered architecture**: web controller → repository (Spring Data JPA). Conflict detection logic belongs in the controller/service layer (it requires DB access and cannot be a pure bean-validation annotation); follow the existing pattern of adding errors to `BindingResult` and re-rendering the form (as done for `@FutureOrPresent`).
- **Repository queries**: use JPQL with date/time parameters, following the patterns established by `VisitRepository` and the Upcoming Visits feature; avoid loading all visits into memory for conflict checks.
- **Testing patterns**: `@WebMvcTest` + `MockMvc` + `@MockitoBean` for controllers; `@DataJpaTest` for repository queries; AssertJ + Hamcrest assertions; Arrange-Act-Assert; descriptive test names.
- **Coverage**: ≥90% line coverage for new code; 100% branch coverage for the overlap/conflict logic (it is critical business logic).
- **i18n**: no hard-coded UI strings; add keys to `messages/messages.properties`.
- **Commits**: conventional commits; PR-based workflow (no direct commits to `main`); pre-commit hooks (including `maven-test-check`) must pass.

## Technical Considerations

- **Entity changes (`Visit`)**: add `private LocalTime startTime;` and `@ManyToOne private Vet vet;`. Keep both **nullable** at the DB level so existing seed rows and historical data remain valid; enforce "required" only via form/controller validation for new bookings.
- **Duration**: represent as a single constant (e.g., `APPOINTMENT_DURATION = Duration.ofMinutes(30)`); compute `endTime = startTime.plus(duration)` for overlap math. Centralize so a future spec can make it configurable.
- **Overlap rule**: half-open intervals `[start, end)` so back-to-back appointments (e.g., 09:00–09:30 and 09:30–10:00) do **not** conflict; condition `startA < endB && startB < endA`.
- **Seed/migration**: the H2 sample data (`data.sql`) and any schema (`schema.sql`) must be updated so existing visits get `startTime = 09:00` and `vet = NULL`; verify MySQL/PostgreSQL DDL paths (`@OneToMany`/`@ManyToOne` mapping) generate correctly. Confirm `Vet` is in the `owner` package's reach or import across packages as needed.
- **`Vet` association across packages**: `Vet` lives in the `vet` package and `Visit` in `owner`; ensure the JPA relationship and any controller injection (`VetRepository`) are wired across packages without creating a circular dependency.
- **Schedule query**: a clinic-wide "find appointments by date" query likely needs a flattened projection (owner + pet + vet + time), analogous to the existing `UpcomingVisit` DTO/constructor-expression approach.
- **Caching**: `VetRepository.findAll()` is cached; reusing it for the vet dropdown is appropriate.
- **No new runtime dependencies**: implementable with current Spring Boot, Thymeleaf, Bootstrap 5, and HTML5 `type="time"`/`type="date"` inputs.

## Security Considerations

- **No new sensitive data**: appointments contain vet/pet/owner names and times already present in the app; no credentials, tokens, or PII beyond existing scope.
- **Input validation**: validate `date`, `startTime`, and `vetId` server-side; treat the schedule page's `date` query parameter defensively (default to today on invalid input) to avoid errors from malformed input.
- **Injection safety**: use parameterized JPQL/Spring Data queries (no string-concatenated SQL) for the new conflict and schedule queries.
- **Proof artifacts**: screenshots contain only sample clinic data — safe to attach; no secrets should appear. Do not commit any local DB dumps or environment files.

## Success Metrics

1. **Conflict prevention**: 100% of attempts to book an overlapping same-vet or same-pet slot are rejected with a clear error and result in **no** saved appointment (verified by tests).
2. **No regressions**: all pre-existing tests (visit, upcoming visits, owner, vet) continue to pass; existing seed data renders on Previous Visits and Upcoming Visits pages.
3. **Schedule visibility**: `GET /schedule` renders all appointments for a chosen day, ordered by start time, with working prev/next-day navigation and an empty state (verified by tests + screenshots).
4. **Coverage**: ≥90% line coverage on new code; 100% branch coverage on the overlap/conflict logic.
5. **i18n**: all new user-facing strings resolved via message keys (no hard-coded literals in templates).

## Open Questions

1. **Schedule date range granularity**: this spec implements a single-**day** view with prev/next navigation. Is a same-spec **week** view desired, or is that a follow-up? (Default assumption: day view only.)
2. **Time-grid vs. list rendering**: a chronological table satisfies the requirements; do you want a true time-slotted grid (rows per 30-min slot) in this spec, or is that a visual enhancement for later? (Default assumption: chronological table.)
3. **Locale coverage for new strings**: add new message keys to **all** 9 locale files now, or English-only with others as a follow-up (consistent with how prior features were handled)? (Default assumption: English now, mirror others if the existing convention requires it.)
