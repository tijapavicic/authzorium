package org.authzorium.client.repository;

import org.authzorium.client.dto.Pet;
import org.authzorium.client.dto.User;
import org.authzorium.client.entity.PetEntity;
import org.authzorium.client.entity.UserEntity;
import org.authzorium.client.mapper.PetMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class PetEntityIntegrationTest {

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PetMapper petMapper;

    @Test
    void dtoToEntityAndBack() {
        UserEntity owner = new UserEntity();
        owner.setUsername("owner2");
        owner.setDisplayName("Owner Two");
        owner = userRepository.save(owner);

        User ownerDto = User.builder().id(owner.getId()).username(owner.getUsername()).displayName(owner.getDisplayName()).build();
        Pet dto = Pet.builder().petName("Buddy").owner(ownerDto).build();
        PetEntity entity = petMapper.toEntity(dto);
        entity = petRepository.save(entity);

        Pet read = petMapper.toDto(petRepository.findById(entity.getId()).orElseThrow());
        assertThat(read.getPetName()).isEqualTo("Buddy");
        assertThat(read.getOwner()).isNotNull();
        assertThat(read.getOwner().getId()).isEqualTo(owner.getId());
        assertThat(read.getOwner().getUsername()).isEqualTo(owner.getUsername());
    }
}
