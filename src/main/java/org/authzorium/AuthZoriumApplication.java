package org.authzorium;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class AuthZoriumApplication {
    private static final Logger log = LoggerFactory.getLogger(AuthZoriumApplication.class);

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(AuthZoriumApplication.class, args);
        Environment env = ctx.getEnvironment();
        String[] activeProfiles = env.getActiveProfiles();
        // local.server.port is populated by the embedded server in some contexts; fall back to server.port
        String port = env.getProperty("local.server.port",
                env.getProperty("server.port", "8081"));
        log.info("Active profiles: {} | server.port={}", activeProfiles, port);
    }
}
