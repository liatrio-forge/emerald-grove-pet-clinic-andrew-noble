# 13-tasks-cicd-pipeline

Task list derived from `13-spec-cicd-pipeline.md`. Parent tasks are ordered to match the requested
build sequence: the deployable artifact is built in GitHub first (1.0–2.0), then the AWS deployment
layer is built on top (3.0–4.0).

> **Phase boundary:** Tasks 1.0 and 2.0 use only the built-in `GITHUB_TOKEN` and require **no AWS**.
> They can be implemented, run, and demoed to completion before any work on 3.0–4.0 begins.
>
> **Standards note (TDD precedence for infra):** This feature adds no Java production code. Per the
> repo's Strict TDD mandate, the equivalent "test-first" evidence for pipeline/IaC work is
> validation tooling and live-run proof: `terraform validate`/`plan`, workflow/YAML validation
> (`actionlint` + pre-commit `check-yaml`), and actual GitHub Actions run results (including a
> deliberately-broken-test PR proving the gate blocks). These substitute for JUnit tests and
> coverage on this spec.

## Relevant Files

| File | Why It Is Relevant |
| --- | --- |
| `.github/workflows/ci.yml` | New PR build+test gate workflow (Task 1.0). |
| `.github/workflows/cd.yml` | New CD workflow containing the `build` job (Task 2.0) and the gated `deploy` job (Task 4.0). |
| `.github/workflows/e2e-tests.yml` | Reference for existing workflow conventions (checkout@v4, setup-java@v4 Temurin 17, `if: always()`, explicit `permissions:`). Not modified. |
| `terraform/versions.tf` | Pins Terraform + AWS provider versions (Task 3.0). |
| `terraform/main.tf` | ECR repo, App Runner service, IAM roles, GitHub OIDC provider + deploy role (Task 3.0). |
| `terraform/variables.tf` | Inputs: region, resource name base, image tag, autoscaling sizing (Task 3.0). |
| `terraform/outputs.tf` | Outputs: App Runner service URL, ECR repo URL, deploy role ARN (Task 3.0). |
| `.gitignore` | Add ignores for Terraform state, `.terraform/`, and `*.tfvars` so secrets/state are never committed (Task 3.0). |
| `scripts/setup-branch-protection.sh` | Reproducible `gh api` script to require the `CI` check on `main` (Task 1.0). Must pass `shellcheck`. |
| `scripts/setup-github-environment.sh` | Reproducible `gh api` script to create the `production` environment with a required reviewer + branch restriction (Task 4.0). Must pass `shellcheck`. |
| `docs/CICD.md` | New doc: pipeline overview, the two-registry rationale, and the one-time manual setup (branch protection, `production` environment, AWS OIDC role) (Tasks 1.0–4.0). |
| `README.md` | Add a brief CI/CD section/link to `docs/CICD.md` (Task 1.0/2.0). |
| `docs/specs/13-spec-cicd-pipeline/13-proofs/` | Destination for proof artifacts (screenshots, CLI output) captured during implementation/validation. |

### Notes

- **No JUnit test files are added** by this feature; it introduces GitHub Actions YAML, Terraform
  HCL, and shell helper scripts. "Tests" are the validation commands and live pipeline runs listed
  in each task's Proof Artifacts (see the Standards note above).
- **Pre-commit gates that apply here:** `check-yaml` validates the new workflow files (the k8s
  exclusion does **not** cover `.github/workflows/`), `shellcheck` validates the helper scripts,
  and `markdownlint` validates `docs/CICD.md` and these spec docs (fenced code blocks with a
  language, ≤120-char lines).
- **Local validation before pushing workflows:** run `actionlint` on the workflow files and
  `pre-commit run --all-files` to catch YAML/shell/markdown issues before opening a PR.
- **Commits:** conventional commits (`ci:`, `feat:`, `docs:`, `chore:`); deliver via PR on a feature
  branch (direct commits to `main` are blocked by pre-commit).
- **Repository settings** (branch protection, `production` environment) cannot be delivered by
  committed files alone. They are scripted via `gh api` and documented so they are reproducible
  rather than click-ops; running them requires repo admin.

## Tasks

### [x] 1.0 Pull Request Build & Test Gate (`ci.yml`) + branch protection

Adds a server-side build+test gate on every PR (and `push` to `main`) via `./mvnw verify`, uploads
the JaCoCo report, and enforces the check as a required status check on `main` so broken code cannot
merge. Delivers Spec Unit 1.

#### 1.0 Proof Artifact(s)

- Screenshot: GitHub Actions check "CI" shown **passed** on a clean PR demonstrates the gate runs
  green end-to-end (Spec FR: trigger on PR, run `./mvnw verify`).
