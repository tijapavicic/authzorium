package org.authzorium;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTestUtils {

    @Value("${security.jwt.secret:changeit-changeit-changeit-changeit}")
    private String secret;

    public String createHmacToken(String subject, long ttlSeconds, Map<String, Object> extraClaims) throws Exception {
        byte[] sharedSecret = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        JWSSigner signer = new MACSigner(sharedSecret);

        JWTClaimsSet.Builder jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer("test")
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plusSeconds(ttlSeconds)))
                .claim("scope", "read");

        if (extraClaims != null) {
            for (Map.Entry<String, Object> e : extraClaims.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    jwtClaimsSet.claim(e.getKey(), e.getValue());
                }
            }
        }

        JWTClaimsSet claims = jwtClaimsSet.build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    public String createHmacToken(String subject, long ttlSeconds) throws Exception {
        return createHmacToken(subject, ttlSeconds, null);
    }

    public String createHmacToken(String subject) throws Exception {
        return createHmacToken(subject, 60);
    }
}
