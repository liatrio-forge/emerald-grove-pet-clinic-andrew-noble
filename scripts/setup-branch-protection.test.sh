#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

LOG_FILE="${TMP_DIR}/gh-calls.log"
STATUS_PAYLOAD_FILE="${TMP_DIR}/status-payload.json"
BRANCH_PUT_FILE="${TMP_DIR}/branch-put.json"

mkdir -p "${TMP_DIR}/bin"
touch "${LOG_FILE}" "${STATUS_PAYLOAD_FILE}" "${BRANCH_PUT_FILE}"

cat > "${TMP_DIR}/bin/gh" <<'GH'
#!/usr/bin/env bash

set -euo pipefail

echo "gh $*" >> "${GH_CALL_LOG}"

if [[ "$1" != "api" ]]; then
  echo "unexpected gh command: gh $*" >&2
  exit 1
fi

shift
method="GET"
input=false
endpoint=""

while (($#)); do
  case "$1" in
    --method)
      method="$2"
      shift 2
      ;;
    --input)
      input=true
      shift 2
      ;;
    -H|--jq)
      shift 2
      ;;
    *)
      endpoint="$1"
      shift
      ;;
  esac
done

if [[ "${method}" == "GET" && "${endpoint}" == "repos/example/repo/branches/main/protection" ]]; then
  python3 - <<'PY'
import json

print(json.dumps({
    "required_status_checks": {
        "strict": False,
        "checks": [
            {"context": "lint", "app_id": 123},
            {"context": "security-scan"},
        ],
    },
    "enforce_admins": {"enabled": True},
    "required_pull_request_reviews": {
        "required_approving_review_count": 2,
    },
    "restrictions": {
        "users": [{"login": "release-manager"}],
        "teams": [{"slug": "admins"}],
        "apps": [],
    },
}))
PY
  exit 0
fi

if [[ "${method}" == "PATCH" && "${endpoint}" == "repos/example/repo/branches/main/protection/required_status_checks" ]]; then
  if [[ "${input}" == "true" ]]; then
    python3 -c 'import os, pathlib, sys; pathlib.Path(os.environ["STATUS_PAYLOAD_FILE"]).write_text(sys.stdin.read())'
  fi
  printf '{}\n'
  exit 0
fi

if [[ "${method}" == "PUT" && "${endpoint}" == "repos/example/repo/branches/main/protection" ]]; then
  if [[ "${input}" == "true" ]]; then
    python3 -c 'import os, pathlib, sys; pathlib.Path(os.environ["BRANCH_PUT_FILE"]).write_text(sys.stdin.read())'
  fi
  printf '{}\n'
  exit 0
fi

echo "unexpected gh api call: method=${method} endpoint=${endpoint}" >&2
exit 1
GH
chmod +x "${TMP_DIR}/bin/gh"

GH_CALL_LOG="${LOG_FILE}" \
STATUS_PAYLOAD_FILE="${STATUS_PAYLOAD_FILE}" \
BRANCH_PUT_FILE="${BRANCH_PUT_FILE}" \
PATH="${TMP_DIR}/bin:${PATH}" \
  "${SCRIPT_DIR}/setup-branch-protection.sh" example/repo main > "${TMP_DIR}/script.out"

if [[ -s "${BRANCH_PUT_FILE}" ]]; then
  echo "Expected existing branch protection to be preserved without a full PUT" >&2
  exit 1
fi

if [[ ! -s "${STATUS_PAYLOAD_FILE}" ]]; then
  echo "Expected required status checks to be updated" >&2
  exit 1
fi

python3 - "${STATUS_PAYLOAD_FILE}" "${LOG_FILE}" <<'PY'
import json
import pathlib
import sys

payload = json.loads(pathlib.Path(sys.argv[1]).read_text())
expected = {
    "strict": False,
    "checks": [
        {"context": "lint", "app_id": 123},
        {"context": "security-scan"},
        {"context": "Build & Test"},
    ],
}
if payload != expected:
    raise SystemExit(f"unexpected status check payload: {payload!r}")

calls = pathlib.Path(sys.argv[2]).read_text()
if "--method PATCH" not in calls:
    raise SystemExit(f"expected status check PATCH call, got: {calls}")
PY

echo "setup-branch-protection preserves existing branch protection settings"
