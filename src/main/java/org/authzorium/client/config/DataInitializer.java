package org.authzorium.client.config;

import org.authzorium.client.model.User;
import org.authzorium.client.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner init(UserRepository userRepository) {
        return args -> {
            if (userRepository.findByUsername("demo-user").isEmpty()) {
                userRepository.save(new User("demo-user", "Demo User"));
            }
        };
    }
}

