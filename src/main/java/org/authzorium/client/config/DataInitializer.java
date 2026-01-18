package org.authzorium.client.config;

import org.authzorium.client.entity.PetEntity;
import org.authzorium.client.entity.UserEntity;
import org.authzorium.client.repository.PetRepository;
import org.authzorium.client.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {
    public static final String CRUELLA = "cruella";
    public static final String GIZMO = "gizmo";
    private static final String DEMO_USER = "demo-user";

    @Bean
    CommandLineRunner init(UserRepository userRepository, PetRepository petRepository) {
        return args -> {
            if (userRepository.findByUsername(DEMO_USER).isEmpty()) {
                userRepository.save(new UserEntity(GIZMO, "Gizmo Cic"));
                userRepository.save(new UserEntity(DEMO_USER, "Demo UserEntity"));
                userRepository.save(new UserEntity(CRUELLA, "Cruella De Vil"));
            }
            // Always ensure demo-user exists before assigning as owner
            userRepository.findByUsername(GIZMO).ifPresent(user -> {
                if (petRepository.findByOwnerId(user.getId()).isEmpty()) {
                    petRepository.save(
                        PetEntity.builder()
                            .petName("Fido")
                            .owner(user)
                            .build()
                    );
                }
            });

            userRepository.findByUsername(CRUELLA).ifPresent(user -> {
                if (petRepository.findByOwnerId(user.getId()).isEmpty()) {
                    petRepository.save(
                            PetEntity.builder()
                                    .petName("Perdita")
                                    .owner(user)
                                    .build()
                    );
                    petRepository.save(
                            PetEntity.builder()
                                    .petName("Pongo")
                                    .owner(user)
                                    .build()
                    );
                    petRepository.save(
                            PetEntity.builder()
                                    .petName("Charlie")
                                    .owner(user)
                                    .build()
                    );
                }
            });
        };
    }
}
