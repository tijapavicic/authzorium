package org.authzorium.client.repository;

import org.authzorium.client.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class UserEntityRepositoryTest {

    public static final String GIZMO_CIC = "Gizmo Cic";
    public static final String GIZMO = "gizmo";
    @Autowired
    private UserRepository userRepository;

    @Test
    void saveAndFindByUsername() {
        UserEntity u = new UserEntity(GIZMO, GIZMO_CIC);
        userRepository.save(u);

        Optional<UserEntity> result = userRepository.findByUsername(GIZMO);
        assertTrue(result.isPresent());
        assertEquals(GIZMO_CIC, result.get().getDisplayName());
    }
}

