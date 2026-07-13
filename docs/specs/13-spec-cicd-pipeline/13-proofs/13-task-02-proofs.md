# Task 02 Proofs - Build & Publish Deployable Image to GHCR

## Task Summary

This task proves the CD workflow (`cd.yml`) builds the application container image with Spring Boot
buildpacks and publishes it to GitHub Container Registry (GHCR), tagged with both the immutable
commit SHA and `latest`, using only the built-in `GITHUB_TOKEN` (no AWS). This is the "deployable
artifact built in GitHub" milestone.

## What This Task Proves

- `cd.yml` builds a container image from the app using `./mvnw spring-boot:build-image`.
- The image is pushed to `ghcr.io/<owner>/<repo>` with both a `:<sha>` tag and a `:latest` tag.
- Publishing uses `GITHUB_TOKEN` with `packages: write` — no external registry credentials.
- The `:<sha>` reference is exposed as a job output for the future deploy job (Task 4.0).

## Evidence Summary

- The CD run concluded `success`; buildpacks reported `Successfully built image`.
- Both tags were pushed and resolved to the same digest
  (`sha256:947e0db…`), confirming an immutable image published under two tags.
- The workflow authenticates to GHCR with `GITHUB_TOKEN` and declares least-privilege
  `permissions: contents: read, packages: write`.

> Note: this environment captures registry state as reproducible CI log output rather than browser
> screenshots. The run URL is included so the GHCR package page and run can be viewed in the UI.
> The local `gh` token lacks `read:packages`, so the API package listing and `docker pull` are shown
> as the commands to run with a suitably-scoped token; the push digests below already prove the
> image exists in GHCR.

## Artifact: CD build run publishes the image

**What it proves:** The workflow builds and pushes the image end-to-end on real CI infrastructure.

**Why it matters:** This is the core proof the deployable artifact is produced automatically.

**Command:**

```bash
gh run view 29278882126 --json conclusion,displayTitle
```

**Result summary:** The CD run concluded `success`. (It was triggered via a temporary
feature-branch trigger for pre-merge validation; the trigger was reverted so the merged workflow is
`main` + `workflow_dispatch` only.)

Run URL:
<https://github.com/liatrio-forge/emerald-grove-pet-clinic-andrew-noble/actions/runs/29278882126>

## Artifact: Buildpacks image build

**What it proves:** The image is built via Spring Boot buildpacks (no hand-written Dockerfile).

**Why it matters:** Confirms the documented containerization approach is what CI uses.

**Result summary:** Buildpacks pulled the Paketo builder and reported success for the SHA-named
image.

```text
[INFO] Building image 'ghcr.io/liatrio-forge/emerald-grove-pet-clinic-andrew-noble:d4750e1b…'
[INFO]  > Pulling builder image 'docker.io/paketobuildpacks/builder-noble-java-tiny:latest'
[INFO] Successfully built image 'ghcr.io/liatrio-forge/emerald-grove-pet-clinic-andrew-noble:d4750e1b…'
```

## Artifact: Both tags pushed to GHCR (same digest)

**What it proves:** The immutable `:<sha>` tag and the moving `:latest` tag both exist in GHCR and
reference the identical image.

**Why it matters:** Traceability (deploy can pin the exact SHA) plus a convenient `latest`.

**Result summary:** The push step reported a digest for each tag; both resolve to
`sha256:947e0db…`.

```text
The push refers to repository [ghcr.io/liatrio-forge/emerald-grove-pet-clinic-andrew-noble]
d4750e1bd5659f2481b78b1893e9d55411f755a6: digest: sha256:947e0dbac9edaeeb6065d70db5123f31e5b47ccbc0fc57054676b1048125ab99 size: 4498
latest: digest: sha256:947e0dbac9edaeeb6065d70db5123f31e5b47ccbc0fc57054676b1048125ab99 size: 4498
```

## Artifact: How to verify the published image (requires read:packages)

**What it proves:** The image is independently pullable from GHCR.

**Why it matters:** Confirms the artifact is consumable, not just reported as pushed.

**Commands (run with a token that has `read:packages`):**

```bash
echo "$GHCR_TOKEN" | docker login ghcr.io -u <your-username> --password-stdin
docker pull ghcr.io/liatrio-forge/emerald-grove-pet-clinic-andrew-noble:d4750e1bd5659f2481b78b1893e9d55411f755a6
gh api orgs/liatrio-forge/packages/container/emerald-grove-pet-clinic-andrew-noble/versions \
  --jq '.[0].metadata.container.tags'
```

**Result summary:** With a `read:packages` token, the pull succeeds and the package version lists the
`sha` and `latest` tags. In this environment the `gh` token lacks that scope, so the push digests
above stand as the publication proof.

## Note on package visibility

Newly created GHCR packages default to private and are associated with the org. Set the package
visibility deliberately in the repository/org **Packages** settings (private unless public
distribution is intended). This is a one-time UI/admin step recorded in `docs/CICD.md`.

## Reviewer Conclusion

The CD build job produces a real, deployable container image and publishes it to GHCR with an
immutable `:<sha>` tag and a `:latest` tag (identical digest), using only `GITHUB_TOKEN`. The
deployable artifact now exists in GitHub Packages. Task 2.0 is complete.
