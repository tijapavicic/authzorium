#!/usr/bin/env bash
set -euo pipefail

# k8s-cheatsheet.sh
# Project-specific Kubernetes cheatsheet for authzorium
# By default the script prints the commands (dry-run). Pass --exec to actually run them.

# Usage examples:
#  - Print available actions:
#      ./scripts/k8s-cheatsheet.sh help
#  - Print commands for creating a Kind cluster and building image (dry-run):
#      ./scripts/k8s-cheatsheet.sh create-cluster build-image
#  - Run create cluster, build image and apply tiny-dev overlay (execute commands):
#      ./scripts/k8s-cheatsheet.sh --exec create-cluster build-image apply-tiny-dev

# bash -n scripts/k8s-cheatsheet.sh && echo 'syntax OK' || echo 'syntax ERROR'

# Project defaults
CLUSTER_NAME="loket-cluster"
IMAGE_NAME="authzorium:latest"
NAMESPACE="loket"
KUSTOMIZE_DIR="k8s"
OVERLAY_SKIP="k8s/overlays/skip-oracle"
OVERLAY_TINY="k8s/overlays/tiny-dev"

EXEC=0

# Logging: write executed command output to a timestamped logfile when --exec is used
LOG_DIR="scripts/logs"
mkdir -p "$LOG_DIR"
TIMESTAMP=$(date +%Y%m%dT%H%M%S)
LOGFILE="$LOG_DIR/k8s-cheatsheet-${TIMESTAMP}.log"

# Timeouts and smoke-test expectations (defaults)
WAIT_TIMEOUT="300s"         # used for kubectl rollout --timeout
SMOKE_TIMEOUT=10             # seconds for curl
SMOKE_EXPECT_STATUS=0        # 0 means don't assert, otherwise numeric status code
SMOKE_EXPECT_BODY=""       # substring to look for in response body (empty => no check)
SMOKE_RETRY=3                # number of attempts for smoke-test
SMOKE_RETRY_DELAY=1         # seconds between smoke-test retries
SMOKE_RETRY_BACKOFF=1.0     # multiplier for retry delay (exponential backoff), 1.0 = no backoff
# JSONPath / jq options
SMOKE_JSONPATH=""          # e.g. $.data.id
SMOKE_JSONPATH_VALUE=""    # expected exact value for the JSONPath expression
SMOKE_JSONPATH_CONTAINS="" # expected substring in JSONPath-selected value

# internal: run or echo
run_cmd() {
  if [ "$EXEC" -eq 1 ]; then
    # Ensure logfile exists
    touch "$LOGFILE"
    # Log the command
    echo "+ $*" | tee -a "$LOGFILE"
    # Execute the command, append stdout/stderr to logfile, and return the exit code
    # Use bash -c to preserve complex commands
    bash -c "$*" 2>&1 | tee -a "$LOGFILE"
    rc=${PIPESTATUS[0]:-0}
    if [ "$rc" -ne 0 ]; then
      echo "[k8s-cheatsheet] Command failed with exit code $rc" | tee -a "$LOGFILE"
    fi
    return $rc
  else
    echo "DRY: $*"
  fi
}

print_header() {
  echo "\n=== k8s cheatsheet for authzorium ===\n"
}

