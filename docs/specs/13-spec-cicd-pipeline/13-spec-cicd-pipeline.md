# 13-spec-cicd-pipeline

## Introduction/Overview

This feature adds a full CI/CD pipeline for the Emerald Grove pet clinic app using GitHub Actions,
Amazon ECR, Terraform, and AWS App Runner. Today, pull requests have **no server-side build/test
gate** (correctness is only enforced locally via pre-commit), and **nothing builds, publishes, or
deploys the application image**. This feature closes both gaps: every pull request is gated by a
server-side build + test run, every merge to `main` produces an immutable, deployable container
image published to a registry, and that image is deployed to a managed AWS App Runner service with
verified health — all provisioned by Terraform with the simplest possible AWS footprint (no VPC,
no RDS; the app runs with its embedded H2 database).

The work is intentionally sequenced so the **deployable artifact is built in GitHub first**
(independently demoable, no AWS required), and the AWS deployment layer is built on top of it
afterward.

## Goals

- Add a server-side **build + test gate** that runs `./mvnw verify` on every pull request and blocks
  merging on failure, removing the "no CI test gate" friction.
- Produce an **immutable, deployable container image** on every merge to `main`, published to GitHub
  Container Registry (GHCR) as the first milestone — proving the artifact builds in GitHub with no
  AWS dependency.
- Provision the **minimal AWS footprint** (ECR repository, App Runner service, IAM roles, GitHub
  OIDC deploy role) as code via **Terraform**, with no VPC or RDS.
- **Gate and deploy** to AWS App Runner on merge to `main`: block merges behind the required CI
  check, then require manual reviewer approval (a `production` environment) before deploying the
  exact commit image and verifying the service reaches a healthy `RUNNING` state.
- Keep the pipeline **secure and low-friction**: OIDC federation for AWS (no long-lived secrets),
  least-privilege permissions, and pinned action versions — mirroring the conventions of the
  existing `e2e-tests.yml` / `performance-tests.yml` workflows.

## User Stories

**As a developer**, I want my pull request to automatically build and run the full test suite on a
clean server so that I get objective, shared confirmation my change is safe before it can merge.

**As a maintainer**, I want merges to `main` blocked when the build or tests fail so that broken
code cannot reach the release branch.

**As a developer**, I want every merge to `main` to produce a versioned, deployable container image
in GitHub so that I always have a traceable artifact tied to a specific commit.

**As a maintainer**, I want the AWS resources for hosting the app defined in Terraform so that the
deployment environment is reproducible, reviewable, and not hand-clicked in a console.

**As a maintainer**, I want merges to `main` to automatically deploy the freshly built image to a
managed AWS service and confirm it is healthy so that the running app always reflects the latest
approved code without manual steps.

## Demoable Units of Work

> Units are ordered by the requested build sequence: the deployable artifact (Units 1–2) is proven
> in GitHub first; the AWS deployment layer (Units 3–4) is built on top afterward.

### Unit 1: Pull Request Build & Test Gate (`ci.yml`)

**Purpose:** Give every pull request a server-side build + test gate that blocks merging on failure.
Serves developers and maintainers. Fully independent of AWS.

**Pre-conditions (already exist):**

- Maven wrapper `./mvnw` and `pom.xml` (Spring Boot 4.0.0, Java 17) with Checkstyle,
  spring-javaformat, and JaCoCo already configured.
- Existing workflow conventions in `.github/workflows/e2e-tests.yml`.

**Functional Requirements:**

- The repository shall include a new workflow file `.github/workflows/ci.yml`.
- The workflow shall trigger on every `pull_request` event and on `push` to `main`.
- The workflow shall check out the code, set up JDK 17 (Temurin distribution), and cache the Maven
  (`~/.m2`) dependencies to speed up runs.
- The workflow shall run `./mvnw verify`, which executes unit + integration tests and the existing
  Checkstyle / spring-javaformat validation.
- The job shall fail (non-zero) if any test, style, or format check fails.
- The workflow shall declare least-privilege `permissions:` (`contents: read`).
- The workflow shall upload the JaCoCo coverage report (`target/site/jacoco/`) as a GitHub Actions
  artifact with `if: always()` so it is available even on failure.
