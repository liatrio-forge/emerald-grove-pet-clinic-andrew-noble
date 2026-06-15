# Task 01 Proofs - Enforce "today or later" on `Visit.date` with a localized message

## Task Summary

This task proves that the `Visit` entity now rejects past dates at the Bean
Validation layer and that the rejection produces a clear, internationalized
message. A declarative `@FutureOrPresent` constraint was added to `Visit.date`,
backed by a new `visit.date.future` message key present in the base bundle and
all 7 translated locale bundles.

## What This Task Proves

- A `Visit` dated before today produces exactly one constraint violation on the
  `date` property (the past-date rule works in isolation).
- A `Visit` dated today or in the future produces zero violations (the
  today-inclusive boundary is correct).
- The violation message resolves to "must be today or a future date" via the
  application message bundle (clear, localized text).
- The new `visit.date.future` key exists across all required bundles, keeping
  locale-key parity intact (`I18nPropertiesSyncTest` passes).

## Evidence Summary

- `ValidatorTests` (4 tests) and `I18nPropertiesSyncTest` (2 tests) both pass.
- `Visit.java` carries `@FutureOrPresent(message = "{visit.date.future}")` on the
  `date` field.
- The `visit.date.future` key is present in `messages.properties` plus the de,
  es, fa, ko, pt, ru, and tr bundles.

## Artifact: Constraint and i18n unit tests pass

**What it proves:** The past-date rule, the today/future boundary, and the
localized message all behave correctly, and locale-key parity is maintained.

**Why it matters:** This is the core RED→GREEN evidence that the validation rule
exists and resolves the right message before any controller or UI wiring.

**Command:**

```bash
./mvnw test -Dtest=ValidatorTests,I18nPropertiesSyncTest
```

**Result summary:** 6 tests run, 0 failures, 0 errors — BUILD SUCCESS.

```text
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in ...system.I18nPropertiesSyncTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in ...model.ValidatorTests
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Earlier RED evidence (before the annotation existed), confirming the test fails
for the right reason:

```text
[ERROR] ValidatorTests.shouldNotValidateWhenVisitDateInPast
Expected size: 1 but was: 0 in: []
```

## Artifact: `@FutureOrPresent` constraint on the entity

**What it proves:** The rule is declared on the domain entity, so the existing
`@Valid` path in `VisitController` enforces it with no controller change.

**Why it matters:** Keeping the rule on the entity matches the existing
`@NotBlank` pattern and is the smallest, most idiomatic implementation.

**Artifact path:** `src/main/java/org/springframework/samples/petclinic/owner/Visit.java`

**Result summary:** The `date` field is annotated with
`@FutureOrPresent(message = "{visit.date.future}")` alongside the existing
`@DateTimeFormat`.

```java
@Column(name = "visit_date")
@DateTimeFormat(pattern = "yyyy-MM-dd")
@FutureOrPresent(message = "{visit.date.future}")
private LocalDate date;
```

## Artifact: `visit.date.future` key across message bundles

**What it proves:** The message is internationalized and parity is preserved
across all bundles enforced by `I18nPropertiesSyncTest`.

**Why it matters:** A missing key in any enforced bundle would fail the sync test
and leave a partial translation.

**Command:**

```bash
grep -rn "visit.date.future" src/main/resources/messages/
```

**Result summary:** The key is present in the base bundle and all 7 translated
bundles (de, es, fa, ko, pt, ru, tr).

```text
messages.properties:visit.date.future=must be today or a future date
messages_de.properties:visit.date.future=muss heute oder ein zukünftiges Datum sein
messages_es.properties:visit.date.future=debe ser hoy o una fecha futura
messages_fa.properties:visit.date.future=باید امروز یا تاریخی در آینده باشد
messages_ko.properties:visit.date.future=오늘 또는 미래 날짜여야 합니다
messages_pt.properties:visit.date.future=deve ser hoje ou uma data futura
messages_ru.properties:visit.date.future=должно быть сегодня или будущей датой
messages_tr.properties:visit.date.future=bugün veya gelecekteki bir tarih olmalıdır
```

### Note on `messages_en.properties` (justified deviation from task wording)

The task text mentioned adding the key to `messages_en.properties`, but that file
is intentionally empty — `I18nPropertiesSyncTest` explicitly skips it and English
falls back to the base `messages.properties` (which holds the English text). To
respect that established convention and keep the sync test green, the English
text lives in the base bundle and `messages_en.properties` was left unchanged.

### Note on test message resolution

`ValidatorTests` builds a `LocalValidatorFactoryBean` backed by a
`ResourceBundleMessageSource` (`createValidatorWithMessages()`) so the custom
`{visit.date.future}` key resolves exactly as it does at runtime, where Spring
wires the application `MessageSource` into the validator.

## Reviewer Conclusion

The past-date rule is enforced declaratively on `Visit.date`, the today/future
boundary is correct, and the rejection surfaces a clear, fully-internationalized
message — all verified by passing `ValidatorTests` and `I18nPropertiesSyncTest`.
