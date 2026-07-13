#!/usr/bin/env bash
#
# Ensure the CI status check ("Build & Test" from .github/workflows/ci.yml) is a
# REQUIRED status check on the default branch, so a failing CI run blocks merging.
#
# This script is additive and idempotent:
#   - If branch protection already exists, it adds the required check via the
#     dedicated required_status_checks endpoint, preserving existing rules
#     (required reviews, push restrictions, other required checks).
#   - If no branch protection exists, it creates a minimal policy with just the
#     required CI check.
#
# Requirements:
#   - gh CLI authenticated (`gh auth status`) with admin permission on the repo
#   - jq
#
# Usage:
#   scripts/setup-branch-protection.sh [owner/repo] [branch]
#
# Defaults: repo = current repo (via gh), branch = main.
# Override the required check name with CHECK_CONTEXT=... if the job name changes.
set -euo pipefail

REPO="${1:-$(gh repo view --json nameWithOwner --jq .nameWithOwner)}"
BRANCH="${2:-main}"
CHECK_CONTEXT="${CHECK_CONTEXT:-Build & Test}"

API="repos/${REPO}/branches/${BRANCH}/protection"

echo "Ensuring branch protection on ${REPO}@${BRANCH} requires check: ${CHECK_CONTEXT}"

if protection="$(gh api "${API}" 2>/dev/null)"; then
  echo "Existing protection found — adding the required check without altering other rules."

  # Read existing required-check settings from the already-fetched protection rule.
  existing="$(jq -c '(((.required_status_checks.checks // []) | map(.context)) + (.required_status_checks.contexts // [])) | map(select(type == "string")) | unique' <<<"${protection}")"
  strict="$(jq -r 'if .required_status_checks == null or .required_status_checks.strict == null then true else .required_status_checks.strict end' <<<"${protection}")"
  merged="$(jq -cn --argjson e "${existing:-[]}" --arg c "${CHECK_CONTEXT}" '($e + [$c]) | unique')"
  payload="$(jq -cn --argjson strict "${strict}" --argjson ctx "${merged}" '{strict: $strict, checks: [$ctx[] | {context: .}]}')"

  printf '%s' "${payload}" | gh api --method PATCH \
    -H "Accept: application/vnd.github+json" \
    "${API}/required_status_checks" --input - >/dev/null
else
  echo "No existing protection — creating a minimal policy with the required check."

  gh api --method PUT \
    -H "Accept: application/vnd.github+json" \
    "${API}" --input - >/dev/null <<JSON
{
  "required_status_checks": { "strict": true, "checks": [ { "context": "${CHECK_CONTEXT}" } ] },
  "enforce_admins": false,
  "required_pull_request_reviews": null,
  "restrictions": null
}
JSON
fi

echo ""
echo "Done. Required status checks now:"
gh api "${API}/required_status_checks" --jq '{strict: .strict, contexts: [.checks[].context]}'
