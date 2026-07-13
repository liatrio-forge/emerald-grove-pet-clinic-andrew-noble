# 13 Questions Round 1 - CI/CD Pipeline

Please answer each question below (select one or more options, or add your own notes).
Feel free to add additional context under any question. When you're done, save the file
and tell me — I'll read your answers and continue.

---

## 1. Deployment target (the big one)

"Deploy to Kubernetes on merge to main" needs an actual cluster to deploy to. The repo has
manifests in `/k8s/` but defines no cluster, kubeconfig, or cloud account. Which target should
the CD job deploy to?

- [ ] (A) **Ephemeral in-CI cluster** — spin up a throwaway `kind` (Kubernetes-in-Docker)
      cluster inside the GitHub Actions runner, load the freshly built image, `kubectl apply`
      the `/k8s/` manifests, and verify the pod becomes Ready. Self-contained, no external
      infra or long-lived secrets, fully provable from CI logs/screenshots. Tears down after.
- [ ] (B) **Real external cluster** — deploy to a persistent cluster you already have (e.g.
      Azure AKS — the devcontainer ships the Azure CLI). Requires storing a `KUBECONFIG` (or
      cloud OIDC federation) as a GitHub secret and a running cluster.
- [ ] (C) **Validate/dry-run only** — no live deploy; CI renders the manifests against the new
      image tag and runs `kubectl apply --dry-run=server` / `kubeconform` to prove the
      manifests are valid and correctly reference the published image.
- [ ] (D) Other (describe)

**Current best-practice context:** For CI/CD *demonstrations* and training deliverables, an
ephemeral `kind` cluster is the standard way to prove an end-to-end build→deploy→verify loop
without provisioning cloud infrastructure or managing production credentials. It produces clean,
reproducible evidence (pod Ready, rollout status) directly in the run logs. A real cluster is
more "production-realistic" but adds infrastructure and secret-management burden that may exceed
this deliverable's intent.

**Recommended answer(s):** [(A)]

**Why these are recommended:**

- `(A)` delivers a genuinely *working* deploy (real `kubectl apply`, real rollout, real
  readiness check) that satisfies the module's "automatically builds, tests, and deploys" exit
  criterion — while staying fully self-contained and reproducible for the demo recording.
- `(A)` needs no cloud account and no long-lived credentials, which keeps the security surface
  minimal and avoids blocking the work on infra you may not have provisioned.
- `(B)` is the most production-realistic but couples the deliverable to external infra and a
  stored `KUBECONFIG`/OIDC setup; pick this only if you already have a cluster you want to use.
- `(C)` is the lowest-risk but arguably doesn't "deploy" — it may not fully satisfy the exit
  criterion on its own. It's a good *fallback* if a live cluster (even ephemeral) proves too
  slow in CI.

---

## 2. Workflow file organization

How should the new pipeline relate to the existing `e2e-tests.yml` and `performance-tests.yml`
workflows?

- [ ] (A) **Two new files** — add `ci.yml` (build + `./mvnw verify` gate on PRs) and `cd.yml`
      (build image → publish to GHCR → deploy, on push to `main`). Leave the existing e2e and
      performance workflows untouched.
- [ ] (B) **One new combined file** — a single `pipeline.yml` with a `build-test` job (runs on
      PR and main) and `publish-deploy` jobs gated to run only on push to `main` via job-level
      `if:` conditions.
- [ ] (C) **Extend/consolidate** — fold the new build+test gate into the existing workflows and
      restructure everything into a unified pipeline.
- [ ] (D) Other (describe)

**Current best-practice context:** Separating "CI" (fast feedback on every PR) from "CD"
(release/deploy on the protected branch) is a widely used pattern — it keeps triggers,
permissions, and least-privilege token scopes cleanly separated (CD needs `packages: write`;
CI does not).

**Recommended answer(s):** [(A)]

**Why these are recommended:**

- `(A)` maps directly to the demoable units in the spec (test gate vs publish vs deploy) and
  keeps each workflow's `permissions:` scoped to only what it needs.
- `(A)` avoids touching the already-working e2e/performance workflows, reducing regression risk.
- `(B)` is fine but mixes PR-triggered and main-triggered concerns in one file, making the
  least-privilege permissions story slightly messier.
- `(C)` is the most disruptive and risks breaking existing green workflows for little benefit.

---

## 3. Test gate strictness (coverage + merge blocking)

The PR job runs `./mvnw verify`. How strict should the gate be?

- [ ] (A) **Tests + style must pass** — `./mvnw verify` runs unit + integration tests and the
      existing Checkstyle/spring-javaformat validation; any failure fails the check. JaCoCo
      report is generated and uploaded as an artifact, but no numeric coverage threshold is
      enforced in CI yet.
- [ ] (B) **Tests + style + coverage threshold** — same as (A), plus fail the build if JaCoCo
      line coverage drops below a fixed threshold (the project docs mention a 90% target).
- [ ] (C) **Tests only** — just run the tests; skip style enforcement in CI (rely on pre-commit
      for style).
- [ ] (D) Other (describe)

**Current best-practice context:** `CLAUDE.md`/`docs` state a ">90% line coverage for new code"
standard, but the *existing* codebase may not hit 90% globally today. Enforcing a hard global
threshold in CI can immediately red-fail the pipeline on legacy code unrelated to this work.

**Recommended answer(s):** [(A)]

**Why these are recommended:**

- `(A)` reliably closes the actual friction point — "PRs have no server-side test gate" — without
  the risk of an unverified global coverage number failing the very first run on pre-existing code.
- `(A)` still surfaces coverage (report artifact), so a threshold can be layered in later once we
  know the current baseline number.
- `(B)` is a great goal but should follow a measurement step; enforcing 90% blind may block merges
  on legacy gaps and derail the demo. Choose it only if you want me to first measure and set a
  realistic threshold.
- `(C)` under-delivers given the repo already values style enforcement (`spring-javaformat` runs in
  the Maven `validate` phase anyway, so `verify` includes it — making style enforcement essentially
  free).

---

## 4. GHCR image tagging

You specified: build via Spring Boot buildpacks, publish to GHCR, versioned by commit SHA.
Beyond the immutable `:<sha>` tag, should the pipeline also push a moving tag?

- [ ] (A) **`:<sha>` + `:latest`** — immutable SHA tag for traceability, plus `latest` updated on
      every main build for convenience.
- [ ] (B) **`:<sha>` only** — strictly immutable, no moving tags.
- [ ] (C) **`:<sha>` + `:latest` + git tag → semver** — also publish a semver tag when a git tag
      is pushed (adds release-tag handling).
- [ ] (D) Other (describe)

**Current best-practice context:** Immutable per-commit tags are the traceability baseline; a
moving `latest` is convenient for humans and for the K8s manifest to reference, but should never
be the sole tag used for deployment (the deploy should pin the exact SHA it built).

**Recommended answer(s):** [(A)]

**Why these are recommended:**

- `(A)` gives both traceability (deploy pins the exact `:<sha>` just built) and a convenient
  human-facing `:latest`, matching common GHCR usage.
- `(B)` is perfectly safe but slightly less convenient for manual pulls/demo.
- `(C)` adds release-management scope (git-tag triggers) beyond this iteration's goal; defer unless
  you want semver releases now.
