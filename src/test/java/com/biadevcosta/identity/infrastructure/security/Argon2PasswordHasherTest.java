package com.biadevcosta.identity.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class Argon2PasswordHasherTest {

    private Argon2PasswordHasher hasher() {
        // deliberately light Argon2 params (1 MiB, 1 pass) so the test stays fast
        Argon2PasswordEncoder argon2 = new Argon2PasswordEncoder(16, 32, 1, 1024, 1);
        PasswordEncoder delegating = new DelegatingPasswordEncoder("argon2id", Map.of("argon2id", argon2));
        return new Argon2PasswordHasher(delegating);
    }

    @Test
    void hash_producesArgon2idPrefixedValue_notThePlainPassword() {
        String hash = hasher().hash("s3cret");

        assertThat(hash).startsWith("{argon2id}");
        assertThat(hash).doesNotContain("s3cret");
    }

    @Test
    void matches_trueForRightPassword_falseForWrong() {
        Argon2PasswordHasher hasher = hasher();
        String hash = hasher.hash("s3cret");

        assertThat(hasher.matches("s3cret", hash)).isTrue();
        assertThat(hasher.matches("wrong", hash)).isFalse();
    }
}
