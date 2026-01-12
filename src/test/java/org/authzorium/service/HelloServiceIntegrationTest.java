package org.authzorium.service;

import org.junit.jupiter.api.Test;
import org.authzorium.BaseTest;
import org.authzorium.model.User;
import org.authzorium.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HelloServiceIntegrationTest extends BaseTest {

    @Autowired
    private HelloService helloService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void helloServiceReturnsUserDisplayNameWhenUserExists() {
        // Clean repository to avoid duplicate demo-user entries from DataInitializer
        userRepository.deleteAll();

        // Insert a single demo-user which HelloService looks up
        userRepository.save(new User("demo-user", "Demo Integration"));

        String greeting = helloService.hello();
        assertEquals("Hello, Demo Integration!", greeting);
    }
}
