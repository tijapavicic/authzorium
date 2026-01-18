# Presentation: Profiles

I'll explain how Spring Boot decides which profiles are active, step-by-step, and give developer-level details, precedence rules, examples, and debugging tips (including how your AuthCoriumApplication snippet fits in).

### Quick checklist
* Show the activation mechanisms (all the ways profiles can be set)
* Show the precedence/order that determines which wins
* Describe when profile-specific config and beans are loaded/registered
  Cover special features: spring.profiles.include, YAML multi-doc spring.profiles, programmatic activation
* Give practical recommendations and debug tips you can run now


### High-level summary*

* A “profile” is a logical name (like dev/prod/test) that controls which beans and configuration are active.
  Spring’s
* Environment holds two profile lists: active and default.
* If no active profiles are set, Spring treats the single profile "default" as active.
* Profiles can be set in many places (command-line, JVM system props, env vars, application properties, programmatically). Spring resolves all property sources in a fixed precedence order — the highest-precedence source that specifies spring.profiles.active wins.
  Once the active profiles are known (early in startup), Spring loads profile-specific configuration (application-{profile}.properties/yaml documents) and evaluates @Profile annotations so only matching beans are registered.

### Detailed, step-by-step behavior and timeline (what happens on startup)

* SpringApplication creation
    * You create/run the app (e.g. SpringApplication.run(AuthCoriumApplication.class,...)).
    * You can call SpringApplication.setAdditionalProfiles(...) before run() to programmatically add profiles. These are applied early.
* Environment preparation and property sources
    * Spring builds an Environment and adds property sources (command line args, System properties, environment variables, config files, etc.). The exact set and order of property sources is important because it determines which value of spring.profiles.active will be used.
    * Since Spring Boot 2.4+, the new Config Data API loads application.properties/application.yml and profile-specific files via the ConfigData loader. But activation still follows the same principle: first discover active profiles (from high-precedence property sources) then load profile-specific resources.
* Determine active profiles
    * Spring looks for the spring.profiles.active property in the aggregated Environment (i.e., across property sources). It also recognizes SPRING_PROFILES_ACTIVE env var and command-line arg --spring.profiles.active=....
    * If none set, no explicit active profile is set; Spring will then consider the special "default" profile as active (you’ll see log: "No active profile set, falling back to 1 default profile: 'default'").
* Load profile-specific configuration
    * For each active profile, Spring Boot loads profile-specific config (application-{profile}.properties/application-{profile}.yml or YAML multi-docs which have spring.profiles: <name>).</name>
    * Loaded profile-specific properties override non-profile-specific ones according to property-source precedence rules.
* Bean registration and @Profile evaluation

    * Bean definitions are processed. @Profile annotations are evaluated against Environment.getActiveProfiles().
    * Beans whose @Profile expression does not match the active profile(s) are not registered into the ApplicationContext.
    * Beans without @Profile are always considered (subject to other conditions like @Conditional).
* Context refresh and application start
    * After configuration and bean registration, the context is refreshed and the app runs. Your code (like your AuthCoriumApplication main) can read env.getActiveProfiles() to log or make runtime decisions.

## Who/what can activate a profile (activation mechanisms)

* Programmatically:
    * SpringApplication.setAdditionalProfiles("dev")
    * ConfigurableEnvironment.setActiveProfiles(...)
* Command-line arguments:
    * --spring.profiles.active=dev,prod
* JVM system properties:
    * -Dspring.profiles.active=dev
* Environment variables:
    * SPRING_PROFILES_ACTIVE=dev (or SPRING_PROFILES_ACTIVE=dev,prod)
* SPRING_APPLICATION_JSON:
    * JSON taken from env var or property: e.g. SPRING_APPLICATION_JSON='{"spring":{"profiles":{"active":"dev"}}}'
* application.properties / application.yml:
    * spring.profiles.active=dev (note: lower precedence than command-line & env vars)
* Test-specific:
  @ActiveProfiles("test") on test classes (test framework sets profiles for test run)
* YAML multi-docs:
    * A YAML document can have top-level spring.profiles: dev to load that document only for that profile
* spring.profiles.include:
    * You can include additional profiles in any config: spring.profiles.include=shared
* Special: environment post-processors and config data loaders can set/affect profiles (e.g. custom EnvironmentPostProcessor)

### Precedence: which source wins when multiple set the active profile (From highest precedence to lowest for the purpose of resolving properties that control profiles)
* Command-line arguments (highest)
* Java system properties (-D)
* OS environment variables (SPRING_PROFILES_ACTIVE)
* SPRING_APPLICATION_JSON
* ServletConfig/ServletContext init params (web apps) / JNDI (less common)
* Profile-specific application properties loaded from config locations (application-{profile}.properties/yml)
* application.properties / application.yml on the classpath
* SpringApplication.setDefaultProperties() Programmatic setAdditionalProfiles is applied early and acts like an explicitly set active profile (effectively high precedence because it’s applied before config file loading). Test annotations like @ActiveProfiles are applied by the Spring TestContext framework and take effect for test contexts.