- A failed run shall produce a failed GitHub Actions check that blocks pull request merging,
  consistent with how `e2e-tests.yml` behaves.
- The `main` branch shall have a **branch protection rule requiring the `ci.yml` status check to
  pass** before a pull request can be merged, so broken code cannot reach `main` (and therefore
  cannot deploy). This is a repository setting delivered alongside the workflow.
- No numeric coverage threshold is enforced in this unit (report only).

**Proof Artifacts:**

- Screenshot: GitHub Actions check "CI" shown **passed** on a clean PR, demonstrating the gate runs
  green end-to-end.
- Screenshot: GitHub Actions check "CI" shown **failed** on a PR with a deliberately broken test,
  demonstrating merge is blocked.
- Screenshot: `jacoco` coverage-report artifact available for download on a run, demonstrating
  coverage is published.

### Unit 2: Build & Publish Deployable Image to GHCR (`cd.yml`, build job)

**Purpose:** On merge to `main`, build the application container image and publish it to GitHub
Container Registry as an immutable, traceable artifact. This is the "artifact built in GitHub"
milestone and requires **no AWS**.

**Functional Requirements:**

- The repository shall include a new workflow file `.github/workflows/cd.yml`.
- The `cd.yml` build job shall trigger on `push` to `main`.
- The job shall build the application container image using Spring Boot buildpacks
  (`./mvnw spring-boot:build-image`).
- The job shall publish the image to GHCR (`ghcr.io/<owner>/<repo>`) authenticated with the
  built-in `GITHUB_TOKEN` (no external secret), using `permissions: packages: write`.
- The image shall be tagged with both the immutable commit SHA (`:<sha>`) and a moving `:latest`
  tag.
- The build job shall fail if the image build or push fails.
- The published image tag (`:<sha>`) shall be exposed as a job output for downstream deploy jobs.

**Proof Artifacts:**

- Screenshot: GHCR package page showing the image with both `:<sha>` and `:latest` tags,
  demonstrating the deployable artifact exists in GitHub.
- Screenshot: GitHub Actions "CD" build job log showing a successful buildpacks image build and
  push, demonstrating the artifact pipeline works end-to-end.
- CLI: `docker pull ghcr.io/<owner>/<repo>:<sha>` succeeds, demonstrating the artifact is pullable.

### Unit 3: AWS Infrastructure as Code (Terraform)

**Purpose:** Provision the minimal AWS footprint required to host the app on App Runner, as
reproducible, reviewable code. Serves maintainers. No VPC, no RDS.

**Functional Requirements:**

- The repository shall include a `terraform/` directory containing the App Runner deployment
  infrastructure.
- The Terraform configuration shall define, at minimum:
  - An **Amazon ECR repository** to hold the deployable image.
  - An **App Runner service** configured to run the ECR image on port `8080`, with an HTTP health
    check, autoscaling min/max instance settings, and `SPRING_PROFILES_ACTIVE` left at the default
    (embedded H2 — no external database).
  - An **App Runner ECR access role** with the `AWSAppRunnerServicePolicyForECRAccess` managed
    policy so App Runner can pull the private image.
  - An **App Runner instance role** for the running service.
  - A **GitHub OIDC identity provider** and a least-privilege **IAM deploy role** whose trust policy
    is scoped to this repository, assumable by GitHub Actions via `AssumeRoleWithWebIdentity`.
- The Terraform configuration shall pin the AWS provider version and declare a documented input for
  AWS region (default `us-east-1`) and resource names (default base `emerald-grove-petclinic`).
- `terraform validate` and `terraform plan` shall succeed against the configuration.
- The Terraform state backend and any secrets shall not be committed (see Security Considerations).

**Proof Artifacts:**

- CLI: `terraform validate` output showing success, demonstrating the configuration is well-formed.
- CLI: `terraform plan` output showing the ECR repo, App Runner service, and IAM roles to be
  created, demonstrating the intended footprint.
- Screenshot: AWS console (or `terraform apply` output) showing the created App Runner service and
  ECR repository, demonstrating the infrastructure exists.

### Unit 4: Deploy to App Runner with Verification (`cd.yml`, deploy job)

**Purpose:** On merge to `main`, deploy the freshly built image to the App Runner service and verify
it becomes healthy. Serves maintainers and end users.

