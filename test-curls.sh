#!/usr/bin/env bash
# Simple test script to start docker-compose and run curl against /hello
set -euo pipefail

# Determine which compose command to use (docker-compose or docker compose)
if command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD="docker-compose"
elif command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD="docker compose"
else
  echo "Error: neither 'docker-compose' nor 'docker compose' is available on PATH. Install Docker Compose." >&2
  exit 1
fi

# Ensure docker is available
if ! command -v docker >/dev/null 2>&1; then
  echo "Error: docker is not available on PATH." >&2
  exit 1
fi

# Check if compose stack has any containers defined/created
# If none exist or any container is not running, we'll start the stack.
need_up=false
IDS=$($COMPOSE_CMD ps -q 2>/dev/null || true)
if [ -z "$IDS" ]; then
  need_up=true
else
  for id in $IDS; do
    # If inspect fails, mark for up
    running=$(docker inspect -f '{{.State.Running}}' "$id" 2>/dev/null || echo "false")
    if [ "$running" != "true" ]; then
      need_up=true
      break
    fi
  done
fi

if [ "$need_up" = true ]; then
  echo "Building and starting services (Oracle + app)..."
  $COMPOSE_CMD up -d --build
else
  echo "Compose services already created and running; skipping 'up'."
fi

# Wait for app to be healthy by polling /hello
APP_URL="http://localhost:8081/hello"
MAX_RETRIES=${MAX_RETRIES:-30}
SLEEP_SECONDS=${SLEEP_SECONDS:-2}

echo "Waiting for app ($APP_URL) to be available..."
for i in $(seq 1 "$MAX_RETRIES"); do
  http_code=$(curl -s -o /dev/null -w "%{http_code}" "$APP_URL" || echo "000")
  if [[ "$http_code" =~ ^2 ]]; then
    echo "App is up (HTTP $http_code)"
    break
  fi
  echo "Waiting... ($i/$MAX_RETRIES) status=$http_code"
  sleep "$SLEEP_SECONDS"
done

# After waiting, perform a final check and print response or logs depending on success
final_code=$(curl -s -o /dev/null -w "%{http_code}" "$APP_URL" || echo "000")
if [[ "$final_code" =~ ^2 ]]; then
  echo "Request successful: HTTP $final_code"
  echo "Response body:"
  curl -s "$APP_URL" || true
else
  echo "App did not become available or returned non-2xx (HTTP $final_code). Showing recent logs for app and oracle-db:"
  $COMPOSE_CMD logs --tail=100 app || true
  $COMPOSE_CMD logs --tail=100 oracle-db || true
  echo "Attempting a verbose request to /hello (to show headers/body):"
  curl -v "$APP_URL" || true
fi

echo "To shutdown services: $COMPOSE_CMD down -v"
