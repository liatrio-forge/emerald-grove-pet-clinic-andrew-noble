# Task 01 Proofs - Pull Request Build & Test Gate + branch protection

## Task Summary

This task proves the repository now has a server-side build/test gate: a GitHub Actions workflow
(`ci.yml`) runs `./mvnw verify` on every pull request and push to `main`, uploads the JaCoCo
coverage report, and — via branch protection — is a **required** status check so a failing build
blocks merging to `main`.

## What This Task Proves

- The `CI` workflow runs `./mvnw verify` on a pull request and passes on clean code.
- The JaCoCo coverage report is published as a build artifact (`if: always()`).
- The same gate fails when a test fails, and once the check is required, the failing pull request is
  blocked from merging.
- Branch protection requiring the `Build & Test` check is configured reproducibly via a script.

## Evidence Summary

- Clean PR #22 CI run concluded `success` in ~2 minutes; a `jacoco` artifact (~394 KB) was produced.
- A deliberately-broken-test PR (#23) CI run concluded `failure`.
- After applying branch protection, the failing PR #23 merge state became `BLOCKED`, while the
  required check on `main` is `Build & Test` with `strict: true`.
- Local quality gates (`actionlint`, `shellcheck`, `markdownlint`, `check-yaml`) all pass.

> Note: this environment captures GitHub UI state as reproducible CLI/API output rather than browser
> screenshots. Each artifact below includes the run/PR URL so a reviewer can view the same UI.

## Artifact: Clean PR CI run passes

**What it proves:** The gate runs `./mvnw verify` on a pull request and succeeds on valid code.

**Why it matters:** This is the core happy-path proof that the build/test gate works end-to-end on
real CI infrastructure.

**Command:**

```bash
gh run view 29274734160 --json displayTitle,conclusion,status,url,headSha
```

**Result summary:** The `CI` run for PR #22 completed with `conclusion: success` on commit
`ce1d41a`. The `Build & Test` job (checkout → JDK 17 → `mvnw verify` → upload JaCoCo) finished in
2m3s.

```json
{
  "conclusion": "success",
  "status": "completed",
  "title": "ci: add CI/CD pipeline — PR build/test gate (Task 1.0, Spec 13)",
  "headSha": "ce1d41a8fd6b9b4cc35b06df70716d41c8d3cc59",
  "url": "https://github.com/liatrio-forge/emerald-grove-pet-clinic-andrew-noble/actions/runs/29274734160"
}
```

Run URL (for the passing-check screenshot):
<https://github.com/liatrio-forge/emerald-grove-pet-clinic-andrew-noble/actions/runs/29274734160>

## Artifact: JaCoCo coverage report published

**What it proves:** Coverage is uploaded as a build artifact on the run (`if: always()`).

**Why it matters:** Coverage visibility is a spec requirement and the basis for a future coverage
threshold.

**Command:**

```bash
gh api repos/liatrio-forge/emerald-grove-pet-clinic-andrew-noble/actions/runs/29274734160/artifacts \
  --jq '.artifacts[] | {name, size_in_bytes, expired}'
```

**Result summary:** A single artifact named `jacoco` (~393,775 bytes, not expired) was produced by
the passing run.

```json
{"name": "jacoco", "size_in_bytes": 393775, "expired": false}
```

## Artifact: Broken-test PR fails the gate

**What it proves:** The gate fails (non-zero) when a test fails.

**Why it matters:** A gate that cannot fail is not a gate. This is the negative-path proof.

**Command:**

```bash
gh run view 29274936888 --json displayTitle,conclusion,status,url
```

**Result summary:** A throwaway PR (#23) with one deliberately failing test produced a `CI` run with
`conclusion: failure` (the `mvnw verify` step exited 1). The branch/PR were deleted after capture.

```json
{
  "conclusion": "failure",
  "status": "completed",
  "title": "test: DEMO — CI gate must block a failing test (Spec 13, T1.5)",
  "url": "https://github.com/liatrio-forge/emerald-grove-pet-clinic-andrew-noble/actions/runs/29274936888"
}
```

## Artifact: Branch protection makes the failing PR un-mergeable

**What it proves:** With the `Build & Test` check required on `main`, a PR whose check fails is
blocked from merging.

**Why it matters:** This is the actual merge gate — it turns an advisory check into an enforced one.

**Commands:**

```bash
./scripts/setup-branch-protection.sh
gh pr view 23 --json mergeStateStatus --jq '.mergeStateStatus'
gh api repos/liatrio-forge/emerald-grove-pet-clinic-andrew-noble/branches/main/protection \
  --jq '{strict: .required_status_checks.strict, required: .required_status_checks.contexts}'
```

**Result summary:** After running the script, the failing PR #23 reported `mergeStateStatus:
BLOCKED`, and `main` requires the `Build & Test` check with `strict: true`.

```json
{ "mergeStateStatus": "BLOCKED" }
{ "strict": true, "required": ["Build & Test"] }
```

## Artifact: Local quality gates pass

**What it proves:** The new workflow, script, and docs satisfy the repo's pre-commit gates.

**Why it matters:** The repo enforces YAML/shell/markdown quality; the deliverables must comply.

**Command:**

```bash
actionlint .github/workflows/ci.yml
shellcheck scripts/setup-branch-protection.sh
pre-commit run --files .github/workflows/ci.yml docs/CICD.md README.md \
  scripts/setup-branch-protection.sh
```

**Result summary:** `actionlint` and `shellcheck` reported no issues; all pre-commit hooks
(`check-yaml`, `markdownlint`, `shellcheck`, whitespace/EOF) passed.

## Known non-blocking warning

The runner emits a GitHub-side deprecation annotation: `actions/checkout@v4`,
`actions/setup-java@v4`, and `actions/upload-artifact@v4` target Node 20 (forced to Node 24). These
are the current major versions of those actions; the warning does not fail the run and can be
revisited when the actions publish Node 24 major releases.

## Reviewer Conclusion

The CI gate works end-to-end: it passes on clean code, publishes coverage, fails on a broken test,
and — with branch protection requiring the `Build & Test` check — blocks a failing pull request from
merging into `main`. Task 1.0 is complete.