print_help() {
  print_header
  cat <<EOF
Available actions (pass one or more actions):
  help                   Show this help
  create-cluster         Create a local Kind cluster (installs kind via brew if needed)
  build-image            Build Docker image: ${IMAGE_NAME}
  load-image             Load Docker image into kind cluster
  apply-full             Apply full k8s manifest (Oracle + app): kubectl apply -k ${KUSTOMIZE_DIR}
  apply-skip-oracle      Apply overlay that deploys app with H2 (no Oracle): kubectl apply -k ${OVERLAY_SKIP}
  apply-tiny-dev         Apply tiny overlay (H2, tiny resources, no probes): kubectl apply -k ${OVERLAY_TINY}
  render-skip-oracle     Print final YAML for the skip-oracle overlay
  render-tiny-dev        Print final YAML for the tiny-dev overlay
  wait-rollout           Wait for authzorium rollout and show status (uses --wait-timeout)
  logs                   Tail logs for authzorium pods
  port-forward           Port-forward service authzorium 8082:8082
  status                 Show pods/services in namespace ${NAMESPACE}
  delete-all             Delete namespace ${NAMESPACE} (removes all resources)
  smoke-test             Perform a simple HTTP GET /hello against the service (via port-forward)

Flags (global, can be passed before actions):
  --exec                 Execute commands (default: dry-run; the script will only PRINT commands)
  --wait-timeout DURATION How long to wait for rollout (kubectl --timeout), e.g. 120s (default: ${WAIT_TIMEOUT})
  --smoke-timeout SEC    How many seconds curl will wait (default: ${SMOKE_TIMEOUT})
  --smoke-http-status N  Expect HTTP status N from /hello (if specified)
  --smoke-http-body STR  Expect response body to contain STR (if specified)

When --exec is used, command output is appended to a logfile in ${LOG_DIR} (example: ${LOGFILE}).

Examples:
  # Dry-run (print commands)
  ./scripts/k8s-cheatsheet.sh create-cluster build-image apply-tiny-dev

  # Execute and wait for rollout, then smoke-test and tail logs
  ./scripts/k8s-cheatsheet.sh --exec --wait-timeout 180s --smoke-timeout 8 --smoke-http-status 200 create-cluster build-image load-image apply-tiny-dev wait-rollout smoke-test logs

EOF
}

# Actions
action_create_cluster() {
  run_cmd "command -v kind >/dev/null 2>&1 || (command -v brew >/dev/null 2>&1 && brew install kind) || echo 'Please install kind manually'"
  run_cmd "kind create cluster --name ${CLUSTER_NAME} || echo 'cluster may already exist'"
  run_cmd "kubectl cluster-info"
}

action_build_image() {
  run_cmd "docker build -t ${IMAGE_NAME} ."
}

action_load_image() {
  run_cmd "kind load docker-image ${IMAGE_NAME} --name ${CLUSTER_NAME}"
}

action_apply_full() {
  run_cmd "kubectl apply -k ${KUSTOMIZE_DIR}"
}

action_apply_skip_oracle() {
  run_cmd "kubectl apply -k ${OVERLAY_SKIP}"
}

action_apply_tiny_dev() {
  run_cmd "kubectl apply -k ${OVERLAY_TINY}"
}

action_render_skip_oracle() {
  run_cmd "kubectl kustomize ${OVERLAY_SKIP}"
}

action_render_tiny_dev() {
  run_cmd "kubectl kustomize ${OVERLAY_TINY}"
}

action_wait_rollout() {
  # Use the configured WAIT_TIMEOUT
  run_cmd "kubectl rollout status deployment/authzorium -n ${NAMESPACE} --watch --timeout=${WAIT_TIMEOUT}"
}

