package com.biadevcosta.identity.infrastructure.security;

import com.biadevcosta.identity.application.port.TokenIssuer;
import com.biadevcosta.identity.domain.Role;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.interfaces.RSAPrivateKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

/**
 * Issues RS256-signed access tokens with Nimbus. Claims: {@code iss}, {@code aud}, {@code sub}
 * (user id), {@code role}, {@code iat}, {@code exp}, plus {@code patientId} for patient users.
 */
public class NimbusJwtTokenIssuer implements TokenIssuer {

    private final RSASSASigner signer;
    private final String issuer;
    private final String audience;
    private final long accessTtlSeconds;
    private final Clock clock;

    public NimbusJwtTokenIssuer(RSAPrivateKey privateKey, String issuer, String audience,
                                long accessTtlSeconds, Clock clock) {
        this.signer = new RSASSASigner(privateKey);
        this.issuer = issuer;
        this.audience = audience;
        this.accessTtlSeconds = accessTtlSeconds;
        this.clock = clock;
    }

    @Override
    public AccessToken issueAccessToken(String userId, Role role, String patientId) {
        Instant now = clock.instant();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .subject(userId)
                .claim("role", role.name())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(accessTtlSeconds)));
        if (patientId != null) {
            claims.claim("patientId", patientId);
        }
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(),
                claims.build());
        try {
            jwt.sign(signer);
        } catch (JOSEException e) {
            throw new IllegalStateException("failed to sign access token", e);
        }
        return new AccessToken(jwt.serialize(), accessTtlSeconds);
    }
}