- Screenshot: GitHub Actions check "CI" shown **failed** on a PR with a deliberately broken test,
  with the merge button blocked, demonstrates the required-check merge gate works.
- Screenshot: `jacoco` coverage-report artifact downloadable on a run demonstrates coverage is
  published with `if: always()`.
- CLI: `gh api repos/:owner/:repo/branches/main/protection` output showing the required `CI` check
  demonstrates branch protection is configured reproducibly.

#### 1.0 Tasks

- [x] 1.1 Create `.github/workflows/ci.yml` triggered on `pull_request` and `push` to `main`, with
  `permissions: contents: read` and a `timeout-minutes` guard.
- [x] 1.2 Add job steps: `actions/checkout@v4`; `actions/setup-java@v4` (distribution `temurin`,
  `java-version: 17`, `cache: maven`); run `./mvnw verify`.
- [x] 1.3 Add a step to upload the JaCoCo report (`target/site/jacoco/`) as an artifact named
  `jacoco` with `if: always()`.
- [x] 1.4 Validate the workflow locally with `actionlint` and `pre-commit run check-yaml`, then push
  a feature branch and open a PR; capture the **passing** "CI" check screenshot.
- [x] 1.5 On a throwaway branch, add a deliberately failing unit test, open a PR, and capture the
  **failing** "CI" check with the merge blocked; then discard the branch/test.
- [x] 1.6 Write `scripts/setup-branch-protection.sh` (`gh api`) to require the `CI` status check on
  `main`; run it and capture the `gh api .../branches/main/protection` output. Ensure it passes
  `shellcheck`.
- [x] 1.7 Add a CI/CD section to `README.md` linking to `docs/CICD.md`; document the CI gate and
  branch-protection setup in `docs/CICD.md`.

### [ ] 2.0 Build & Publish Deployable Image to GHCR (`cd.yml` build job)

On `push` to `main`, builds the app container image with Spring Boot buildpacks and publishes it to
GHCR tagged `:<sha>` and `:latest`, using only `GITHUB_TOKEN` (no AWS). This is the "artifact built
in GitHub" milestone and the recommended **stop-and-test** point before AWS work. Delivers Spec
Unit 2.

#### 2.0 Proof Artifact(s)

- Screenshot: GHCR package page showing the image with both `:<sha>` and `:latest` tags demonstrates
  the deployable artifact exists in GitHub.
- Screenshot: GitHub Actions "CD" build-job log showing a successful buildpacks build and push
  demonstrates the artifact pipeline works end-to-end.
- CLI: `docker pull ghcr.io/<owner>/<repo>:<sha>` succeeds demonstrates the artifact is pullable
  (committed docs use `<owner>`/`<repo>` placeholders).

#### 2.0 Tasks

- [ ] 2.1 Create `.github/workflows/cd.yml` triggered on `push` to `main` (add `workflow_dispatch`
  for manual test runs), with a `build` job and `permissions: contents: read, packages: write` and a
  `timeout-minutes` guard.
- [ ] 2.2 Add build steps: `actions/checkout@v4`; `actions/setup-java@v4` (Temurin 17, `cache:
  maven`); compute tags from `${{ github.sha }}` and `latest`.
- [ ] 2.3 Log in to GHCR with `docker/login-action@v3` using `${{ github.actor }}` and
  `${{ secrets.GITHUB_TOKEN }}`.
- [ ] 2.4 Build the image with `./mvnw spring-boot:build-image` named
  `ghcr.io/${{ github.repository }}:${{ github.sha }}`, then `docker tag` it `:latest` and
  `docker push` both tags.
- [ ] 2.5 Expose the published `:<sha>` reference as a `build` job output (for the future deploy
  job) using `$GITHUB_OUTPUT`.
- [ ] 2.6 Validate by running the workflow (via `workflow_dispatch` or a merge to `main`); capture
  the build-job log, the GHCR package page (both tags), and a successful `docker pull` of the
  `:<sha>` image. Set the GHCR package visibility deliberately and note it in `docs/CICD.md`.

### [ ] 3.0 AWS Infrastructure as Code (Terraform)

Provisions the minimal AWS footprint (ECR repo, App Runner service on port 8080 with embedded H2,
App Runner ECR-access + instance roles, GitHub OIDC provider + repo-scoped deploy role) with pinned
provider versions and no VPC/RDS. Delivers Spec Unit 3.

#### 3.0 Proof Artifact(s)

- CLI: `terraform validate` output showing "Success! The configuration is valid." demonstrates the
  config is well-formed.
- CLI: `terraform plan` output listing the ECR repo, App Runner service, and IAM roles to be created
  demonstrates the intended minimal footprint.
