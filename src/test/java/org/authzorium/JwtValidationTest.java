package org.authzorium;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JwtValidationTest {

    @Test
    public void signedJwtShouldValidateWithJwks() throws Exception {
        // Generate RSA key pair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        RSAPublicKey pub = (RSAPublicKey) kp.getPublic();
        RSAPrivateKey priv = (RSAPrivateKey) kp.getPrivate();

        // Build RSA JWK
        RSAKey jwk = new RSAKey.Builder(pub).privateKey(priv).keyID("test-key-1").build();
        JWKSet jwkSet = new JWKSet(jwk.toPublicJWK());

        // Create JWT claims
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("https://idp.example.com/")
                .subject("user-123")
                .audience("api://default")
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plusSeconds(60)))
                .claim("scope", "read write")
                .build();

        // Sign the JWT
        SignedJWT signedJWT = new SignedJWT(new com.nimbusds.jose.JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(jwk.getKeyID()).build(), claims);
        JWSSigner signer = new RSASSASigner(priv);
        try {
            signedJWT.sign(signer);
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }

        String token = signedJWT.serialize();

        // Create NimbusJwtDecoder from the JWK set
        // NimbusJwtDecoder requires a JWK Set URL; we'll instantiate decoder directly from the public RSA key
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(pub).build();

        Jwt jwt = decoder.decode(token);

        assertEquals("user-123", jwt.getSubject());
    }
}

