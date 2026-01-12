package org.authzorium;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.authzorium.security.Role;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Base helpers for slice (web) tests. These helpers are intentionally lightweight and do not
 * require a full Spring Boot context; they resolve the JWT secret from system properties or
 * environment variables so slice tests can generate tokens easily.
 */
public abstract class BaseSliceTest {

    // Default secret (same default used elsewhere in the project)
    private static final String DEFAULT_SECRET = "changeit-changeit-changeit-changeit";

    // Resolve secret from System property -> ENV -> default
    protected String secretFromEnv() {
        String prop = System.getProperty("security.jwt.secret");
        if (prop != null && !prop.isBlank()) return prop;
        String env = System.getenv("SECURITY_JWT_SECRET");
        if (env != null && !env.isBlank()) return env;
        return DEFAULT_SECRET;
    }

    protected String createToken(String subject) {
        return createToken(subject, 60);
    }

    protected String createToken(String subject, long ttlSeconds) {
        return createToken(subject, ttlSeconds, null);
    }

    /**
     * Create an HMAC-signed JWT for tests with optional extra claims.
     * The standard claims (sub, iss, iat, exp) are always set. Extra claims in the map
     * will be added directly to the JWT claims set. For 'scope' prefer a space-separated
     * string (or use the convenience helper), and for 'roles' pass a List<String>.
     */
    protected String createToken(String subject, long ttlSeconds, Map<String, Object> extraClaims) {
        try {
            byte[] sharedSecret = secretFromEnv().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            JWSSigner signer = new MACSigner(sharedSecret);

            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                    .subject(Objects.requireNonNull(subject))
                    .issuer("test")
                    .issueTime(Date.from(Instant.now()))
                    .expirationTime(Date.from(Instant.now().plusSeconds(ttlSeconds)));

            if (extraClaims != null) {
                for (Map.Entry<String, Object> e : extraClaims.entrySet()) {
                    if (e.getKey() != null && e.getValue() != null) {
                        builder.claim(e.getKey(), e.getValue());
                    }
                }
            }

            JWTClaimsSet claims = builder.build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create HMAC JWT", e);
        }
    }

    // Convenience: include scopes as space-separated string in 'scope' claim
    protected String createTokenWithScopes(String subject, List<String> scopes) {
        String scopeValue = (scopes == null || scopes.isEmpty()) ? null : String.join(" ", scopes);
        return createToken(subject, 60, scopeValue == null ? null : Map.of("scope", scopeValue));
    }

    // Convenience: include roles as a collection under 'roles' claim
    protected String createTokenWithRoles(String subject, List<String> roles) {
        return createToken(subject, 60, roles == null ? null : Map.of("roles", roles));
    }

    protected String authHeader(String subject) {
        return "Bearer " + createToken(subject);
    }

    protected RequestPostProcessor bearerToken(String subject) {
        String token = createToken(subject);
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }

    protected RequestPostProcessor bearerTokenWithClaims(String subject, Map<String, Object> extraClaims) {
        String token = createToken(subject, 60, extraClaims);
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }

    protected RequestPostProcessor bearerTokenWithRoles(String subject, List<String> roles) {
        String token = createTokenWithRoles(subject, roles);
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }

    protected RequestPostProcessor bearerTokenWithScopes(String subject, List<String> scopes) {
        String token = createTokenWithScopes(subject, scopes);
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }

    /**
     * Convenience: map role names to ROLE_ authorities expected by SecurityConfig.
     */
    protected List<String> rolesToAuthorities(List<String> roles) {
        List<String> out = new ArrayList<>();
        if (roles == null) return out;
        for (String r : roles) if (r != null) out.add("ROLE_" + r);
        return out;
    }

    // Overload that accepts enum roles
    protected List<String> rolesToAuthoritiesFromEnum(List<Role> roles) {
        List<String> out = new ArrayList<>();
        if (roles == null) return out;
        for (Role r : roles) if (r != null) out.add(r.authority());
        return out;
    }

    // Create token with roles claim from enum list
    protected String createTokenWithRolesEnums(String subject, List<Role> roles) {
        try {
            java.util.Map<String, Object> extra = new java.util.HashMap<>();
            if (roles != null && !roles.isEmpty()) {
                java.util.List<String> rs = new java.util.ArrayList<>();
                for (Role r : roles) rs.add(r.toString());
                extra.put("roles", rs);
            }
            return createToken(subject, 60, extra);
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