**Functional Requirements:**

- The `cd.yml` deploy job shall run after the build job on `push` to `main` and shall declare
  `needs: build` so it only runs if the image build succeeded.
- The deploy job shall run inside a GitHub **deployment Environment named `production`** that is
  configured with a **required reviewer**, so the job **pauses after the build job until a reviewer
  approves** (continuous delivery). The environment shall also restrict deployments to the `main`
  branch. This makes the manual approval the explicit deploy gate.
- The job shall authenticate to AWS using **OIDC** via `aws-actions/configure-aws-credentials`
  assuming the Terraform-provisioned deploy role, with `permissions: id-token: write` — no
  long-lived AWS keys stored in GitHub.
- The job shall push the deployable image (the exact `:<sha>` built in Unit 2) to the ECR repository
  provisioned in Unit 3.
- The job shall trigger an App Runner deployment pinned to that exact `:<sha>` image (workflow-
  triggered deployment, not tag-watching auto-deploy).
- The job shall wait for the App Runner operation to reach a successful `RUNNING` state and shall
  fail if the deployment fails or rolls back.
- On success, the job shall surface the App Runner service URL in the job log/summary.

**Proof Artifacts:**

- Screenshot: GitHub Actions run showing the deploy job **paused on the `production` environment
  awaiting approval**, then proceeding after a reviewer approves, demonstrating the manual deploy
  gate.
- Screenshot: GitHub Actions "CD" deploy job log showing OIDC auth, ECR push, and App Runner
  deployment reaching `RUNNING`, demonstrating the automated deploy works.
- Screenshot / URL: the live App Runner service URL serving the pet clinic home page,
  demonstrating the deployed app is reachable and healthy.
- Screenshot: AWS App Runner console showing the active deployment pinned to the commit `:<sha>`,
  demonstrating image traceability from commit to running service.

## Non-Goals (Out of Scope)

1. **Persistent database / RDS**: The deployed app runs with the embedded in-memory H2 database.
   No RDS, no VPC connector, no external data store. Data is ephemeral by design.
2. **Kubernetes deployment**: The existing `/k8s/` manifests are not the deploy path for this
   feature and are left unchanged. App Runner replaces them for this deliverable.
3. **Multi-environment promotion**: A single environment (one App Runner service) is deployed on
   merge to `main`. No staging/prod separation, blue-green, or canary strategy.
4. **Coverage threshold gating**: The CI gate runs tests and publishes a coverage report but does
   not enforce a numeric coverage percentage. Threshold enforcement is a future iteration.
5. **Modifying existing workflows**: `e2e-tests.yml` and `performance-tests.yml` are left as-is.
6. **Semver / release-tag publishing**: Only `:<sha>` and `:latest` tags are produced; git-tag-driven
   semantic version releases are out of scope.
7. **Terraform remote state provisioning**: Choosing/creating a shared remote state backend
   (e.g. S3 + DynamoDB) is out of scope; local or a documented pre-existing backend is assumed.

## Design Considerations

No specific UI/UX design requirements. The primary human-facing outputs are the GitHub Actions
check statuses, the GHCR package page, the Terraform plan output, and the live App Runner service
URL serving the existing pet clinic UI (unchanged by this feature).

## Repository Standards

- **Workflow structure**: Follow the pattern in `.github/workflows/e2e-tests.yml` — `actions/checkout@v4`,
  `actions/setup-java@v4` with Temurin JDK 17, explicit `timeout-minutes`, and artifact uploads with
  `if: always()`.
- **Least-privilege permissions**: Each workflow declares only the `permissions:` it needs
  (`contents: read` for CI; `packages: write` for the GHCR build; `id-token: write` +
  `contents: read` for the AWS deploy job), matching the explicit `permissions:` block already used
  in `performance-tests.yml`.
- **Pinned actions**: Third-party actions are referenced by a stable released version tag.
- **Maven conventions**: Use the `./mvnw` wrapper; do not introduce a parallel Gradle CI path.
- **Commit style**: Conventional commits (`feat:`, `chore:`, `ci:`) as used in the project history.
- **Branch protection**: Changes are delivered via pull request; direct commits to `main` are
  blocked by the `no-direct-commits-to-main` pre-commit hook.
