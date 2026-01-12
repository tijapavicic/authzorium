package org.authzorium.client.security;

import org.junit.jupiter.api.Test;
import org.authzorium.BaseTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Import(SecurityIntegrationTest.TestController.class)
public class SecurityIntegrationTest extends BaseTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void whenNoToken_thenUnauthorized_andJsonBody() throws Exception {
        // Use controller endpoint that throws AuthenticationCredentialsNotFoundException so ControllerAdvice handles it
        mvc.perform(get("/test/throw-auth").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/test/throw-auth"));
    }

    @Test
    void whenValidToken_thenOk() throws Exception {
        String token = jwtTestUtils.createHmacToken("user-1");

        mvc.perform(get("/test/secure").header("Authorization", "Bearer " + token).accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(content().string("secure-ok"));
    }

    @Test
    void whenControllerThrowsAccessDenied_thenForbidden_andJsonBody() throws Exception {
        String token = jwtTestUtils.createHmacToken("user-1");

        mvc.perform(get("/test/throw-access").header("Authorization", "Bearer " + token).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied from controller"))
                .andExpect(jsonPath("$.path").value("/test/throw-access"));
    }

    @RestController
    @RequestMapping("/test")
    public static class TestController {

        @GetMapping("/secure")
        public String secure() {
            return "secure-ok";
        }

        @GetMapping("/throw-access")
        public void throwAccess() {
            throw new org.springframework.security.access.AccessDeniedException("Access denied from controller");
        }

        @GetMapping("/throw-auth")
        public void throwAuth() {
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException("No credentials available");
        }
    }
}
