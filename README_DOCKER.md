# Dockerized Development Environment

This document explains how to run the application together with a Dockerized Oracle XE database on your local machine. It includes quick commands you can copy/paste, a small test script, and troubleshooting tips.

Prerequisites
-------------
- Docker and Docker Compose installed and available on your PATH.
- Java 17 and Maven (for local builds; optional when using the provided Dockerfile which builds the app image).
- (Optional) Oracle JDBC driver if your environment cannot download it from a configured Maven repo.

# Verify your Docker setup
```bash

docker manifest inspect quay.io/keycloak/keycloak:latest
docker manifest inspect postgres:14
docker manifest inspect gvenzl/oracle-xe:18.4.0

docker version
env | grep -i DOCKER_DEFAULT_PLATFORM || echo "DOCKER_DEFAULT_PLATFORM not set"

```
Files of interest
-----------------
- `docker-compose.yml` — starts two services: `oracle-db` (Oracle XE) and `app` (your Spring Boot service).
- `Dockerfile` — multi-stage build that packages the Spring Boot app into a runnable image.
- `test-curls.sh` — convenience script that builds/starts the compose stack and polls the `/hello` endpoint.
- `src/main/resources/application-oracle.properties` — Spring profile properties for the Oracle datasource.

Quick start (recommended)
-------------------------
1. Make the test script executable (from the project root):

    ```bash
    chmod +x ./test-curls.sh
    ```

2. Start the services (build images if needed):

    ```bash
    docker-compose up -d --build
    ```

    This command will:
    - Pull the Oracle XE image (gvenzl/oracle-xe:18.4.0) if you don't have it.
    - Build the app image from the local `Dockerfile` (it runs `mvn -DskipTests package` during the image build).
    - Start both containers and wait for the Oracle container healthcheck.

3. Run the quick test script which will poll the app and issue a curl to `/hello`:

    ```bash
    chmod +x ./test-curls.sh
   
    ./test-curls.sh
    ```

    If you prefer to run curl manually (once services are up):

    ```bash
    # App is mapped to host port 8081 by compose; /hello is permitted without auth for testing
    curl -v http://localhost:8081/hello
    ```

Run the app locally without Docker (optional)
--------------------------------------------
If you want to run the app on your host (without the Docker app image) but still connect to the Oracle container started by docker-compose:

1. Start only the Oracle container:

    ```bash
    docker-compose up -d oracle-db
    ```
   1.1. validate Oracle is running and healthy:

    ```bash
    docker-compose ps
    nc -zv localhost 1521 || echo "Oracle not reachable"
   
    ```

2. Build the app locally and run it with the `oracle` profile:

    ```bash
    mvn -DskipTests clean package
    java -jar target/authzorium-1.0-SNAPSHOT.jar --spring.profiles.active=oracle
    ```

    The application will read connection settings from `src/main/resources/application-oracle.properties` (default username `system`, password `loket_pwd`, URL `jdbc:oracle:thin:@localhost:1521:XE`).

Installing the Oracle JDBC driver (if needed)
---------------------------------------------
Many Oracle JDBC artifacts are not in Maven Central. If your build fails to download `ojdbc8`, you have two options:

1) Install the JDBC jar into your local Maven repository manually:

    ```bash
    mvn install:install-file -Dfile=/path/to/ojdbc8.jar -DgroupId=com.oracle.database.jdbc \
      -DartifactId=ojdbc8 -Dversion=19.8.0.0 -Dpackaging=jar
    ```

2) Or add an Oracle Maven repository (if available to you) to `pom.xml`.

Stopping and cleaning up
------------------------
To stop and remove containers and the Oracle data volume:

```bash
docker-compose down -v
```

Logs and debugging
------------------
- View compose logs for both services:

    ```bash
    docker-compose logs -f
    ```

- View only the app logs:

    ```bash
    docker-compose logs -f app
    ```

- If the app container fails to start, check its logs and the Oracle logs. Oracle images need some startup time; the compose file uses a healthcheck and the app depends on the DB being healthy.

Healthcheck & wait
------------------
- The Oracle image used provides a healthcheck which the compose config depends on. If the app cannot connect to Oracle right away, wait a minute and retry requests.
- If you see connection errors like `ORA-xxxxx` or `Communications link failure`, ensure:
  - Oracle container is healthy (`docker-compose ps` shows `healthy`).
  - You used the same credentials `system` / `loket_pwd` (configured in the compose file).

Security note for local testing
-------------------------------
- For convenience `/hello` and the H2 console (`/h2-console`) are permitted without JWT authentication in this development setup. Do NOT enable these in production.
- The application is still configured as a JWT resource server for all other endpoints; adapt security for your environment.

Advanced: Testcontainers (CI-friendly)
--------------------------------------
If you prefer integration tests that spin up a fresh database for CI, consider switching repository-level tests to Testcontainers instead of relying on docker-compose. I can add Testcontainers-based tests if you want CI-friendly automation.

Common troubleshooting checklist
-------------------------------
- Docker not found: install Docker Desktop (macOS/Windows) or Docker Engine (Linux).
- Port conflicts: if host port 8082 is in use, note the app is published on host port 8081; change mappings in `docker-compose.yml` if needed.
- Oracle driver download errors: install `ojdbc8.jar` locally as shown above.
- App can't connect to DB: check `docker-compose logs oracle-db` and wait until the healthcheck passes.

If you want, I can:
- Add the Oracle JDBC jar into the Docker image automatically (copy from a `lib/` directory) so you don't need to install the driver locally, or
- Add Testcontainers-based integration tests for CI.

Enjoy — run the `./test-curls.sh` script to exercise the local stack quickly.
