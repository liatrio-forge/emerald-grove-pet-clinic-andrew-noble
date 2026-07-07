# 12 Questions Round 1 - Calendar-Based Appointment Booking with Conflict Detection

Please answer each question below (check one or more options, or add your own notes under any question). When you're done, save the file and tell me you're ready.

> **Context:** Today a `Visit` is **date-only** (`LocalDate`), has **no time**, **no duration**, and **no assigned vet**. Conflict detection requires adding at least some of these. These questions decide the shape of the first spec.

---

## 1. What counts as a "conflict"? (the core of "conflict detection")

Which booking collisions should the system detect and prevent?

- [x] (A) **Vet double-booking** — the same vet cannot have two overlapping appointments
- [x] (B) **Pet double-booking** — the same pet cannot have two overlapping appointments
- [ ] (C) **Both vet and pet** — detect either kind of overlap
- [ ] (D) **Clinic capacity** — limit total simultaneous appointments (e.g., N exam rooms) regardless of vet/pet
- [ ] (E) Other (describe)

**Recommended answer(s):** (A), and optionally (B)

**Why these are recommended:**

- `(A)` is the canonical, demoable meaning of "conflict detection" for a clinic and forces us to model a **vet ↔ appointment** relationship, which is the highest-value missing piece.
- Adding `(B)` is cheap once we have time slots and gives an obvious second test case, but it's optional for the first slice.
- `(D)` (room/capacity modeling) is a meaningfully bigger domain change (rooms as a new entity) — better deferred to a follow-up spec unless you specifically need it now.

---

## 2. Time granularity — do appointments get a time of day and duration?

Conflict detection needs more than a date. How precise should appointments be?

- [x] (A) **Start time + fixed duration** — appointment has a `LocalTime` start; all appointments are a standard length (e.g., 30 min)
- [ ] (B) **Start time + selectable duration** — user picks start time and a duration (15/30/60 min)
- [ ] (C) **Fixed time slots** — clinic day divided into discrete slots (e.g., 9:00, 9:30, 10:00…), pick one
- [ ] (D) **Date only (keep as-is)** — conflict = same vet same *day* (no time)
- [ ] (E) Other (describe)

**Current best-practice context:** Modeling a start instant plus a duration (or fixed slot length) is the standard way to make overlap detection unambiguous and timezone-safe.

**Recommended answer(s):** (A)

**Why these are recommended:**

- `(A)` is the smallest change that makes "overlap" well-defined (start + fixed length → computable end), keeping the first spec focused and testable.
- `(C)` is also clean and pairs nicely with a calendar grid, but it requires defining clinic hours/slot config up front — a bit more scope.
- `(B)` is the most flexible but adds a duration input + more conflict math; good as a follow-up.
- `(D)` keeps the schema but makes "appointment booking" feel coarse and weakens the calendar UI value.

---

## 3. Calendar UI — what does the user actually see and interact with?

What should the "calendar-based" interface look like in this first slice?

- [x] (A) **Read-only calendar/day view** — a visual day or week grid showing existing appointments; booking still happens via the existing form (now extended with time + vet)
- [ ] (B) **Interactive calendar** — click an empty slot on the grid to open/prefill the booking form
- [ ] (C) **Full drag-and-drop calendar** — using a JS library (e.g., FullCalendar) with drag-to-create and drag-to-reschedule
- [ ] (D) Other (describe)

**Current best-practice context:** This app is server-rendered (Thymeleaf + Bootstrap 5), with **no** JS calendar library currently bundled. Option (C) would introduce a significant front-end dependency and AJAX layer.

**Recommended answer(s):** (A), then (B) as a stretch

**Why these are recommended:**

- `(A)` fits the existing server-side Thymeleaf pattern, needs no new JS library, and is fully demoable (screenshot of a day grid + a rejected double-booking).
- `(B)` is a natural, modest enhancement (a link/anchor per empty slot) and can be included if you want it.
- `(C)` is powerful but is effectively its own spec (library integration, AJAX endpoints, reschedule semantics) — recommend deferring.

