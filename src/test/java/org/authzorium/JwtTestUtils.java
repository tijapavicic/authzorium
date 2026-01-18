package org.authzorium;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTestUtils {

    // Default secret must match SecurityConfig default: changeit-changeit-changeit-changeit
    private static final String DEFAULT_SECRET = "changeit-changeit-changeit-changeit";

    public String createHmacToken(String subject) throws Exception {
        return createHmacToken(subject, 60);
    }

    public String createHmacToken(String subject, long ttlSeconds) throws Exception {
        return createHmacToken(subject, ttlSeconds, null);
    }

    public String createHmacToken(String subject, long ttlSeconds, Map<String, Object> extra) throws Exception {
        byte[] secret = DEFAULT_SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        JWSSigner signer = new MACSigner(secret);

        Instant now = Instant.now();
        JWTClaimsSet.Builder b = new JWTClaimsSet.Builder()
                .subject(subject)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(ttlSeconds)));
        if (extra != null) {
            for (Map.Entry<String, Object> e : extra.entrySet()) {
                b.claim(e.getKey(), e.getValue());
            }
        }

        SignedJWT signedJWT = new SignedJWT(new com.nimbusds.jose.JWSHeader(JWSAlgorithm.HS256), b.build());
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }
}

