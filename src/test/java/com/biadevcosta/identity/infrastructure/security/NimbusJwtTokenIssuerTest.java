package com.biadevcosta.identity.infrastructure.security;

import com.biadevcosta.identity.application.port.TokenIssuer;
import com.biadevcosta.identity.domain.Role;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class NimbusJwtTokenIssuerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
    private static final long TTL = 3600;

    private static RSAPrivateKey privateKey;
    private static RSAPublicKey publicKey;

    @BeforeAll
    static void keys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        privateKey = (RSAPrivateKey) pair.getPrivate();
        publicKey = (RSAPublicKey) pair.getPublic();
    }

    private NimbusJwtTokenIssuer issuer() {
        return new NimbusJwtTokenIssuer(privateKey, "hospital-identity", "hospital-services",
                TTL, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void issue_signsWithRs256_andFillsExpectedClaims() throws Exception {
        TokenIssuer.AccessToken token = issuer().issueAccessToken("user-1", Role.NURSE, null);

        assertThat(token.expiresInSeconds()).isEqualTo(TTL);

        SignedJWT jwt = SignedJWT.parse(token.value());
        assertThat(jwt.verify(new RSASSAVerifier(publicKey))).isTrue();
        assertThat(jwt.getHeader().getAlgorithm().getName()).isEqualTo("RS256");

        JWTClaimsSet claims = jwt.getJWTClaimsSet();
        assertThat(claims.getIssuer()).isEqualTo("hospital-identity");
        assertThat(claims.getAudience()).containsExactly("hospital-services");
        assertThat(claims.getSubject()).isEqualTo("user-1");
        assertThat(claims.getStringClaim("role")).isEqualTo("NURSE");
        assertThat(claims.getStringClaim("patientId")).isNull();
        assertThat(claims.getIssueTime().toInstant()).isEqualTo(NOW);
        assertThat(claims.getExpirationTime().toInstant()).isEqualTo(NOW.plusSeconds(TTL));
    }

    @Test
    void issue_includesPatientIdClaim_forPatientUsers() throws Exception {
        TokenIssuer.AccessToken token = issuer().issueAccessToken("pat-1", Role.PATIENT, "pat-1");

        JWTClaimsSet claims = SignedJWT.parse(token.value()).getJWTClaimsSet();
        assertThat(claims.getStringClaim("role")).isEqualTo("PATIENT");
        assertThat(claims.getStringClaim("patientId")).isEqualTo("pat-1");
    }

    @Test
    void issue_tokenFailsVerificationWithAWrongKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        RSAPublicKey otherKey = (RSAPublicKey) generator.generateKeyPair().getPublic();

        SignedJWT jwt = SignedJWT.parse(issuer().issueAccessToken("user-1", Role.ADMIN, null).value());

        assertThat(jwt.verify(new RSASSAVerifier(otherKey))).isFalse();
    }
}
