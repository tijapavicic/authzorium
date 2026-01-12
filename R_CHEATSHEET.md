
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=localh2

mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=localh2"

```


Build and run a fat jar:
```bash
mvn -DskipTests package
java -jar target/*.jar --spring.profiles.active=localh2

```