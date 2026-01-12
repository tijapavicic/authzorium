package org.authzorium.repository;

import org.junit.jupiter.api.Test;
import org.authzorium.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class UserRepositoryTest {

    public static final String GIZMO_CIC = "Gizmo Cic";
    public static final String GIZMO = "gizmo";
    @Autowired
    private UserRepository userRepository;

    @Test
    public void saveAndFindByUsername() {
        User u = new User(GIZMO, GIZMO_CIC);
        userRepository.save(u);

        Optional<User> result = userRepository.findByUsername(GIZMO);
        assertTrue(result.isPresent());
        assertEquals(GIZMO_CIC, result.get().getDisplayName());
    }
}

