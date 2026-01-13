.PHONY: up-h2 down-h2

# Start the project in H2/dev mode (skip Oracle)
up-h2: ; docker compose -f docker-compose.yml -f docker-compose.skip-oracle.yml up -d

# Stop and remove containers and named volumes
down-h2: ; docker compose down -v

# run keycloak-h2: up-h2 ;
# make -n up-h2 ; echo '--- docker ps keycloak ---' ; docker ps --filter name=loket-keycloak --format "{{.Names}}\t{{.Status}}\t{{.Ports}}" ; echo '--- curl probe ---' ; curl -I --max-time 5 http://localhost:8087/ || true

#  docker inspect --format 'Name: {{.Name}}\nState: {{.State.Status}} (ExitCode={{.State.ExitCode}})\nEntrypoint: {{json .Config.Entrypoint}}\nCmd: {{json .Config.Cmd}}\nImage: {{.Config.Image}}\nHostConfig: {{json .HostConfig}}\nEnvironment:\n{{range .Config.Env}}{{println .}}{{end}}' loket-keycloak || docker ps -a --filter name=loket-keycloak --format 'container not found: {{.Names}} {{.Status}}'
