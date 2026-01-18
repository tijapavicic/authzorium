.PHONY: up-h2 down-h2 keycloak-up keycloak-down

# Start the project in H2/dev mode (skip Oracle). Do not build images from the terminal (--no-build)
up-h2: ; docker compose -f docker-compose.yml -f docker-compose.skip-oracle.yml up --no-build -d

# Stop and remove containers and named volumes
down-h2: ; docker compose down -v

# Start only Keycloak (and its Postgres DB) using .env for bootstrap credentials; do not build images
keycloak-up: ; docker compose --env-file .env -f docker-compose.yml -f docker-compose.skip-oracle.yml up --no-build -d keycloak-db keycloak

# Stop and remove Keycloak and its DB (stops, then removes containers)
keycloak-down: ; docker compose --env-file .env -f docker-compose.yml -f docker-compose.skip-oracle.yml stop keycloak keycloak-db && docker compose --env-file .env -f docker-compose.yml -f docker-compose.skip-oracle.yml rm -f keycloak keycloak-db

# make -n  keycloak-up ; echo '--- now executing make target ---' ; make keycloak-up ; echo '--- wait 8s ---' ; sleep 8 ; docker ps --filter name=loket-keycloak --format "{{.Names}}\t{{.Status}}\t{{.Ports}}" ; docker logs --tail 80 loket-keycloak 2>&1 | sed -n '1,200p' ; curl -I --max-time 10 http://localhost:8087/ || true
# run keycloak-h2: up-h2 ;
# make -n up-h2 ; echo '--- docker ps keycloak ---' ; docker ps --filter name=loket-keycloak --format "{{.Names}}\t{{.Status}}\t{{.Ports}}" ; echo '--- curl probe ---' ; curl -I --max-time 5 http://localhost:8087/ || true

#  docker inspect --format 'Name: {{.Name}}\nState: {{.State.Status}} (ExitCode={{.State.ExitCode}})\nEntrypoint: {{json .Config.Entrypoint}}\nCmd: {{json .Config.Cmd}}\nImage: {{.Config.Image}}\nHostConfig: {{json .HostConfig}}\nEnvironment:\n{{range .Config.Env}}{{println .}}{{end}}' loket-keycloak || docker ps -a --filter name=loket-keycloak --format 'container not found: {{.Names}} {{.Status}}'

# make -n keycloak-up