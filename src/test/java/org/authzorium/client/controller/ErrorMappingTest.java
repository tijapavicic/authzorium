package org.authzorium.client.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ErrorMappingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenValidationFails_thenReturns400Json() throws Exception {
        // send empty body to trigger @NotBlank validation failure
        mockMvc.perform(post("/test/validate").with(user("test").roles("USER")).with(csrf()).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                // message may be produced by our handler or by ProblemDetail produced earlier; accept either form
                .andExpect(jsonPath("$.message", anyOf(containsString("must not be blank"), containsString("Invalid request content."))))
                .andExpect(jsonPath("$.path").value("/test/validate"));
    }

    @Test
    void whenEntityNotFound_thenReturns404Json() throws Exception {
        mockMvc.perform(get("/test/notfound").with(user("test").roles("USER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/test/notfound"));
    }

    @Test
    void whenProblemDetail_thenConvertedToErrorResponse() throws Exception {
        mockMvc.perform(get("/test/problem").with(user("test").roles("USER")))
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(418))
                .andExpect(jsonPath("$.error").value("I'm a teapot"))
                .andExpect(jsonPath("$.message").value("Short and stout"))
                .andExpect(jsonPath("$.path").value("/test/problem"));
    }

    @Test
    void whenProblemDetailEmpty_thenFallbacksApply() throws Exception {
        mockMvc.perform(get("/test/problem-empty").with(user("test").roles("USER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                // ProblemDetail implementation falls back to reason phrase
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/test/problem-empty"));
    }

    @Test
    void whenJwtException_thenReturns401WithStandardMessage() throws Exception {
        mockMvc.perform(get("/test/throw-jwt").with(user("test").roles("USER")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid or expired JWT token"))
                .andExpect(jsonPath("$.path").value("/test/throw-jwt"));
    }
}
