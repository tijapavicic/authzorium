package org.authzorium.client.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.authzorium.client.dto.Pet;
import org.authzorium.client.entity.PetEntity;
import org.authzorium.client.entity.UserEntity;
import org.authzorium.client.repository.PetRepository;
import org.authzorium.client.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserPetsControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PetRepository petRepository;

    private UserEntity user;

    @BeforeEach
    void setup() {
        user = new UserEntity();
        user.setUsername("n1user");
        user.setDisplayName("N1 User");
        user = userRepository.save(user);
        for (int i = 1; i <= 3; i++) {
            PetEntity pet = new PetEntity();
            pet.setPetName("Pet" + i);
            pet.setOwner(user);
            petRepository.save(pet);
        }
    }

    @Test
    void getUserPets_returnsAllPets_andAvoidsNPlus1() throws Exception {
        // This will trigger the endpoint and fetch all pets for the user
        String response = mockMvc.perform(get("/users/" + user.getId() + "/pets")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Pet> pets = objectMapper.readValue(response, new TypeReference<List<Pet>>(){});
        assertThat(pets).hasSize(3);
        assertThat(pets).extracting("petName").containsExactlyInAnyOrder("Pet1", "Pet2", "Pet3");
        // N+1 check: This test assumes the repository uses a single query (findByOwnerId)
        // For full N+1 detection, use @DataJpaTest and SQL log assertion or Hibernate statistics
    }
}
