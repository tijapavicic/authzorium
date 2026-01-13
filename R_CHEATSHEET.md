
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=localh2

mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=localh2"

```


Build and run a fat jar:
```bash
mvn -DskipTests package
java -jar target/*.jar --spring.profiles.active=localh2

```

# Start full stack with Docker Compose
```bash
# from project root
docker compose up --build
# or detached:
docker compose up --build -d
```
# Start stack without Oracle DB
```bash
# uses the skip-oracle override file which sets SPRING_PROFILES_ACTIVE=localh2 and removes Oracle dependency
docker compose -f docker-compose.yml -f docker-compose.skip-oracle.yml up --build
# detached:
docker compose -f docker-compose.yml -f docker-compose.skip-oracle.yml up --build -d
```

If you want only specific services (app + keycloak + keycloak-db + sonarqube + sonarqube-db) with the H2 override:
```bash
docker compose -f docker-compose.yml -f docker-compose.skip-oracle.yml up --build app keycloak keycloak-db sonarqube sonarqube-db
# detached:
docker compose -f docker-compose.yml -f docker-compose.skip-oracle.yml up --build -d app keycloak keycloak-db sonarqube sonarqube-db
```
## rebuild only the app service:
```bash
docker compose build --no-cache app
# then start
docker compose up -d app

```
Force Oracle torun on m1
```bash
# Start only Oracle under amd64 emulation
DOCKER_DEFAULT_PLATFORM=linux/amd64 docker compose up -d oracle-db

# Or run the full stack under amd64 (slower, forces emulation for all services)
DOCKER_DEFAULT_PLATFORM=linux/amd64 docker compose up --build -d

docker buildx build --platform linux/amd64 -t oracle-db:21c . -f docker/oracle-db/Dockerfile
```
#Tail logs
```bash
# Follow logs for all compose services
docker compose logs -f

# Follow only app logs
docker compose logs -f app

# Or tail the container directly (container name from compose):
docker logs -f authzorium
docker logs -f loket-oracle-xe

```
# Clean up
# remove all containers, networks, volumes, and images created by `up`
```bash
# Stop and remove containers, keep named volumes:
docker compose down

# Stop/remove containers AND named volumes (removes Oracle DB data):
docker compose down -v

# Remove stopped containers for project:
docker compose rm -f

# Remove a named volume (e.g., oracle-data) if compose down -v didn't remove it:
docker volume rm authzorium_oracle-data
# note: volume name may be prefixed by the compose project name; run `docker volume ls` to confirm

# Stop/remove containers, volumes, AND images built by compose:
docker compose down --rmi all -v
```
# test the REST API
```bash
# make the script executable (one-time)
chmod +x ./test-curls.sh

# builds/starts the stack and polls the /hello endpoint (as documented in README_DOCKER.md)
./test-curls.sh

```
# Access Keycloak admin console
http://localhost:8080/auth/admin/
- username: admin
- password: admin
# Access SonarQube
http://localhost:9000
- username: admin
- password: admin
``` 
# Access the application
http://localhost:8081/api/hello
``` 
Returns:
```json
{
  "message": "Hello, World!"
}
``` 
# Access H2 Console
http://localhost:8081/h2-console
- JDBC URL: jdbc:h2:mem:testdb
- User Name: sa
- Password: (leave blank)
```         
# Stop the stack
```bash
docker compose down
```

