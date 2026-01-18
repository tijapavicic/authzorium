package org.authzorium.client;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
@SpringBootApplication
public class AuthZoriumApplication {

    public static final String LOCAL_SERVER_PORT = "local.server.port";
    public static final String PORT_DEFAULT_VALUE = "8081";

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(AuthZoriumApplication.class, args);
        Environment env = ctx.getEnvironment();
        String[] activeProfiles = env.getActiveProfiles();
        // local.server.port is populated by the embedded server in some contexts; fall back to server.port
        String port = env.getProperty(LOCAL_SERVER_PORT,
                env.getProperty("server.port", PORT_DEFAULT_VALUE));
        log.info("Active profiles: {} | server.port={}", activeProfiles, port);
    }
}

