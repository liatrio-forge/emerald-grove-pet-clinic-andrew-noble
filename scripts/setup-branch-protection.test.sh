#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"

fail() {
  echo "not ok - $*" >&2
  exit 1
}

tmpdir="$(mktemp -d)"
trap 'rm -rf "${tmpdir}"' EXIT

payload_file="${tmpdir}/payload.json"
calls_file="${tmpdir}/gh-calls.log"

cat >"${tmpdir}/gh" <<'GH'
#!/usr/bin/env bash
set -euo pipefail

echo "$*" >>"${GH_CALLS_FILE}"

if [[ "$1" != "api" ]]; then
  echo "unexpected gh command: $*" >&2
  exit 1
fi

shift
method="GET"
input_file=""
path=""
jq_filter=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --method)
      method="$2"
      shift 2
      ;;
    --input)
      input_file="$2"
      shift 2
      ;;
    --jq)
      jq_filter="$2"
      shift 2
      ;;
    -H)
      shift 2
      ;;
    *)
      path="$1"
      shift
      ;;
  esac
done

case "${method}:${path}" in
  GET:repos/example/repo/branches/main/protection)
    printf '{"protected":true}\n'
    ;;
  GET:repos/example/repo/branches/main/protection/required_status_checks)
    response='{"strict":false,"checks":[{"context":"Security Scan","app_id":12345},{"context":"Lint","app_id":null}]}'
    case "${jq_filter}" in
      '.checks // []')
        printf '%s\n' "${response}" | jq '.checks // []'
        ;;
      '[.checks[].context]')
        printf '%s\n' "${response}" | jq '[.checks[].context]'
        ;;
      '{strict: .strict, contexts: [.checks[].context]}')
        printf '%s\n' "${response}" | jq '{strict: .strict, contexts: [.checks[].context]}'
        ;;
      *)
        printf '%s\n' "${response}"
        ;;
    esac
    ;;
  PATCH:repos/example/repo/branches/main/protection/required_status_checks)
    if [[ "${input_file}" == "-" ]]; then
      cp /dev/stdin "${GH_PAYLOAD_FILE}"
    else
      cp "${input_file}" "${GH_PAYLOAD_FILE}"
    fi
    ;;
  *)
    echo "unexpected gh api call: ${method}:${path}" >&2
    exit 1
    ;;
esac
GH

chmod +x "${tmpdir}/gh"

GH_PAYLOAD_FILE="${payload_file}" \
  GH_CALLS_FILE="${calls_file}" \
  PATH="${tmpdir}:${PATH}" \
  CHECK_CONTEXT="Build & Test" \
  bash "${REPO_ROOT}/scripts/setup-branch-protection.sh" "example/repo" "main" >/dev/null

[[ -s "${payload_file}" ]] || fail "expected PATCH payload to be captured"

jq -e '.checks[] | select(.context == "Security Scan" and .app_id == 12345)' "${payload_file}" >/dev/null \
  || fail "expected existing app-scoped check to keep app_id"

build_check_count="$(jq '[.checks[] | select(.context == "Build & Test")] | length' "${payload_file}")"
[[ "${build_check_count}" == "1" ]] || fail "expected one Build & Test check, got ${build_check_count}"

jq -e '.checks[] | select(.context == "Build & Test" and (has("app_id") | not))' "${payload_file}" >/dev/null \
  || fail "expected new check to be added without app_id"

echo "ok - preserves app_id while adding required check"
