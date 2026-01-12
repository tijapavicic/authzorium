package org.authzorium.config;

import org.authzorium.model.User;
import org.authzorium.repository.UserRepository;
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