---

## 4. Where do vets come from, and is assignment required?

The `Vet` entity exists but is **not** linked to visits today.

- [x] (A) **Vet is required** on every appointment, chosen from a dropdown of existing vets
- [ ] (B) **Vet is optional** — appointments can be unassigned; conflict detection only applies when a vet is set
- [ ] (C) **No vet** — detect conflicts some other way (answer Q1 accordingly)
- [ ] (D) Other (describe)

**Recommended answer(s):** (A)

**Why these are recommended:**

- `(A)` makes vet-based conflict detection (Q1-A) meaningful and unambiguous, and gives a clean dropdown UI backed by the existing `VetRepository.findAll()` (already cached).
- `(B)` is reasonable for backward compatibility with existing sample visits, but complicates conflict rules ("only when set") and validation messaging.
- Existing seed visits have no vet, so we'll need a migration/backfill stance regardless — see Q6.

---

## 5. What happens on conflict, and which view is the calendar attached to?

Two parts: (a) the failure behavior, (b) the entry point/scope of the calendar.

**(a) On a detected conflict, the system should:**

- [x] (A) Reject the booking and re-render the form with a clear validation error
- [ ] (B) Reject and suggest the next available slot/time
- [ ] (C) Allow but warn (soft conflict)
- [ ] (D) Other (describe)

**(b) The calendar/booking view is scoped to:**

- [x] (A) **Clinic-wide** day/week view (all vets) at a new route like `/schedule` or `/calendar`
- [ ] (B) **Per-vet** schedule (pick a vet, see their day)
- [ ] (C) **Per-pet/owner** (extend the existing owner→pet→visit flow only)
- [ ] (D) Other (describe)

**Recommended answer(s):** (a) → (A); (b) → (A) (clinic-wide), with per-vet filter as a stretch

**Why these are recommended:**

- (a)`(A)` mirrors the existing validation pattern (`@FutureOrPresent` → form re-render with field error), so it's consistent and easy to test. `(B)` (suggest next slot) is a nice follow-up but adds search logic.
- (b)`(A)` gives the most compelling "calendar" demo (see the whole clinic day and a blocked double-booking) and a natural home for conflict detection across vets.

---

## 6. Existing seed/sample visits & migration stance

The H2 sample data and existing tests assume date-only visits with no vet/time. How should we treat them?

- [x] (A) **Backfill with defaults** — give existing visits a default time (e.g., 09:00) and leave vet null/optional for legacy rows, but require vet+time on *new* bookings
- [ ] (B) **Keep legacy visits as-is** and only enforce new fields going forward (implies vet/time optional at the DB level)
- [ ] (C) **Reset/replace sample data** to include vet + time so everything is consistent
- [ ] (D) Other (describe)

**Recommended answer(s):** (A)

**Why these are recommended:**

- `(A)` preserves the existing "Previous Visits" / "Upcoming Visits" features and their tests while still requiring complete data on new bookings — least disruption, fully TDD-able.
- `(C)` is cleanest long-term but will churn multiple existing tests and seed scripts (more blast radius).
- `(B)` risks a permanently nullable schema that weakens conflict guarantees.

---

## 7. Scope confirmation (deferrals)

To keep this spec "just right," I propose **deferring** the following to later specs. Check anything you actually want pulled **into** this first spec instead:

- [ ] Recurring/repeating appointments
- [ ] Appointment reschedule (drag-and-drop or edit time/vet after creation)
- [ ] Appointment cancellation/status (booked/completed/cancelled)
- [ ] Email/SMS reminders or notifications
- [ ] Clinic hours / business-hours validation (reject bookings outside open hours)
- [ ] Exam-room / resource capacity modeling
- [ ] "Next available slot" suggestion

**Recommended:** leave all unchecked (defer all) for the first spec; pull in **Clinic hours validation** only if you want bookings constrained to open hours from day one.

**Why:** Each of these is independently demoable and would inflate the first spec past the "just right" size. Deferring keeps the Red-Green-Refactor cycle tight and the spec reviewable.