# New action: smoke test the /hello endpoint via port-forward
action_smoke_test() {
  PORT=8082
  PATH_CHECK="/hello"

  if [ "$EXEC" -ne 1 ]; then
    echo "DRY: Would port-forward svc/authzorium ${PORT}->${PORT} and curl http://localhost:${PORT}${PATH_CHECK}"
    return 0
  fi

  echo "+ kubectl port-forward svc/authzorium ${PORT}:${PORT} -n ${NAMESPACE} &" | tee -a "$LOGFILE"
  kubectl port-forward svc/authzorium ${PORT}:${PORT} -n ${NAMESPACE} >>"$LOGFILE" 2>&1 &
  PF_PID=$!
  sleep 2

  RESP_FILE=$(mktemp)
  ATTEMPT=1
  while [ $ATTEMPT -le "$SMOKE_RETRY" ]; do
    echo "+ curl -s -m ${SMOKE_TIMEOUT} -o ${RESP_FILE} -w '%{http_code}' http://localhost:${PORT}${PATH_CHECK} (attempt ${ATTEMPT}/${SMOKE_RETRY})" | tee -a "$LOGFILE"
    HTTP_CODE=$(curl -s -m ${SMOKE_TIMEOUT} -o "$RESP_FILE" -w '%{http_code}' "http://localhost:${PORT}${PATH_CHECK}" 2>&1 | tee -a "$LOGFILE")
    CURL_RC=${PIPESTATUS[0]:-0}

    if [ "$CURL_RC" -ne 0 ]; then
      echo "[k8s-cheatsheet] smoke-test attempt ${ATTEMPT} curl failed (rc=${CURL_RC})" | tee -a "$LOGFILE"
      RESULT=2
    else
      echo "[k8s-cheatsheet] smoke-test attempt ${ATTEMPT}: HTTP ${HTTP_CODE}" | tee -a "$LOGFILE"
      RESULT=0
      # status assertion
      if [ "${SMOKE_EXPECT_STATUS:-0}" -ne 0 ] && [ "${HTTP_CODE}" -ne "${SMOKE_EXPECT_STATUS}" ]; then
        echo "[k8s-cheatsheet] expected status ${SMOKE_EXPECT_STATUS} but got ${HTTP_CODE}" | tee -a "$LOGFILE"
        RESULT=2
      fi
      # body assertion
      if [ -n "${SMOKE_EXPECT_BODY}" ]; then
        if grep -qF "${SMOKE_EXPECT_BODY}" "$RESP_FILE"; then
          echo "[k8s-cheatsheet] body contains expected substring" | tee -a "$LOGFILE"
        else
          echo "[k8s-cheatsheet] body did not contain expected substring" | tee -a "$LOGFILE"
          RESULT=2
        fi
      fi
      # JSONPath assertion (if requested)
      if [ -n "$SMOKE_JSONPATH" ]; then
        JQ_FILTER=$(translate_jsonpath_to_jq "$SMOKE_JSONPATH")
        # attempt to extract value via jq
        JP_VAL=$(jq -r "$JQ_FILTER // empty" "$RESP_FILE" 2>>"$LOGFILE" || echo "")
        if [ -z "$JP_VAL" ]; then
          echo "[k8s-cheatsheet] JSONPath check: no value extracted for $SMOKE_JSONPATH (jq filter: $JQ_FILTER)" | tee -a "$LOGFILE"
          RESULT=2
        else
          echo "[k8s-cheatsheet] JSONPath value: $JP_VAL" | tee -a "$LOGFILE"
          if [ -n "$SMOKE_JSONPATH_VALUE" ] && [ "$JP_VAL" != "$SMOKE_JSONPATH_VALUE" ]; then
            echo "[k8s-cheatsheet] JSONPath value mismatch: expected '$SMOKE_JSONPATH_VALUE' got '$JP_VAL'" | tee -a "$LOGFILE"
            RESULT=2
          fi
          if [ -n "$SMOKE_JSONPATH_CONTAINS" ] && ! echo "$JP_VAL" | grep -qF "$SMOKE_JSONPATH_CONTAINS"; then
            echo "[k8s-cheatsheet] JSONPath value did not contain expected substring '$SMOKE_JSONPATH_CONTAINS'" | tee -a "$LOGFILE"
            RESULT=2
          fi
        fi
      fi
    fi

    if [ "$RESULT" -eq 0 ]; then
      echo "[k8s-cheatsheet] smoke-test succeeded on attempt ${ATTEMPT}" | tee -a "$LOGFILE"
      break
    fi

    # if not last attempt, wait and retry
    if [ $ATTEMPT -lt "$SMOKE_RETRY" ]; then
      echo "[k8s-cheatsheet] retrying in ${SMOKE_RETRY_DELAY}s..." | tee -a "$LOGFILE"
      sleep "$SMOKE_RETRY_DELAY"
      # apply exponential backoff multiplier if > 1.0
      if awk "BEGIN{exit !($SMOKE_RETRY_BACKOFF > 1.0)}"; then
        NEW_DELAY=$(awk -v d="$SMOKE_RETRY_DELAY" -v m="$SMOKE_RETRY_BACKOFF" 'BEGIN{printf "%d", (d * m + 0.5)}')
        echo "[k8s-cheatsheet] increasing retry delay from ${SMOKE_RETRY_DELAY}s to ${NEW_DELAY}s (backoff=${SMOKE_RETRY_BACKOFF})" | tee -a "$LOGFILE"
        SMOKE_RETRY_DELAY=$NEW_DELAY
      fi
    fi
    ATTEMPT=$((ATTEMPT+1))
  done

  echo "+ kill ${PF_PID}" | tee -a "$LOGFILE"
  kill ${PF_PID} >/dev/null 2>&1 || true
  wait ${PF_PID} 2>/dev/null || true
  rm -f "$RESP_FILE" || true

  return $RESULT
}

