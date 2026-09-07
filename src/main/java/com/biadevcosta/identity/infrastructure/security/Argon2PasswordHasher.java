package com.biadevcosta.identity.infrastructure.security;

import com.biadevcosta.identity.application.port.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * {@link PasswordHasher} backed by a Spring Security {@link PasswordEncoder}. The encoder is a
 * {@code DelegatingPasswordEncoder} whose default algorithm is Argon2id (see the security wiring),
 * so hashes are stored with a {@code {argon2id}} prefix.
 */
public class Argon2PasswordHasher implements PasswordHasher {

    private final PasswordEncoder encoder;

    public Argon2PasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        return encoder.matches(rawPassword, hash);
    }
}
