package org.authzorium.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("localh2")
class LocalH2PropertiesTest {

    @Autowired
    private ApplicationContext ctx;

    @Test
    void localH2PropertiesAreLoaded() {
        Environment env = ctx.getEnvironment();

        // server port
        assertThat(env.getProperty("server.port")).isEqualTo("8081");

        // datasource
        assertThat(env.getProperty("spring.datasource.url")).isEqualTo("jdbc:h2:mem:loketdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        assertThat(env.getProperty("spring.datasource.driver-class-name")).isEqualTo("org.h2.Driver");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("sa");

        // JPA
        assertThat(env.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("create-drop");
        assertThat(env.getProperty("spring.jpa.show-sql")).isEqualTo("true");

        // H2 console
        assertThat(env.getProperty("spring.h2.console.enabled")).isEqualTo("true");
        assertThat(env.getProperty("spring.h2.console.path")).isEqualTo("/h2-console");

        // Logging pattern contains our MDC tokens
        String pattern = env.getProperty("logging.pattern.console");
        assertThat(pattern).isNotNull();
        assertThat(pattern).contains("%X{requestId");
        assertThat(pattern).contains("%X{remoteIp");
    }
}