action_logs() {
  run_cmd "kubectl logs -n ${NAMESPACE} -l app=authzorium -f --tail=200"
}

action_port_forward() {
  run_cmd "kubectl port-forward svc/authzorium 8082:8082 -n ${NAMESPACE}"
}

action_status() {
  run_cmd "kubectl get ns ${NAMESPACE} --ignore-not-found"
  run_cmd "kubectl get pods -n ${NAMESPACE} -o wide"
  run_cmd "kubectl get svc -n ${NAMESPACE}"
}

action_delete_all() {
  echo "About to delete namespace ${NAMESPACE} and all resources in it. This is destructive. Continue? (y/N)"
  read -r confirm
  if [ "${confirm}" != "y" ]; then
    echo "Aborted deletion."
    return 0
  fi
  run_cmd "kubectl delete namespace ${NAMESPACE} --ignore-not-found"
}

# Validate kubectl-style duration strings (e.g., 30s, 5m, 1h)
is_valid_kubectl_duration() {
  case "$1" in
    ''|*[!0-9smh]*) return 1 ;;
    *) ;;
  esac
  if [[ "$1" =~ ^[0-9]+(s|m|h)$ ]]; then
    return 0
  fi
  return 1
}

# Translate a simple JSONPath expression (like $.a.b or $['a'][0].b) to a jq filter (.a.b[0].b)
# This is a limited translator covering common cases.
translate_jsonpath_to_jq() {
  local jp="$1"
  # remove leading $ if present
  jp="${jp#\$}"
  # replace ["key"] and ['key'] with .key
  local tmp
  tmp=$(echo "$jp" | sed -e "s/\[\'\([^']*\)\'\]/.\1/g" -e 's/\["\([^"]*\)"\]/.\1/g')
  # convert [*] to [] for arrays (JSONPath wildcard -> jq array iterator)
  tmp=$(echo "$tmp" | sed -e 's/\[\*\]/\[\]/g')
  # ensure leading dot
  if [[ "$tmp" != .* ]]; then
    tmp=".$tmp"
  fi
  # collapse repeated dots
  echo "$tmp" | sed -e 's/\..\././g'
}

