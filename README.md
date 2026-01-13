authzorium — developer-friendly README

<!-- Coverage & CI badges: replace OWNER/REPO with your GitHub org/repo. If you use Codecov and Coveralls configure repo secrets as described in the CI section. -->
[![CI](https://github.com/OWNER/REPO/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/OWNER/REPO/actions/workflows/ci.yml)
[![Codecov](https://codecov.io/gh/OWNER/REPO/branch/main/graph/badge.svg)](https://codecov.io/gh/OWNER/REPO)
[![Coveralls](https://coveralls.io/repos/github/OWNER/REPO/badge.svg?branch=main)](https://coveralls.io/github/OWNER/REPO)

<!-- Tech badges: Java, Spring Boot, Docker -->
[![Java](https://img.shields.io/badge/Java-17-blue?logo=java&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.6-brightgreen?logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-enabled-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)

What this repo is
-----------------
A tiny Java (Maven) starter project that contains a simple `AuthZoriumApplication` class in `org.authzorium.AuthZoriumApplication`. 
It uses Java 17 and includes a dependency on Spring Security SAML2 (declared in the project's `pom.xml`). 
This README focuses on fast, practical developer workflows: build, run, iterate, and troubleshoot.

Quick checklist (what you'll do)
-------------------------------
- Install prerequisites (JDK 17, Maven)
- Build the project
- Run the app locally (two simple options)
- Tips for IDE, debugging, and common Maven commands

Prerequisites
-------------
- JDK 17+ (verify with `java -version`)
- Apache Maven 3.6+ (verify with `mvn -v`)

If you use macOS, using Homebrew is the quickest:

```bash
# Install OpenJDK 17 and Maven via Homebrew
brew install openjdk@17 maven
# If needed, add JAVA_HOME to your shell (zsh):
export JAVA_HOME="$(/usr/libexec/java_home -v17)"
```

Build (fast)
------------
```bash
mvn -DskipTests clean package
```
Run (basic)
-----------
```bash
mvn -Dspring-boot.run.profiles=localh2 -DskipTests spring-boot:run
```

This produces compiled classes in `target/classes` and a JAR under `target/` (the produced JAR isn't an "uber/fat" jar by default).

Run (option A) — run the compiled class directly
-----------------------------------------------
This is the fastest way during development (no JAR packaging necessary):

```bash
# after mvn package
java -cp target/classes org.authzorium.AuthZoriumApplicationZoriumApplication
```

Run (option B) — use the Maven Exec plugin (single command)
-----------------------------------------------------------
If you prefer running via Maven without changing the POM, use the plugin coordinates on the CLI:

```bash
mvn org.codehaus.mojo:exec-maven-plugin:3.1.0:java -Dexec.mainClass="org.authzorium.AuthZoriumApplication"
```

Run (option C) — run the JAR (if you later create an executable jar)
-------------------------------------------------------------------
If you build a runnable JAR (for example by adding a shade/assembly plugin), run:

```bash
java -jar target/authzorium-1.0-SNAPSHOT.jar
```

Tests
-----
Run tests (if/when added):

```bash
mvn test
```

Dev workflow tips (quick wins)
-----------------------------
- Iteration without packaging: edit code, then `mvn -DskipTests compile` and run with `java -cp target/classes org.authzorium.AuthZoriumApplication`.
- See the dependency tree when debugging transitive deps:
  `mvn dependency:tree`
- To run a single integration-style class from Maven, prefer the Exec plugin (Option B).
- Want a reproducible, single-file artifact? Add the Maven Shade plugin to produce an "uber-jar".

IDE Tips
--------
- IntelliJ IDEA: Open the project root (it will detect the Maven project). Run the `AuthZoriumApplication` class from the gutter or create a Run Configuration for `org.authzorium.AuthZoriumApplication`.
- VS Code: Install the Java Extension Pack and use the Run/Debug panel.

Debugging
---------
To attach a remote debugger when running from the command line, start the JVM with remote debug flags:

```bash
# suspend=n means the JVM won't wait for the debugger; change to 'y' if you want it to wait
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -cp target/classes org.authzorium.AuthZoriumApplicationZoriumApplication
```
Then attach your IDE to localhost:5005.

Common issues & troubleshooting
-------------------------------
- "java: command not found" — check `JAVA_HOME` and your PATH.
- Dependency resolution issues — try `mvn -U clean package` to force updates, and verify your `repositories` section in `pom.xml`.
- If the app doesn't start as expected, try running the `Main` class directly from your IDE to get better stack traces and quick restarts.

Extending this README (next steps)
----------------------------------
- Add a runnable/fat JAR step (Maven Shade) if you want a single-distributable artifact.
- Add unit tests with JUnit and an example test-run `mvn test` entry.
- Add a small Makefile or wrapper scripts for common commands (build/run/test).

Dockerized Oracle for local development
--------------------------------------
This project includes a Docker Compose file that runs an Oracle XE container you can use for integration testing or local development.

Files added:
- `docker-compose.yml` — starts `gvenzl/oracle-xe:18.4.0` on port 1521 and exposes the optional web console on 8082.
- `src/main/resources/application-oracle.properties` — Spring profile to use Oracle as the datasource.

How to start Oracle with Docker Compose

```bash
#docker buildx imagetools inspect quay.io/keycloak/keycloak:latest
docker compose run --service-ports keycloak start-dev
# from repo root
docker-compose up -d
docker compose -f docker-compose.yml -f docker-compose.skip-oracle.yml up --build -d
# wait until the container passes healthcheck (gvenzl image starts quickly)
docker-compose ps
```

How to run the app against Oracle

1. Start the Oracle container (build the app image and services if needed):

```bash
# From the repository root - builds the app image and starts Oracle + app services
docker-compose up -d --build
# wait until the container passes healthcheck (gvenzl image starts quickly)
docker-compose ps
```

2. Alternatively, build and run the app jar locally against the Oracle container (useful if you prefer running the app on the host rather than in Docker):

```bash
# Build the application (skip tests for faster iteration)
mvn -DskipTests clean package

# Run the jar and activate the 'oracle' Spring profile so the app uses application-oracle.properties
# Example: run in foreground
java -jar target/authzorium-1.0-SNAPSHOT.jar --spring.profiles.active=oracle

# Or run in background (the exact command used previously):
gs nohup java -jar target/authzorium-1.0-SNAPSHOT.jar > logs/app.log 2>&1 & echo $!
# then view logs
tail -f logs/app.log

# Exact combined command (build then background run, log, and quick curl) used in the terminal:
# This runs the build, starts the app in background, prints PID, shows last logs and attempts a curl to /hello
mvn -DskipTests package && nohup java -jar target/authzorium-1.0-SNAPSHOT.jar > logs/app.log 2>&1 & echo $! && sleep 1 && tail -n 200 logs/app.log && echo "--- curl ---" && curl -v http://localhost:8082/hello || true
```

3. Quick verification (from host):

```bash
# Use the provided script to start containers (if needed) and curl /hello
chmod +x ./test-curls.sh
./test-curls.sh

# Or curl directly (app default port 8082 or docker mapping 8081):
# If running jar on host (default)
curl -v http://localhost:8082/hello
# If running via docker-compose mapping to host 8081
curl -v http://localhost:8081/hello
```

Notes: if you used the Docker path above the compose command already builds and starts the app image; if you prefer to run the jar directly, step 2 shows the exact jar command (the same command used in the terminal). If you run the background nohup command shown above, `echo $!` prints the background PID and `logs/app.log` contains the startup logs.

Notes and troubleshooting
- Oracle JDBC driver: Many Oracle JDBC artifacts are not available in Maven Central. The POM includes `ojdbc8` as an optional runtime dependency — if Maven cannot download it, please install the driver into your local Maven repository or configure Oracle's Maven repo. Alternatively set up your own local Maven repository with the driver.
- Persistence DDL: `spring.jpa.hibernate.ddl-auto=update` is set for the Oracle profile. For production use prefer explicit migrations (Flyway/Liquibase).
- H2 remains the default test/dev DB; tests won't be affected by Oracle settings.

Cleanup

```bash
docker-compose down -v
```

That's it — you now have a one-command local Oracle environment for integration testing.

## CI: injecting a different JWT secret

The application and tests read the JWT signing secret from the Spring property `security.jwt.secret`. You can override it in CI by setting the environment variable `SECURITY_JWT_SECRET` (Spring Boot maps environment variables to properties by replacing dots with underscores and upper-casing).

Example (GitHub Actions)

- Add a repository secret named `SECURITY_JWT_SECRET` (Settings -> Secrets -> Actions) containing the secret you want to use in CI.
- The included workflow `.github/workflows/ci.yml` reads that secret and sets it as an environment variable for the build job:

  SECURITY_JWT_SECRET: ${{ secrets.SECURITY_JWT_SECRET }}

This will override `security.jwt.secret` during the CI run so tests and the `JwtDecoder`/`JwtTestUtils` use the CI-provided secret.

If you run locally, you can also override the secret on the command line when running Maven:

```bash
# Bash / zsh
SECURITY_JWT_SECRET=my-super-secret mvn test
```

Or pass it as a Spring property:

```bash
mvn -Dsecurity.jwt.secret=my-super-secret test
```

## Test helpers: `BaseSliceTest`

For lightweight slice/web tests (MockMvc standalone or focused controller tests) you can use `BaseSliceTest` (added in `src/test/java/org/loket/authN`) which provides small helpers to generate HMAC-signed JWTs without bootstrapping the whole Spring context.

Example usage in a slice test:

```java
public class MyControllerSliceTest extends BaseSliceTest {
    @Test
    void secureEndpoint_withToken_returnsOk() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new MyController()).build();
        mvc.perform(get("/secure").with(bearerToken("alice")))
           .andExpect(status().isOk());
    }
}

```

`BaseSliceTest` resolves the secret from (in order):
- System property `security.jwt.secret`
- Environment variable `SECURITY_JWT_SECRET`
- fallback default `changeit-changeit-changeit-changeit`

Test coverage:
```bash
mvn -DskipTests=false clean test jacoco:report -Djacoco.skip=false -e
```
how to run only the logging tests quickly:
```bash
 mvn -Djacoco.skip=true -Dtest=logging.org.authzorium.RequestFlowLoggingTest,logging.org.authzorium.RequestFlowIntegrationTest test -DskipTests=false -q
```

test-curls.sh

## Local Docker / Make targets (quick)

- Start the project in H2/dev mode (skip Oracle): run `make up-h2` from the repository root. This target runs the compose files used for local H2/dev testing and does not trigger image builds (it uses `docker compose -f docker-compose.yml -f docker-compose.skip-oracle.yml up -d`).

- Stop and clean up containers and named volumes: `make down-h2` (runs `docker compose down -v`).

- Start only Keycloak (and its Postgres DB) without starting all services:
  - Detached (recommended for local testing):
    `docker compose -f docker-compose.yml -f docker-compose.skip-oracle.yml up -d keycloak-db keycloak`
  - Interactive/dev run (one-off; useful for debugging startup):
    `docker compose run --rm --service-ports keycloak start-dev`

- Access Keycloak admin UI (when running via the compose setup in this repo):
  - Open: http://localhost:8087/admin/ (Keycloak is mapped to host port 8087 -> container port 8080 in the compose file).

- Admin/bootstrap environment variables (recommended):
  - Note: `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` are deprecated in newer Keycloak versions. Prefer using the bootstrap variables `KC_BOOTSTRAP_ADMIN_USERNAME` and `KC_BOOTSTRAP_ADMIN_PASSWORD` in `docker-compose.yml` or your environment when you want to create a permanent admin user during container startup.
  - For quick local dev we set an admin user via compose envs (dev only). For production use `start` (not `start-dev`) and use secure bootstrap / secrets management.

- Troubleshooting tip: if host port 8087 is already in use, pick a different host port in the `ports:` mapping (e.g. `9097:8080`) or stop the process/container holding 8087.

### Quick copyable commands

Run these exact commands from the repository root (copy-paste into a zsh terminal):

```bash
# Start H2/dev (detached)
make up-h2

# Stop and remove containers + named volumes
make down-h2

# Start only Keycloak (detached)
docker compose -f docker-compose.yml -f docker-compose.skip-oracle.yml up -d keycloak-db keycloak

# One-off interactive/dev Keycloak run (useful for debugging startup)
docker compose run --rm --service-ports keycloak start-dev

# Example: set bootstrap admin envs in your shell (or put them in .env)
export KC_BOOTSTRAP_ADMIN_USERNAME=admin
export KC_BOOTSTRAP_ADMIN_PASSWORD=admin

# Follow Keycloak logs
docker logs -f loket-keycloak

# Quick HTTP probe for admin UI
curl -I http://localhost:8087/
```

- If you prefer a different host port for Keycloak, edit the `ports:` mapping in `docker-compose.yml` (for example use `9097:8080` instead of `8087:8080`).
