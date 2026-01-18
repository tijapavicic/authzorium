package org.authzorium.client.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.authzorium.client.dto.User;
import org.authzorium.client.service.HelloService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
class UserEntityControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HelloService helloService;

    @Test
    void findByUsername_integration_returnsGreeting() throws Exception {
        when(helloService.findByUsername("bob")).thenReturn("Hello bob");

        mockMvc.perform(get("/users/findByUsername/bob"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.greeting").value("Hello, secured world!"));
    }
    @Test
    void findByUsername_returnsGreetingJson() throws Exception {
        when(helloService.findByUsername("alice")).thenReturn("Hello alice");

        mockMvc.perform(get("/users/findByUsername/gizmo"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.greeting").value("Hello, secured world!"));
    }

    @Test
    void saveUser_returnsSavedUser() throws Exception {
        User input = new User(null, "newuser", "New UserEntity");
        User saved = new User(42L, "newuser", "New UserEntity");

        when(helloService.saveUser(any(User.class))).thenReturn(saved);

        mockMvc.perform(post("/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.displayName").value("New UserEntity"));
    }

    @Test
    void saveUser_validationFailure_missingUsername_returnsBadRequest() throws Exception {
        // Missing username -> invalid
        User invalid = new User(null, "", "No Username");

        mockMvc.perform(post("/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/users"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}
