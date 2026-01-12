.PHONY: up-h2 down-h2

# Start the project in H2/dev mode (skip Oracle)
up-h2: ; docker compose -f docker-compose.yml -f docker-compose.skip-oracle.yml up --build -d

# Stop and remove containers and named volumes
down-h2: ; docker compose down -v

