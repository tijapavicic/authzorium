package org.authzorium;

import org.authzorium.client.mapper.UserMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfig {
    // Add test-scoped bean definitions here if needed later

    @Bean
    public JwtTestUtils jwtTestUtils() {
        return new JwtTestUtils();
    }

    @Bean
    public UserMapper userMapper() {
        // Provide a MapStruct mapper instance for tests. If MapStruct generated a Spring component,
        // that bean will take precedence in application context; this test bean ensures availability
        // in case annotation processing didn't produce a Spring component for MapStruct.
        try {
            return Mappers.getMapper(UserMapper.class);
        } catch (Exception ex) {
            return new org.authzorium.client.mapper.TestUserMapper();
        }
    }
}
