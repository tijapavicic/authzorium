package org.authzorium.controller;

import org.junit.jupiter.api.Test;
import org.authzorium.BaseTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandlerIntegrationTest.TestController.class)
public class GlobalExceptionHandlerIntegrationTest extends BaseTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void postInvalidPayload_returnsErrorResponseJson() throws Exception {
        // send empty JSON so @NotNull field is missing
        mvc.perform(post("/test/validate-integration")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/test/validate-integration"));
    }

    @RestController
    @RequestMapping("/test")
    public static class TestController {
        public static class Payload {
            @NotNull
            public String required;
        }

        @PostMapping("/validate-integration")
        public void validate(@Valid @RequestBody Payload payload) {
            // no-op
        }
    }
}
