run Keycloak container with admin user:
```bash
# Use Keycloak's bootstrap env vars (KC_BOOTSTRAP_ADMIN_USERNAME / KC_BOOTSTRAP_ADMIN_PASSWORD)
# Map host 8087 to container 8080
docker run --rm -it -p 8087:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:latest start-dev

```
remove all containers after stopping the Keycloak container
```bash
containers=$(docker ps -aq)
if [ -n "$containers" ]; then
  docker rm -f $containers
else
  echo "No containers to remove."
fi

```
or compact command to stop and remove all containers:
```bash
docker stop $(docker ps -q) 2>/dev/null || true && docker rm -f $(docker ps -aq) 2>/dev/null || true

```
validate docker compose file:
```bash
docker compose config
```
start Keycloak with docker compose:
```bash
# Start everything in the compose file, building images if needed
docker compose up --build -d

```
test if Keycloak is running and fetch the welcome message:
```bash
URL="http://localhost:8081/hello"
echo "Waiting for $URL to respond (up to ~60s)..."

for i in {1..30}; do
  if curl -sSf --max-time 2 "$URL" >/dev/null; then
    echo "Service is up (attempt $i). Fetching body..."
    break
  fi
  printf '.'
  sleep 2
done

# If the endpoint responded, pretty-print the JSON; otherwise show last curl output and container logs
if curl -sSf --max-time 2 "$URL" -o /tmp/hello.json; then
  if command -v jq >/dev/null 2>&1; then
    jq . /tmp/hello.json || cat /tmp/hello.json
  else
    python3 -m json.tool /tmp/hello.json || cat /tmp/hello.json
  fi
else
  echo
  echo "Endpoint did not become healthy. Showing last 200 bytes of container logs for 'app':"
  docker compose logs --no-color --tail=200 app
  echo
  echo "Last curl attempt output:"
  curl -sv --max-time 5 "$URL" || true
fi

```