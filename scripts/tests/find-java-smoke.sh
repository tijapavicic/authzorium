#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "$0")/.." && pwd)
FIND_SCRIPT="$ROOT_DIR/find-java.sh"

if [[ ! -x "$FIND_SCRIPT" ]]; then
  echo "Make sure $FIND_SCRIPT is executable" >&2
  exit 2
fi

echo "Running smoke tests for $FIND_SCRIPT"

# Start a background process with argv0 set to 'java'
# Use bash -c 'exec -a java sleep 300' which sets argv0 if bash supports exec -a
bash -c 'exec -a java sleep 300' &
bg_pid=$!
sleep 0.3

cleanup() {
  echo "Cleaning up..."
  kill "$bg_pid" 2>/dev/null || true
  wait "$bg_pid" 2>/dev/null || true
}
trap cleanup EXIT

# Test 1: basic detection
if "$FIND_SCRIPT" >/dev/null 2>&1; then
  echo "Test 1: detected java process - PASS"
else
  echo "Test 1: expected to detect java process - FAIL" >&2
  exit 1
fi

# Test 2: pattern that matches should pass
if "$FIND_SCRIPT" --pattern "sleep" >/dev/null 2>&1; then
  echo "Test 2: pattern match 'sleep' - PASS"
else
  echo "Test 2: expected pattern 'sleep' to match - FAIL" >&2
  exit 1
fi

# Test 3: pattern that doesn't match should fail (exit 1)
if "$FIND_SCRIPT" --pattern "SOME_UNLIKELY_TOKEN_123" >/dev/null 2>&1; then
  echo "Test 3: unexpected match for unlikely token - FAIL" >&2
  exit 1
else
  echo "Test 3: no match for unlikely token - PASS"
fi

# All done
exit 0

