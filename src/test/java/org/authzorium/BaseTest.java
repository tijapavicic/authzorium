package org.authzorium;

import org.authzorium.client.security.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
public abstract class BaseTest {
    // Extend this class in Spring-based tests to enable the 'test' profile and Spring context

    @Autowired
    protected JwtTestUtils jwtTestUtils;

    // Helper: create a token with default TTL
    protected String createToken(String subject) {
        try {
            return jwtTestUtils.createHmacToken(subject);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Helper: create a token with explicit TTL (seconds)
    protected String createToken(String subject, long ttlSeconds) {
        try {
            return jwtTestUtils.createHmacToken(subject, ttlSeconds);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Helper: Authorization header value
    protected String authHeader(String subject) {
        return "Bearer " + createToken(subject);
    }

    // Helper: RequestPostProcessor to attach Bearer token to MockMvc requests
    protected RequestPostProcessor bearerToken(String subject) {
        String token = createToken(subject);
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }

    /**
     * Convenience: map role names to the authorities format expected by SecurityConfig.
     * Example: ["ADMIN","USER"] -> ["ROLE_ADMIN","ROLE_USER"]
     */
    protected List<String> rolesToAuthorities(List<String> roles) {
        List<String> out = new ArrayList<>();
        if (roles == null) return out;
        for (String r : roles) {
            if (r != null) out.add("ROLE_" + r);
        }
        return out;
    }

    // New overload: accept enum roles directly
    protected List<String> rolesToAuthoritiesFromEnum(List<Role> roles) {
        List<String> out = new ArrayList<>();
        if (roles == null) return out;
        for (Role r : roles) if (r != null) out.add(r.authority());
        return out;
    }

    // Create token with roles claim from enum list
    protected String createTokenWithRolesEnums(String subject, List<Role> roles) {
        try {
            Map<String, Object> extra = new HashMap<>();
            if (roles != null && !roles.isEmpty()) {
                List<String> rs = new ArrayList<>();
                for (Role r : roles) rs.add(r.toString());
                extra.put("roles", rs);
            }
            return jwtTestUtils.createHmacToken(subject, 60, extra);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected RequestPostProcessor bearerTokenWithRolesEnums(String subject, List<Role> roles) {
        String token = createTokenWithRolesEnums(subject, roles);
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }
}
