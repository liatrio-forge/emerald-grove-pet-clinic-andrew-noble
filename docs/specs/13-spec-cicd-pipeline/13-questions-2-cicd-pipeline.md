# 13 Questions Round 2 - CI/CD Pipeline (AWS App Runner deployment)

> **Note:** This file was revised after you chose **AWS App Runner** as the deploy target.
> The earlier EKS-specific version has been replaced (it was unanswered, so nothing is lost).

Decisions already settled:

- **Deploy target:** AWS App Runner (managed container service; no Kubernetes, no `/k8s/` manifests
  in the deploy path).
- **Registry:** Amazon ECR (App Runner pulls the private image from ECR via a service access role).
- **Workflow layout:** two new files — `ci.yml` (PR test gate) + `cd.yml` (build → push ECR →
  deploy App Runner on merge to `main`).
- **Test gate:** `./mvnw verify` (unit + integration + Checkstyle/spring-javaformat); JaCoCo report
  uploaded, no numeric threshold enforced yet.
- **Image tags:** immutable `:<sha>` + moving `:latest`.

Please answer the four remaining questions, save, and tell me.

---

## 1. Who creates/manages the App Runner service (and IAM roles)?

App Runner needs a service (plus an ECR **access role** and an **instance role**). How should
those be provisioned?

- [ ] (A) **Workflow-managed** — let the `awslabs/amazon-app-runner-deploy` action create-or-update
      the App Runner service idempotently on each run; create the two IAM roles once by hand (or a
      short documented AWS CLI snippet). Fastest path to a working deploy; minimal IaC.
- [ ] (B) **Terraform in this spec** — define the App Runner service, ECR repo, and IAM roles in
      Terraform as part of this deliverable; the workflow only pushes the image and triggers a
      deploy. Adds an IaC artifact but enlarges the spec.
- [ ] (C) **Split** — this spec builds the pipeline (service managed by the action as in (A)); a
      **separate spec (14)** captures Terraform for the App Runner service + ECR + roles as its own
      IaC deliverable.
- [ ] (D) Other (describe)

**Current best-practice context:** The `awslabs/amazon-app-runner-deploy` action supports
create-or-update of an image-based service, so a working pipeline needs no separate provisioning
tool. The module's deliverables do call for Terraform/IaC, but provisioning is cleanly separable
from the delivery pipeline and is often its own unit of work.

**Recommended answer(s):** [(C), or (A) if you don't need a separate Terraform artifact]

**Why these are recommended:**

- `(C)` keeps this spec lean and demoable while still producing a Terraform deliverable for the
  module — as a focused, reviewable second spec rather than bloating this one.
- `(A)` is the absolute fastest route to a green end-to-end deploy if you don't need Terraform now.
- `(B)` is valid but risks an oversized spec mixing pipeline + infra provisioning.

**Please also note:** the AWS **region** and desired **App Runner service name** (e.g.
`us-east-1` / `emerald-grove`), plus the **ECR repository name** (e.g. `emerald-grove-petclinic`).

---

## 2. Database for the deployed app

The app defaults to an in-memory **H2** database; the K8s setup used PostgreSQL. What should the
App Runner deployment run against?

- [ ] (A) **Default H2 (in-memory)** — deploy with the default profile; the running service is fully
      self-contained with sample data. Data resets on each deploy/restart. Simplest; no extra infra.
- [ ] (B) **Amazon RDS PostgreSQL** — provision an RDS instance, wire App Runner to it (VPC
      connector + DB credentials via secret), and run the `postgres` profile. Realistic and
      persistent, but adds significant AWS infra, networking, and secrets scope.
- [ ] (C) Other (describe)

**Current best-practice context:** For a pipeline-focused deliverable whose goal is proving
build→test→deploy, a self-contained image (H2) yields a working, publicly reachable app with no
data-tier dependencies. A managed database (RDS) is a separate infrastructure concern typically
specced on its own.

**Recommended answer(s):** [(A) Default H2]

**Why these are recommended:**

- `(A)` produces a genuinely working, reachable deployment (App Runner HTTPS URL serving the app)
  that satisfies the exit criterion, without dragging RDS + VPC networking into this spec.
- `(A)` keeps the demo clean and reproducible; persistence isn't needed to prove the pipeline.
- `(B)` is a reasonable future spec (pairs naturally with the Terraform split in Q1) but would
  materially enlarge this deliverable and its AWS footprint/cost.

---

## 3. How GitHub Actions authenticates to AWS

You have AWS credentials. How should the pipeline use them?

- [ ] (A) **OIDC federation (no stored keys)** — register GitHub's OIDC provider in your AWS
      account and create an IAM role scoped to this repo; the workflow assumes it via
      `aws-actions/configure-aws-credentials`. One-time AWS setup; no long-lived secrets.
- [ ] (B) **Stored access keys** — put `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` in GitHub repo
      secrets. Simpler to start; long-lived credentials to guard and rotate.
- [ ] (C) Other (describe)

**Current best-practice context:** AWS + GitHub guidance strongly favors OIDC — short-lived,
per-run credentials via `AssumeRoleWithWebIdentity`, scoped to specific repos/branches by the IAM
trust policy. Stored access keys are discouraged for new pipelines.

**Recommended answer(s):** [(A) OIDC]

**Why these are recommended:**

- `(A)` avoids standing AWS credentials in GitHub — a strong security story for a DevOps deliverable.
- `(A)`'s one-time setup is modest and can be documented (or folded into the Terraform split).
- `(B)` is an acceptable **fallback** if you cannot create the OIDC provider / IAM role in the
  account — pick it and note that, and we'll use repo secrets instead.

---

## 4. Deploy trigger & verification

How should the new image reach App Runner, and how do we confirm success?

- [ ] (A) **Workflow-triggered deploy of the exact `:<sha>`, then wait** — the CD job points App
      Runner at the specific `:<sha>` it just pushed and waits for the App Runner operation to reach
      a successful `RUNNING` state (fail the job on a failed/rolled-back deployment). Controlled and
      fully provable.
- [ ] (B) **App Runner auto-deploy on ECR push** — enable App Runner automatic deployments so it
      redeploys whenever a watched tag changes; the workflow just pushes the image.
- [ ] (C) Other (describe)

**Current best-practice context:** Guidance recommends explicit version tags with a
workflow-triggered deploy for controlled releases and clean traceability, over auto-deploy on a
moving tag. Waiting on the App Runner operation status turns a bad image into a red build rather
than a silent failure.

**Recommended answer(s):** [(A)]

**Why these are recommended:**

- `(A)` pins the deploy to the exact commit built and gives real proof of health (operation
  SUCCEEDED / service RUNNING) — ideal demo evidence.
- `(A)` avoids surprise redeploys and works correctly with immutable `:<sha>` tags.
- `(B)` is convenient but couples deploys to tag-watching and gives weaker in-pipeline verification.
