package org.authzorium.client.repository;

import org.authzorium.client.entity.PetEntity;
import org.authzorium.client.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

@DataJpaTest
public class PetEntityRepositoryTest {

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void crudOperationsOnPetEntity() {
        UserEntity owner = new UserEntity();
        owner.setUsername("owner1");
        owner.setDisplayName("Owner One");
        owner = userRepository.save(owner);

        PetEntity pet = new PetEntity();
        pet.setPetName("Fido");
        pet.setOwner(owner);
        pet = petRepository.save(pet);

        assertThat(pet.getId()).isNotNull();
        assertThat(petRepository.findByPetName("Fido")).isPresent();

        List<PetEntity> byOwner = petRepository.findByOwnerId(owner.getId());
        assertThat(byOwner).hasSize(1);

        // update
        pet.setPetName("Fido2");
        pet = petRepository.save(pet);
        assertThat(petRepository.findByPetName("Fido2")).isPresent();

        // delete
        petRepository.delete(pet);
        assertThat(petRepository.findByPetName("Fido2")).isEmpty();
    }
}

