package org.authzorium.client.security;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.authzorium.client.security.LoggingAccessDeniedHandler;
import org.authzorium.client.security.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test,local-h2")
class SecurityLoggingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenUnauthenticated_then401LoggedAndHeaderEchoed() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(RestAuthenticationEntryPoint.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            String requestId = "sec-test-401";
            mockMvc.perform(get("/hello").header("X-Request-ID", requestId))
                    .andExpect(status().isOk()); // /hello is permitAll and returns 200

            // Call protected admin endpoint to force 401
            mockMvc.perform(get("/admin/hello").header("X-Request-ID", requestId))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("X-Request-ID", requestId))
                    // Assert ErrorResponse JSON shape
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.path").value("/admin/hello"));

            List<ILoggingEvent> logs = appender.list;
            boolean found = logs.stream().anyMatch(e -> requestId.equals(e.getMDCPropertyMap().get("requestId")));
            assertThat(found).isTrue();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void whenAuthenticatedWithoutRole_then403Logged() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(LoggingAccessDeniedHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            String requestId = "sec-test-403";
            mockMvc.perform(get("/admin/hello").header("X-Request-ID", requestId))
                    .andExpect(status().isForbidden())
                    .andExpect(header().string("X-Request-ID", requestId))
                    // Assert ErrorResponse JSON shape
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.path").value("/admin/hello"));

            List<ILoggingEvent> logs = appender.list;
            boolean found = logs.stream().anyMatch(e -> requestId.equals(e.getMDCPropertyMap().get("requestId")));
            assertThat(found).isTrue();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
