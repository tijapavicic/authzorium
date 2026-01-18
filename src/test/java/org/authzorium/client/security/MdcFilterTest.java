package org.authzorium.client.security;


import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
class MdcFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenUnauthenticated_thenXRequestIdEchoed_andMdcLogged() throws Exception {
        // attach a ListAppender to capture logs from RestAuthenticationEntryPoint
        Logger logger = (Logger) LoggerFactory.getLogger(RestAuthenticationEntryPoint.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            String requestId = "test-req-123";
            String forwardedIp = "203.0.113.7";

            // perform a request that will be rejected (401) by the security filter chain
            mockMvc.perform(get("/admin/hello")
                            .header("X-Request-ID", requestId)
                            .header("X-Forwarded-For", forwardedIp))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("X-Request-ID", requestId))
                    // Assert ErrorResponse JSON shape
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.path").value("/admin/hello"));

            // Assert that one of the logged events contains the MDC properties set by the filter
            List<ILoggingEvent> logsList = listAppender.list;
            boolean foundRequestId = logsList.stream().anyMatch(e -> requestId.equals(e.getMDCPropertyMap().get("requestId")));
            boolean foundRemoteIp = logsList.stream().anyMatch(e -> forwardedIp.equals(e.getMDCPropertyMap().get("remoteIp")));

            assertThat(foundRequestId).as("Expected a log event with MDC requestId=%s", requestId).isTrue();
            assertThat(foundRemoteIp).as("Expected a log event with MDC remoteIp=%s", forwardedIp).isTrue();
        } finally {
            logger.detachAppender(listAppender);
            listAppender.stop();
        }
    }

    @Test
    void whenAuthenticated_thenXRequestIdEchoed_andMdcLogged() throws Exception {
        // attach a ListAppender to capture logs from RestAuthenticationEntryPoint
        Logger logger = (Logger) LoggerFactory.getLogger(RestAuthenticationEntryPoint.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            String requestId = "test-req-123";
            String forwardedIp = "203.0.113.7";

            // perform a request that will be rejected (401) by the security filter chain
            mockMvc.perform(get("/hello")
                            .header("X-Request-ID", requestId)
                            .header("X-Forwarded-For", forwardedIp))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Request-ID", requestId));

            // Assert that one of the logged events contains the MDC properties set by the filter
            List<ILoggingEvent> logsList = listAppender.list;
            boolean foundRequestId = logsList.stream().anyMatch(e -> requestId.equals(e.getMDCPropertyMap().get("requestId")));
            boolean foundRemoteIp = logsList.stream().anyMatch(e -> forwardedIp.equals(e.getMDCPropertyMap().get("remoteIp")));

            assertThat(foundRequestId).as("Expected a log event with MDC requestId=%s", requestId).isFalse();
            assertThat(foundRemoteIp).as("Expected a log event with MDC remoteIp=%s", forwardedIp).isFalse();
        } finally {
            logger.detachAppender(listAppender);
            listAppender.stop();
        }
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void whenAuthenticatedWithoutAdmin_then403Logged_andMdcLogged() throws Exception {
        // attach a ListAppender to capture logs from LoggingAccessDeniedHandler
        Logger logger = (Logger) LoggerFactory.getLogger(LoggingAccessDeniedHandler.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            String requestId = "test-req-403";
            String forwardedIp = "203.0.113.7";

            mockMvc.perform(get("/admin/hello")
                            .header("X-Request-ID", requestId)
                            .header("X-Forwarded-For", forwardedIp))
                    .andExpect(status().isForbidden())
                    .andExpect(header().string("X-Request-ID", requestId))
                    // Assert ErrorResponse JSON shape
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.path").value("/admin/hello"));

            List<ILoggingEvent> logsList = listAppender.list;
            boolean foundRequestId = logsList.stream().anyMatch(e -> requestId.equals(e.getMDCPropertyMap().get("requestId")));
            boolean foundRemoteIp = logsList.stream().anyMatch(e -> forwardedIp.equals(e.getMDCPropertyMap().get("remoteIp")));

            assertThat(foundRequestId).as("Expected a log event with MDC requestId=%s", requestId).isTrue();
            assertThat(foundRemoteIp).as("Expected a log event with MDC remoteIp=%s", forwardedIp).isTrue();
        } finally {
            logger.detachAppender(listAppender);
            listAppender.stop();
        }
    }
}
