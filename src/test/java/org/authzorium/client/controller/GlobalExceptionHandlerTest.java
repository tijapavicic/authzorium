package org.authzorium.client.controller;

import org.authzorium.client.controller.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.authzorium.BaseSliceTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest extends BaseSliceTest {

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void whenControllerThrowsAuthenticationException_thenReturns401Json() throws Exception {
        mvc.perform(get("/test/throw-auth").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void whenControllerThrowsJwtException_thenReturns401JsonWithSpecificMessage() throws Exception {
        mvc.perform(get("/test/throw-jwt").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid or expired JWT token"));
    }

    @Test
    void whenControllerThrowsAccessDeniedException_thenReturns403Json() throws Exception {
        mvc.perform(get("/test/throw-access").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void whenEntityNotFound_thenReturns404Json() throws Exception {
        mvc.perform(get("/test/throw-not-found").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @Disabled
    @DisplayName("fixme: Validation errors should return 400 with JSON body")
    void whenValidationFails_thenReturns400Json() throws Exception {
        // send empty JSON so @NotNull field is missing
        mvc.perform(post("/test/validate").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/test/validate"));
    }


    @Test
    void whenRequestHasBearerToken_thenHeaderIsPresent() throws Exception {
        mvc.perform(get("/test/secure").with(bearerToken("alice"))
                        .accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertTrue("secure-ok".equals(body));
                });
    }

    @RestController
    @RequestMapping("/test")
    public static class TestController {

        @GetMapping("/throw-auth")
        public void throwAuth() {
            throw new AuthenticationCredentialsNotFoundException("No credentials available");
        }

        @GetMapping("/throw-jwt")
        public void throwJwt() {
            throw new JwtException("JWT invalid");
        }

        @GetMapping("/throw-access")
        public void throwAccess() {
            throw new AccessDeniedException("Access is denied");
        }

        @GetMapping("/throw-not-found")
        public void throwNotFound() {
            throw new EntityNotFoundException("Entity not found");
        }

        public static class Payload {
            @NotNull
            public String required;
        }

        @PostMapping("/validate")
        public void validate(@Valid @RequestBody Payload payload) {
            // if valid, do nothing
        }

        @GetMapping("/secure")
        public String secure() {
            return "secure-ok";
        }
    }
}
