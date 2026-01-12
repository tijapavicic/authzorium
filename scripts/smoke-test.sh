#!/usr/bin/env bash
# Simple smoke test for the app /hello endpoint suitable for CI
# Usage:
#   scripts/smoke-test.sh [URL]
# Environment overrides:
#   RETRIES - number of attempts (default 30)
#   DELAY   - seconds between attempts (default 2)
#   TIMEOUT - curl timeout in seconds (default 2)
#   EXPECT_STATUS - expected HTTP status code (default 200)

set -euo pipefail

DEFAULT_URL="http://localhost:8081/hello"
URL="${1:-${URL:-$DEFAULT_URL}}"
RETRIES=${RETRIES:-30}
DELAY=${DELAY:-2}
TIMEOUT=${TIMEOUT:-2}
EXPECT_STATUS=${EXPECT_STATUS:-200}

echo "Smoke-test: $URL (expect=$EXPECT_STATUS) retries=$RETRIES delay=${DELAY}s timeout=${TIMEOUT}s"

attempt=0
last_status=""
TMPBODY=""

while [ "$attempt" -lt "$RETRIES" ]; do
  attempt=$((attempt+1))
  TMPBODY=$(mktemp)
  # curl returns non-zero on some failures; capture body and status explicitly
  HTTP_STATUS=$(curl -sS -w "%{http_code}" -o "$TMPBODY" --max-time "$TIMEOUT" "$URL" || true)
  last_status="$HTTP_STATUS"
  if [ "$HTTP_STATUS" = "$EXPECT_STATUS" ]; then
    echo "\nOK: received HTTP $HTTP_STATUS on attempt $attempt"
    if command -v jq >/dev/null 2>&1; then
      echo "Response body:" && jq . "$TMPBODY" || cat "$TMPBODY"
    else
      # try python pretty print, fallback to raw
      python3 -m json.tool "$TMPBODY" 2>/dev/null || cat "$TMPBODY"
    fi
    rm -f "$TMPBODY"
    exit 0
  fi
  # not ready yet
  printf '.'
  sleep "$DELAY"
  rm -f "$TMPBODY"
done

echo "\nFAIL: did not receive HTTP $EXPECT_STATUS after $RETRIES attempts (last_status=$last_status)"
# try to show some helpful debugging info
if command -v docker >/dev/null 2>&1; then
  echo "\n--- docker compose logs (tail 200) for 'app' ---"
  docker compose logs --no-color --tail=200 app || true
  echo "\n--- docker compose ps ---"
  docker compose ps || true
fi

exit 1