Important notes on precedence and interaction
- If you set --spring.profiles.active=prod on the command line, that will override whatever is in application.properties.
* If your code calls setAdditionalProfiles("dev") and you also pass --spring.profiles.active=prod on the command line, the final active profiles will contain both unless overridden — but order matters for which config files are loaded and overrides. Programmatic additional profiles are added to the environment’s active profile list.
* spring.profiles.include lets you attach additional profiles to whatever is active. It’s additive — it does not “win over” spring.profiles.active; it augments.
### How profile-specific configuration is applied
Profiles only affect which profile-specific config documents/files are loaded. Example:
* application.yml contains base config
* application-dev.yml contains dev overrides
* If active profile is dev, application-dev.yml is loaded and its properties override base ones (subject to property source precedence).
* YAML multi-document: you can put multiple documents in application.yml:
  spring: profiles: "dev" foo: dev-value
  spring: profiles: "prod" foo: prod-value The loader will include the appropriate document(s) based on active profiles.

* Default profile behavior
    * If no active profile is set, the special profile "default" is considered active. Beans declared with @Profile("default") are included only when no explicit active profiles were set.
    * You normally don't need to use the "default" profile; it is mostly for explicit fallback configuration.
### Debugging and inspecting profiles at runtime
* Programmatically:
    * Environment env = ctx.getEnvironment(); env.getActiveProfiles(); env.getDefaultProfiles();
    * Log them (your main already does env.getActiveProfiles() and logs them).
* Actuator:
    * /actuator/env endpoint exposes active profiles and property sources (if actuator is enabled and endpoints exposed).
* Start with --debug:
    * Passing --debug will instruct Spring Boot to log the auto-configuration report and property source origins; very useful to see where spring.profiles.active came from.

### Useful commands/techniques:
    * Run with --spring.profiles.active=dev and verify logs: Spring Boot logs which profiles are active.
    * Add a small bean that prints Environment.getActiveProfiles() early (e.g., ApplicationRunner or an initializer).
    * Use SPRING_APPLICATION_JSON to temporarily set profiles without touching files.
### Examples (practical)
* Shell / container:
    * docker run -e SPRING_PROFILES_ACTIVE=prod myapp
    * kubernetes: set env var SPRING_PROFILES_ACTIVE=prod in Pod spec
* Command-line:
    * java -jar app.jar --spring.profiles.active=prod
* JVM:
  JVM:
    * java -Dspring.profiles.active=dev -jar app.jar
* application.properties (lowest priority):
    * application.properties: spring.profiles.active=dev
    * Not recommended for runtime differences because it’s easy to override accidentally.
### Common pitfalls and gotchas
* Local dev: localhost resolution to ::1 vs 127.0.0.1 can change behavior unrelated to profiles — that’s networking. (You reported ECONNREFUSED ::1:8084 earlier; not related to profiles.)
* Setting spring.profiles.active IN application.properties is often a maintenance footgun — you’ll need to override it for deploys; prefer environment or command-line for ops.
* Tests: @SpringBootTest will load a context and, if you don’t set @ActiveProfiles, you’ll likely see the "default" profile. Use @ActiveProfiles for explicit test isolation.
* Order of activation vs property loading: because profile selection occurs very early, a profile-specific file will be loaded only when the profile is active — but the property that activates the profile must be discoverable in a high-enough precedence source (e.g., command line, env var, system prop). If you put spring.profiles.active in a profile-specific file, that’s circular and won’t work.

### How your Application ties in

```java
@Slf4j
@SpringBootApplication
public class AuthCoriumApplication {
    public static void main(String[] args) {

        ConfigurableApplicationContext ctx =
                SpringApplication.run(AuthCoriumApplication.class, args);

        Environment env = ctx.getEnvironment();
        String[] activeProfiles = env.getActiveProfiles();

        /* local.server.port is populated by
        embedded server in some contexts;
        fall back to server.port*/
        String port = env.getProperty("local.server.port",
                env.getProperty("server.port", "8081"));
        log.info("Active profiles: {} | server.port={}", activeProfiles, port);
    }
}
```

* Your main reads active profiles:
    * Environment env = ctx.getEnvironment();
    * String[] activeProfiles = env.getActiveProfiles(); That will show whatever the early resolution determined (command line, env var, or default fallback). When you see the log message "No active profile set, falling back to 1 default profile: 'default'" (as appeared earlier), that means no source provided spring.profiles.active and the default profile is being applied.
### Recommendations / best practices
    * For deploys/containers: set SPRING_PROFILES_ACTIVE (env) or pass --spring.profiles.active= on startup.
    * For dev: use application-dev.yml for dev-specific configuration and do not hardcode profiles in application.properties.
    * For tests: use @ActiveProfiles and/or @TestPropertySource to control test config.
    * For shared config snippets: use spring.profiles.include to pull common settings into profiles that need them.
    * For debugging: start with --debug, check /actuator/env, or log Environment.getActiveProfiles() early. 
