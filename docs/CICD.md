# CI/CD Pipeline

This document describes the continuous integration and delivery pipeline for the Emerald Grove
Veterinary Clinic application, implemented with GitHub Actions, Amazon ECR, Terraform, and AWS App
Runner.

Full design and rationale live in
[`docs/specs/13-spec-cicd-pipeline/`](specs/13-spec-cicd-pipeline/13-spec-cicd-pipeline.md).

## Pipeline overview

```text
Pull request ──▶ CI gate (ci.yml): ./mvnw verify  ── required check ──▶ merge allowed
                                                                             │
push to main ─────────────────────────────────────────────────────────────┘
     │
     ├─▶ CD build job (cd.yml): buildpacks image ──▶ GHCR (:<sha>, :latest)
     │
     └─▶ CD deploy job (cd.yml): [production environment → manual approval]
             └─▶ push image to ECR ──▶ deploy to App Runner ──▶ wait for RUNNING
```

The pipeline is intentionally split so the deployable artifact is produced in GitHub first (no AWS
required), and the AWS deployment layer builds on top of it.

## Continuous Integration — `ci.yml`

**Status: implemented (Task 1.0).**

- **Triggers:** every `pull_request`, and `push` to `main`.
- **What it does:** checks out the code, sets up Temurin JDK 17 with Maven dependency caching, and
  runs `./mvnw verify` (unit + integration tests plus the existing Checkstyle and spring-javaformat
  validation).
- **Coverage:** the JaCoCo report (`target/site/jacoco/`) is uploaded as a build artifact named
  `jacoco` on every run (`if: always()`), even on failure. No numeric coverage threshold is enforced
  yet.
- **Permissions:** least privilege — `contents: read` only.

### Branch protection (the merge gate)

The CI check only blocks merges if `main` is configured to require it. This is a repository setting,
delivered reproducibly via a script rather than manual clicks:

```bash
# Requires gh CLI authenticated with admin on the repo
scripts/setup-branch-protection.sh            # uses current repo + main
scripts/setup-branch-protection.sh owner/repo main
```

The script requires the `Build & Test` status check (the `ci.yml` job name) to pass before a pull
request can merge. Verify it with:

```bash
gh api repos/:owner/:repo/branches/main/protection --jq '.required_status_checks'
```

## Continuous Delivery — `cd.yml`

**Status: planned (Tasks 2.0 and 4.0).** This section will be completed as those tasks land.

- **Build job (Task 2.0):** on `push` to `main`, builds the container image with Spring Boot
  buildpacks and publishes it to GitHub Container Registry (GHCR) tagged with the commit SHA and
  `latest`, authenticated with the built-in `GITHUB_TOKEN`.
- **Deploy job (Task 4.0):** runs inside a `production` GitHub Environment that requires manual
  reviewer approval, authenticates to AWS via OIDC (no stored keys), pushes the image to ECR, and
  triggers an App Runner deployment pinned to the exact commit image, waiting for `RUNNING`.

### Why two registries

The build milestone publishes to **GHCR** so the deployable artifact exists in GitHub with no AWS
dependency. AWS App Runner can only pull image-based services from **private ECR**, so the deploy
job re-pushes the same immutable `:<sha>` image to ECR. This keeps the build phase AWS-free while
satisfying App Runner's registry requirement.

## Infrastructure as Code — `terraform/`

**Status: planned (Task 3.0).** Terraform will provision the minimal AWS footprint (ECR repository,
App Runner service running the app with its embedded H2 database, App Runner IAM roles, and the
GitHub OIDC provider + repo-scoped deploy role). No VPC or RDS.

## One-time manual setup

These steps require repository admin and/or AWS access and are not delivered by committed files
alone:

1. **Branch protection:** run `scripts/setup-branch-protection.sh` (Task 1.0).
2. **`production` environment + required reviewer:** run `scripts/setup-github-environment.sh`
   (Task 4.0).
3. **AWS OIDC role + resources:** `terraform apply` in `terraform/` (Tasks 3.0–4.0).
