package org.authzorium;

import com.nimbusds.jwt.SignedJWT;
import org.authzorium.client.security.Role;
import org.junit.jupiter.api.Test;


import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BaseSliceTestUsageTest extends BaseSliceTest {

    @Test
    void createToken_withRolesAndScopes_includesClaims() throws Exception {
        List<Role> roles = List.of(Role.ADMIN, Role.USER);
        List<String> scopes = List.of("read", "write");

        String tokenWithRoles = createTokenWithRolesEnums("alice", roles);
        String tokenWithScopes = createTokenWithScopes("bob", scopes);

        SignedJWT jwtRoles = SignedJWT.parse(tokenWithRoles);
        SignedJWT jwtScopes = SignedJWT.parse(tokenWithScopes);

        Object rolesClaim = jwtRoles.getJWTClaimsSet().getClaim("roles");
        Object scopeClaim = jwtScopes.getJWTClaimsSet().getClaim("scope");

        assertNotNull(rolesClaim, "roles claim should be present");
        assertInstanceOf(List.class, rolesClaim, "roles claim should be a List");
        // convert to list of strings and compare to enum names
        @SuppressWarnings("unchecked")
        List<String> rolesList = (List<String>) rolesClaim;
        assertEquals(List.of("ADMIN", "USER"), rolesList);

        assertNotNull(scopeClaim, "scope claim should be present");
        assertInstanceOf(String.class, scopeClaim, "scope should be a space-separated string");
        assertEquals(String.join(" ", scopes), scopeClaim);

        // also verify subject and expiration exist
        assertEquals("alice", jwtRoles.getJWTClaimsSet().getSubject());
        assertEquals("bob", jwtScopes.getJWTClaimsSet().getSubject());
        assertNotNull(jwtRoles.getJWTClaimsSet().getExpirationTime());
        assertNotNull(jwtScopes.getJWTClaimsSet().getExpirationTime());
    }
}
