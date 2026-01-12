package org.authzorium;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfig {
    // Add test-scoped bean definitions here if needed later

    @Bean
    public JwtTestUtils jwtTestUtils() {
        return new JwtTestUtils();
    }
}
