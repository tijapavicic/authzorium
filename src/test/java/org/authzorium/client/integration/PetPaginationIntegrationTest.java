package org.authzorium.client.integration;

import org.authzorium.client.repository.PetRepository;
import org.authzorium.client.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.IntStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PetPaginationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PetRepository petRepository;

    @BeforeEach
    void setup() {
        petRepository.deleteAll();
        userRepository.deleteAll();

        var userEntity = new org.authzorium.client.entity.UserEntity();
        userEntity.setUsername("test-user");
        userEntity.setDisplayName("Test User");
        var savedUser = userRepository.save(userEntity);

        IntStream.range(0, 5).forEach(i -> {
            var pet = new org.authzorium.client.entity.PetEntity();
            pet.setPetName("pet-" + i);
            pet.setOwner(savedUser);
            petRepository.save(pet);
        });
    }

    @Test
    void pagedPetsEndpointReturnsPagedResults() throws Exception {
        mockMvc.perform(get("/users/test-user/pets?page=0&size=2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    void blazeEndpointFallbackOrBlazeWorks() throws Exception {
        // This test asserts that the blaze endpoint at least returns results or falls back to JPA.
        mockMvc.perform(get("/users/test-user/pets-blaze?page=0&size=3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(5));
    }
}
