package org.authzorium.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"localh2", "test"})
class SecurityActuatorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void actuatorHealthIsPermitted() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andDo(print())
                .andExpect(status().isOk())
                // Actuator uses a vendor media type (application/vnd.spring-boot.actuator.v3+json).
                // Assert the response Content-Type contains 'json' so vendor types pass too.
                .andExpect(result -> {
                    String ct = result.getResponse().getContentType();
                    org.junit.jupiter.api.Assertions.assertTrue(ct != null && ct.toLowerCase().contains("json"),
                            () -> "Expected JSON content-type, but was: " + ct);
                })
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void actuatorRootAndEndpointsVisibility() throws Exception {
        // GET /actuator (links root)
        mockMvc.perform(get("/actuator"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links").exists());

        // GET /actuator again and allow security outcomes (200/401/403) for downstream checks
        mockMvc.perform(get("/actuator"))
                .andDo(print())
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(status == 200 || status == 401 || status == 403,
                            () -> "Unexpected status for /actuator: " + status);
                });
    }
}
