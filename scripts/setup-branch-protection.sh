#!/usr/bin/env bash
#
# Configure branch protection on the default branch so the CI status check
# ("Build & Test" from .github/workflows/ci.yml) is REQUIRED before a pull
# request can be merged. This makes the CI gate a hard merge gate rather than
# an advisory check.
#
# Requirements:
#   - gh CLI authenticated (`gh auth status`)
#   - jq
#   - admin permission on the target repository
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

echo "Configuring branch protection on ${REPO}@${BRANCH}"
echo "Requiring status check: ${CHECK_CONTEXT}"

EXISTING_PROTECTION="$(gh api "repos/${REPO}/branches/${BRANCH}/protection" 2>/dev/null || true)"

if [[ -n "${EXISTING_PROTECTION}" ]]; then
  STATUS_CHECKS_PAYLOAD="$(
    jq --arg check "${CHECK_CONTEXT}" '
      def normalize_check:
        { context: .context } + (if .app_id? != null then { app_id: .app_id } else {} end);
      def unique_by_context:
        reduce .[] as $item ([]; if any(.[]; .context == $item.context) then . else . + [$item] end);

      (.required_status_checks // {}) as $status
      | {
          strict: (if $status.strict == null then true else $status.strict end),
          checks: (
            ([$status.checks[]? | select(.context != null) | normalize_check]
              + [$status.contexts[]? | { context: . }]
              + [{ context: $check }])
            | unique_by_context
          )
        }
    ' <<< "${EXISTING_PROTECTION}"
  )"

  gh api \
    --method PATCH \
    -H "Accept: application/vnd.github+json" \
    "repos/${REPO}/branches/${BRANCH}/protection/required_status_checks" \
    --input - <<< "${STATUS_CHECKS_PAYLOAD}"
else
  gh api \
    --method PUT \
    -H "Accept: application/vnd.github+json" \
    "repos/${REPO}/branches/${BRANCH}/protection" \
    --input - <<JSON
{
  "required_status_checks": {
    "strict": true,
    "checks": [
      { "context": "${CHECK_CONTEXT}" }
    ]
  },
  "enforce_admins": false,
  "required_pull_request_reviews": null,
  "restrictions": null
}
JSON
fi

echo ""
echo "Branch protection applied. Required status checks:"
gh api "repos/${REPO}/branches/${BRANCH}/protection" --jq '.required_status_checks'
