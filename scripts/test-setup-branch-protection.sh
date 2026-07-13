#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPT_UNDER_TEST="${SCRIPT_DIR}/setup-branch-protection.sh"

run_with_stubbed_gh() {
  local payload_file="$1"
  local temp_dir
  temp_dir="$(mktemp -d)"

  cat >"${temp_dir}/gh" <<'GH'
#!/usr/bin/env bash
set -euo pipefail

if [[ "${1:-}" != "api" ]]; then
  echo "unexpected gh command: $*" >&2
  exit 1
fi
shift

if [[ "${1:-}" == "--method" && "${2:-}" == "PATCH" ]]; then
  cat >"${PAYLOAD_FILE}"
  exit 0
fi

endpoint="${1:-}"
jq_filter=""
while (($#)); do
  if [[ "${1}" == "--jq" ]]; then
    jq_filter="${2:-}"
    break
  fi
  shift
done

case "${endpoint}" in
  repos/example/repo/branches/main/protection)
    printf '%s\n' '{"required_status_checks":{"strict":false,"checks":[{"context":"E2E"},{"context":"Lint"}]}}'
    ;;
  repos/example/repo/branches/main/protection/required_status_checks)
    if [[ "${jq_filter}" == "[.checks[].context]" ]]; then
      exit 42
    fi
    printf '%s\n' '{"strict":false,"checks":[{"context":"E2E"},{"context":"Lint"},{"context":"Build & Test"}]}'
    ;;
  *)
    echo "unexpected endpoint: ${endpoint}" >&2
    exit 1
    ;;
esac
GH
  chmod +x "${temp_dir}/gh"

  PATH="${temp_dir}:${PATH}" PAYLOAD_FILE="${payload_file}" "${SCRIPT_UNDER_TEST}" example/repo main >/dev/null
}

test_preserves_existing_checks_when_dedicated_status_read_fails() {
  local temp_dir payload_file
  temp_dir="$(mktemp -d)"
  payload_file="${temp_dir}/payload.json"

  run_with_stubbed_gh "${payload_file}"

  jq -e '([.checks[].context] | index("E2E") != null and index("Lint") != null and index("Build & Test") != null)' "${payload_file}" >/dev/null
}

test_preserves_existing_strict_mode() {
  local temp_dir payload_file
  temp_dir="$(mktemp -d)"
  payload_file="${temp_dir}/payload.json"

  run_with_stubbed_gh "${payload_file}"

  jq -e '.strict == false' "${payload_file}" >/dev/null
}

test_preserves_existing_checks_when_dedicated_status_read_fails
test_preserves_existing_strict_mode