# Parse --exec
ARGS=()
while [ "$#" -gt 0 ]; do
  case "$1" in
    --exec)
      EXEC=1; shift;;
    --wait-timeout)
      WAIT_TIMEOUT="$2"; shift 2;;
    --smoke-timeout)
      SMOKE_TIMEOUT="$2"; shift 2;;
    --smoke-http-status)
      SMOKE_EXPECT_STATUS="$2"; shift 2;;
    --smoke-http-body)
      SMOKE_EXPECT_BODY="$2"; shift 2;;
    --smoke-retry)
      SMOKE_RETRY="$2"; shift 2;;
    --smoke-retry-delay)
      SMOKE_RETRY_DELAY="$2"; shift 2;;
    --smoke-retry-backoff)
      SMOKE_RETRY_BACKOFF="$2"; shift 2;;
    --smoke-jsonpath)
      SMOKE_JSONPATH="$2"; shift 2;;
    --smoke-jsonpath-value)
      SMOKE_JSONPATH_VALUE="$2"; shift 2;;
    --smoke-jsonpath-contains)
      SMOKE_JSONPATH_CONTAINS="$2"; shift 2;;
    help|--help|-h)
      print_help; exit 0;;
    *)
      ARGS+=("$1"); shift;;
  esac
done

# Validate timeout and retry parameters
if ! is_valid_kubectl_duration "$WAIT_TIMEOUT"; then
  echo "Invalid --wait-timeout value: '$WAIT_TIMEOUT'. Use a kubectl duration like 30s, 5m, or 1h." >&2
  exit 2
fi

if ! [[ "$SMOKE_TIMEOUT" =~ ^[0-9]+$ ]] || [ "$SMOKE_TIMEOUT" -le 0 ]; then
  echo "Invalid --smoke-timeout value: '$SMOKE_TIMEOUT'. Must be a positive integer (seconds)." >&2
  exit 2
fi

if ! [[ "$SMOKE_RETRY" =~ ^[0-9]+$ ]] || [ "$SMOKE_RETRY" -le 0 ]; then
  echo "Invalid --smoke-retry value: '$SMOKE_RETRY'. Must be a positive integer." >&2
  exit 2
fi

if ! [[ "$SMOKE_RETRY_DELAY" =~ ^[0-9]+$ ]] || [ "$SMOKE_RETRY_DELAY" -lt 0 ]; then
  echo "Invalid --smoke-retry-delay value: '$SMOKE_RETRY_DELAY'. Must be a non-negative integer (seconds)." >&2
  exit 2
fi

# Validate backoff multiplier: must be a number >= 1.0
if ! echo "$SMOKE_RETRY_BACKOFF" | grep -E -q '^[0-9]+(\.[0-9]+)?$'; then
  echo "Invalid --smoke-retry-backoff value: '$SMOKE_RETRY_BACKOFF'. Must be a number (e.g. 1 or 2.0) >= 1.0" >&2
  exit 2
fi
if ! awk -v v="$SMOKE_RETRY_BACKOFF" 'BEGIN{ if (v+0 >= 1) exit 0; else exit 1 }'; then
  echo "Invalid --smoke-retry-backoff value: '$SMOKE_RETRY_BACKOFF'. Must be >= 1.0" >&2
  exit 2
fi

# If JSONPath options are used, ensure jq is available when executing
if [ -n "$SMOKE_JSONPATH" ] && [ "$EXEC" -eq 1 ]; then
  if ! command -v jq >/dev/null 2>&1; then
    echo "JSONPath checks require 'jq' to be installed on the system when using --exec. Please install jq." >&2
    exit 2
  fi
fi

# Execute requested actions
for act in "${ARGS[@]}"; do
  case "$act" in
    create-cluster) action_create_cluster ;;
    build-image) action_build_image ;;
    load-image) action_load_image ;;
    apply-full) action_apply_full ;;
    apply-skip-oracle) action_apply_skip_oracle ;;
    apply-tiny-dev) action_apply_tiny_dev ;;
    render-skip-oracle) action_render_skip_oracle ;;
    render-tiny-dev) action_render_tiny_dev ;;
    wait-rollout) action_wait_rollout ;;
    smoke-test) action_smoke_test ;;
    logs) action_logs ;;
    port-forward) action_port_forward ;;
    status) action_status ;;
    delete-all) action_delete_all ;;
    *) echo "Unknown action: $act"; print_help; exit 2 ;;
  esac
done

exit 0

