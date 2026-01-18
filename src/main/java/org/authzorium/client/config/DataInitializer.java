package org.authzorium.client.config;

import org.authzorium.client.entity.UserEntity;
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
                userRepository.save(new UserEntity("gizmo", "Gizmo Cic"));
                userRepository.save(new UserEntity("demo-user", "Demo UserEntity"));
            }
        };
    }
}

