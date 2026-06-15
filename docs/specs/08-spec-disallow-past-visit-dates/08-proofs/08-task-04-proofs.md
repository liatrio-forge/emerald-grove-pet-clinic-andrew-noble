# Task 04 Proofs - Final verification, coverage, and conventional commits

## Task Summary

This task proves the feature is complete and healthy: the entire Java test suite
passes, the changed production code (`Visit`) is fully covered by JaCoCo, and the
work lives on a dedicated feature branch with a clean, conventional commit
history.

## What This Task Proves

- The full `./mvnw test` suite passes (84 tests; 5 DB-container tests skipped
  because Docker is absent locally).
- The changed `Visit` class has 100% line coverage, exceeding the >90% standard
  for new code.
- All work is committed on `feat/disallow-past-visit-dates` (not `main`) using
  conventional commit messages.

## Evidence Summary

- Full suite: 84 run, 0 failures, 0 errors, 5 skipped — BUILD SUCCESS.
- JaCoCo: `Visit` = 9/9 lines, 20/20 instructions, 5/5 methods covered.
- Four conventional commits on the feature branch (docs, feat, test, test-e2e).

## Artifact: Full Java test suite passes

**What it proves:** Nothing in the existing suite regressed and all new tests
pass together.

**Why it matters:** This is the same gate the pre-commit `maven-test-check` hook
enforces; a green full run is the project's definition of commit-ready.

**Command:**

```bash
./mvnw clean test jacoco:report
```

**Result summary:** 84 tests run, 0 failures, 0 errors, 5 skipped. The 5 skips are
the MySQL/Postgres Testcontainers tests (no Docker locally); the CrashController
ERROR log lines are an intentional, asserted exception.

```text
[WARNING] Tests run: 84, Failures: 0, Errors: 0, Skipped: 5
[INFO] BUILD SUCCESS
```

## Artifact: JaCoCo coverage for changed production code

**What it proves:** The modified `Visit` entity is fully covered by tests.

**Why it matters:** AGENTS.md requires >90% line coverage for new code; this
confirms the standard is met for the only changed production class.

**Artifact path:** `target/site/jacoco/jacoco.csv`

**Result summary:** `Visit` shows 0 instructions missed / 20 covered, 0 lines
missed / 9 covered, 0 methods missed / 5 covered → 100% line coverage.

```text
GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,BRANCH_MISSED,BRANCH_COVERED,LINE_MISSED,LINE_COVERED,COMPLEXITY_MISSED,COMPLEXITY_COVERED,METHOD_MISSED,METHOD_COVERED
petclinic,...owner,Visit,0,20,0,0,0,9,0,5,0,5
```

(Message bundle `.properties` files are resources, not bytecode, so they are not
measured by JaCoCo; their resolution is verified by `ValidatorTests`.)

## Artifact: Feature branch and conventional commit history

**What it proves:** Work is isolated on a feature branch with descriptive,
conventional commits, satisfying the `no-direct-commits-to-main` policy.

**Why it matters:** The repository enforces a PR-based workflow; direct commits to
`main` are blocked by a pre-commit hook.

**Command:**

```bash
git branch --show-current && git log --oneline -4
```

**Result summary:** Branch `feat/disallow-past-visit-dates` holds four
conventional commits spanning planning, implementation, and tests.

```text
feat/disallow-past-visit-dates
e5f2e9c test(e2e): reject past visit dates and fix hard-coded date regression
dc8d5fe test: verify visit controller rejects past dates and preserves input
1c25946 feat: reject past visit dates at the validation layer
276f663 docs: add spec, questions, and planning audit for disallow-past-visit-dates
```

## Reviewer Conclusion

The feature is complete and verified: the whole suite is green, the changed
production code is fully covered, and the history is clean and conventional on an
isolated feature branch — ready for spec validation and PR.