- Screenshot: `terraform apply` completion (or AWS console) showing the created App Runner service +
  ECR repository demonstrates the infrastructure exists.
- Diff: `terraform/` files (`versions.tf`, `main.tf`, `variables.tf`, `outputs.tf`) with pinned
  provider versions demonstrates reproducible IaC (no account IDs/secrets committed).

#### 3.0 Tasks

- [ ] 3.1 Create `terraform/versions.tf` pinning `required_version` and the `hashicorp/aws` provider
  version; create `terraform/variables.tf` (region default `us-east-1`, name base
  `emerald-grove-petclinic`, image tag, autoscaling min/max) and `terraform/outputs.tf`.
- [ ] 3.2 Define the `aws_ecr_repository` in `terraform/main.tf`.
- [ ] 3.3 Define the App Runner **ECR access role** (attach `AWSAppRunnerServicePolicyForECRAccess`)
  and the App Runner **instance role** in `terraform/main.tf`.
- [ ] 3.4 Define the `aws_apprunner_service` (image-based from ECR, port 8080, HTTP health check,
  autoscaling min/max, default profile so the app uses embedded H2).
- [ ] 3.5 Define the GitHub OIDC provider (`aws_iam_openid_connect_provider`) and a least-privilege
  deploy role whose trust policy is scoped to this repository and whose permissions allow ECR push +
  App Runner deploy on the specific resources.
- [ ] 3.6 Update `.gitignore` to exclude `.terraform/`, `*.tfstate*`, and `*.tfvars`; run
  `terraform fmt`, `terraform validate`, and `terraform plan`; capture validate + plan output.
- [ ] 3.7 (Optional, when credentials available) `terraform apply` and capture the created App Runner
  service + ECR repo; document all one-time setup in `docs/CICD.md`.

### [ ] 4.0 Gated Deploy to App Runner with Verification (`cd.yml` deploy job)

On `push` to `main`, after the build job, deploys the exact `:<sha>` image to App Runner inside a
`production` GitHub Environment that requires reviewer approval (manual gate), authenticating to AWS
via OIDC, pushing the image to ECR, and waiting for the service to reach `RUNNING`. Delivers Spec
Unit 4.

#### 4.0 Proof Artifact(s)

- Screenshot: GitHub Actions run showing the deploy job **paused on the `production` environment
  awaiting approval**, then proceeding after approval, demonstrates the manual deploy gate.
- Screenshot: "CD" deploy-job log showing OIDC auth, ECR push, and App Runner deployment reaching
  `RUNNING` demonstrates the automated, gated deploy works.
- URL/Screenshot: the live App Runner service URL serving the pet clinic home page demonstrates the
  deployed app is reachable and healthy.
- Screenshot: AWS App Runner console showing the active deployment pinned to commit `:<sha>`
  demonstrates image traceability from commit to running service.
- Screenshot/Log: the deploy job exiting non-zero when pointed at a knowingly-bad image tag / failed
  health check demonstrates the health gate actually blocks a bad deploy (negative-path proof).

#### 4.0 Tasks

- [ ] 4.1 Write `scripts/setup-github-environment.sh` (`gh api`) to create the `production`
  environment with a required reviewer and a `main`-only branch policy; run it and capture the
  result. Ensure it passes `shellcheck`.
- [ ] 4.2 Add a `deploy` job to `cd.yml` with `needs: build`, `environment: production`, and
  `permissions: id-token: write, contents: read`.
- [ ] 4.3 Authenticate to AWS via `aws-actions/configure-aws-credentials@v4` assuming the
  Terraform-provisioned deploy role (region from repo/vars); log in to ECR with
  `aws-actions/amazon-ecr-login@v2`.
- [ ] 4.4 Pull the `:<sha>` image from GHCR (build-job output), retag it to the ECR repository as
  `:<sha>`, and push to ECR.
- [ ] 4.5 Trigger an App Runner deployment pinned to the `:<sha>` ECR image (App Runner deploy action
  or `aws apprunner start-deployment`) and wait for the operation to reach `RUNNING`; fail the job on
  a failed/rolled-back deployment.
- [ ] 4.6 Write the App Runner service URL to the job summary (`$GITHUB_STEP_SUMMARY`).
- [ ] 4.7 Validate end-to-end: merge to `main`, observe the deploy job pause on `production`, approve
  it, and capture the paused-approval screenshot, the deploy log reaching `RUNNING`, the live service
  URL serving the app, and the App Runner console showing the pinned `:<sha>`.
- [ ] 4.8 Validate the negative path: run the deploy against a knowingly-bad image tag (or force a
  failed health check) and capture the deploy job exiting non-zero, demonstrating the health gate
  blocks a bad deploy.
