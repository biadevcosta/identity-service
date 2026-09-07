package com.biadevcosta.identity.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Sha256RefreshTokenHasherTest {

    private final Sha256RefreshTokenHasher hasher = new Sha256RefreshTokenHasher();

    @Test
    void randomToken_isUrlSafe_unique_andLongEnough() {
        String a = hasher.randomToken();
        String b = hasher.randomToken();

        assertThat(a).isNotEqualTo(b);
        assertThat(a).matches("[A-Za-z0-9_-]+");          // Base64URL, no padding
        assertThat(a.length()).isGreaterThanOrEqualTo(43); // 32 random bytes
    }

    @Test
    void hash_is64HexChars_deterministic_andInputSensitive() {
        String h1 = hasher.hash("token-abc");

        assertThat(h1).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(hasher.hash("token-abc")).isEqualTo(h1);
        assertThat(hasher.hash("token-abd")).isNotEqualTo(h1);
    }
}
