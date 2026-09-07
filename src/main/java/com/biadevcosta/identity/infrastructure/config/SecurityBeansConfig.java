package com.biadevcosta.identity.infrastructure.config;

import com.biadevcosta.identity.application.port.PasswordHasher;
import com.biadevcosta.identity.application.port.RefreshTokenHasher;
import com.biadevcosta.identity.application.port.TokenIssuer;
import com.biadevcosta.identity.infrastructure.security.Argon2PasswordHasher;
import com.biadevcosta.identity.infrastructure.security.NimbusJwtTokenIssuer;
import com.biadevcosta.identity.infrastructure.security.Sha256RefreshTokenHasher;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.util.Map;

/** Wires the security adapters (password hashing, JWT issuing, refresh-token hashing) and the clock. */
@Configuration
@EnableConfigurationProperties({JwtProperties.class, Argon2Properties.class})
public class SecurityBeansConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    PasswordEncoder passwordEncoder(Argon2Properties argon2) {
        Argon2PasswordEncoder argon2Encoder = new Argon2PasswordEncoder(
                argon2.saltLength(), argon2.hashLength(), argon2.parallelism(),
                argon2.memoryKb(), argon2.iterations());
        return new DelegatingPasswordEncoder("argon2id", Map.of("argon2id", argon2Encoder));
    }

    @Bean
    PasswordHasher passwordHasher(PasswordEncoder passwordEncoder) {
        return new Argon2PasswordHasher(passwordEncoder);
    }

    @Bean
    RefreshTokenHasher refreshTokenHasher() {
        return new Sha256RefreshTokenHasher();
    }

    @Bean
    TokenIssuer tokenIssuer(JwtProperties jwt, Clock clock) {
        return new NimbusJwtTokenIssuer(
                RsaKeys.privateKey(jwt.privateKey()), jwt.issuer(), jwt.audience(),
                jwt.accessTtlSeconds(), clock);
    }

    /** Public key the resource server uses to validate the access token on the ADMIN routes. */
    @Bean
    RSAPublicKey jwtVerificationKey(JwtProperties jwt) {
        return RsaKeys.publicKey(jwt.publicKey());
    }
}