- **Terraform layout**: Standard `terraform/` directory with `main.tf`, `variables.tf`,
  `outputs.tf`, and pinned provider versions.

## Technical Considerations

- **Image build**: Spring Boot buildpacks (`spring-boot:build-image`) produce the OCI image with no
  hand-written Dockerfile, consistent with the project's documented containerization approach.
- **Two registries by design**: GHCR is the GitHub-native artifact target for the build milestone
  (Unit 2, `GITHUB_TOKEN` only). App Runner can only pull image-based services from **private ECR**
  (not GHCR), so the deploy job (Unit 4) pushes the same immutable `:<sha>` image to ECR. This keeps
  the build phase AWS-free while satisfying App Runner's registry requirement.
- **AWS authentication**: Use GitHub OIDC federation with `aws-actions/configure-aws-credentials`
  (short-lived credentials via `AssumeRoleWithWebIdentity`); the IAM trust policy is scoped to this
  repository. This is the current AWS/GitHub best practice and avoids storing long-lived keys.
  *Fallback:* if the OIDC provider/role cannot be created in the target account, repository secrets
  (`AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY`) may be substituted.
- **App Runner deployment**: Use a workflow-triggered deployment of the explicit `:<sha>` tag and
  wait on the operation status; this is preferred over auto-deploy-on-push for controlled, traceable
  releases and stronger in-pipeline verification.
- **Health check**: App Runner health check targets an HTTP path on port 8080 (the app root `/`
  returns the home page; `actuator` health endpoints are also available if a dedicated path is
  preferred).
- **Scope note**: Bundling CI + image publish + Terraform + deploy in one spec is on the larger
  side, but the AWS footprint is deliberately minimal (no VPC/RDS), keeping the Terraform and deploy
  units small and reviewable.

## Security Considerations

- **No long-lived AWS credentials**: OIDC federation is preferred so no AWS access keys are stored
  in GitHub. If the fallback keys are used, they must live only in GitHub encrypted secrets, never
  in the repo.
- **Least-privilege IAM**: The GitHub deploy role's trust policy is scoped to this repository, and
  its permissions are limited to ECR push and App Runner deploy actions on the specific resources.
- **Least-privilege workflow tokens**: Each workflow requests only the `permissions:` scopes it
  needs.
- **No committed secrets/state**: Terraform state (which can contain sensitive values), `.tfvars`
  with secrets, and any AWS credentials must be git-ignored and never committed. `GITHUB_TOKEN` is
  ephemeral and injected by Actions.
- **GHCR image visibility**: The GHCR package visibility should be set deliberately (private unless
  public distribution is intended).

## Success Metrics

1. **PR gate coverage**: 100% of new pull requests trigger the `ci.yml` build+test check, and a
   failing test/style check blocks merge (verified by an intentionally broken PR).
2. **Artifact production**: Every merge to `main` publishes a `:<sha>`-tagged image to GHCR that is
   pullable (verified by `docker pull`).
3. **Reproducible infra**: `terraform plan`/`validate` succeed and `terraform apply` creates the
   ECR repo + App Runner service + IAM roles with no manual console steps.
4. **Gated deploy**: The deploy job is blocked until a reviewer approves the `production`
   environment; after approval, the exact commit image is deployed to App Runner and the service
   reaches `RUNNING`, with the live URL serving the app (verified end-to-end).
5. **No stored AWS secrets**: The deploy authenticates via OIDC with zero long-lived AWS keys in the
   repository (verified by absence of AWS key secrets).

## Open Questions

1. **AWS config values**: Confirm the target AWS **region** (default assumed `us-east-1`), the
   **App Runner service name**, and the **ECR repository name** (default base assumed
   `emerald-grove-petclinic`).
2. **Terraform state backend**: Is there a preferred remote backend (e.g. S3 + DynamoDB), or is
   local/pre-existing state acceptable for this iteration?
3. **App Runner sizing**: Confirm CPU/memory (default smallest tier, e.g. 0.25 vCPU / 0.5 GB) and
   autoscaling min/max instances are acceptable for the demo.
4. **OIDC feasibility**: Confirm the target AWS account permits creating a GitHub OIDC provider and
   IAM role; otherwise we fall back to stored access keys.
