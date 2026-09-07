package com.biadevcosta.identity.domain;

import com.biadevcosta.identity.domain.exception.InvalidRefreshTokenException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 12, 0);
    private static final long TTL = 3600;

    @Test
    void issue_startsUsableAndNotRevoked() {
        RefreshToken t = RefreshToken.issue("user-1", "hash-1", TTL, NOW);

        assertThat(t.id()).isNotBlank();
        assertThat(t.userId()).isEqualTo("user-1");
        assertThat(t.tokenHash()).isEqualTo("hash-1");
        assertThat(t.revoked()).isFalse();
        assertThat(t.expiresAt()).isEqualTo(NOW.plusSeconds(TTL));
        assertThat(t.createdAt()).isEqualTo(NOW);
        assertThat(t.isUsable(NOW)).isTrue();
    }

    @Test
    void isUsable_falseFromExpiryOnward() {
        RefreshToken t = RefreshToken.issue("user-1", "hash-1", TTL, NOW);

        assertThat(t.isUsable(NOW.plusSeconds(TTL - 1))).isTrue();
        assertThat(t.isUsable(NOW.plusSeconds(TTL))).isFalse();        // expiresAt is not "before" itself
        assertThat(t.isUsable(NOW.plusSeconds(TTL + 10))).isFalse();
    }

    @Test
    void revoke_makesItUnusable() {
        RefreshToken t = RefreshToken.issue("user-1", "hash-1", TTL, NOW);

        t.revoke();

        assertThat(t.revoked()).isTrue();
        assertThat(t.isUsable(NOW)).isFalse();
    }

    @Test
    void revoke_isIdempotent() {
        RefreshToken t = RefreshToken.issue("user-1", "hash-1", TTL, NOW);

        t.revoke();
        t.revoke();

        assertThat(t.revoked()).isTrue();
    }

    @Test
    void issue_rejectsBadArguments() {
        assertThatThrownBy(() -> RefreshToken.issue(" ", "hash", TTL, NOW))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> RefreshToken.issue("user-1", " ", TTL, NOW))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> RefreshToken.issue("user-1", "hash", 0, NOW))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> RefreshToken.issue("user-1", "hash", TTL, null))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rehydrate_doesNotRevalidate() {
        RefreshToken t = RefreshToken.rehydrate("id-1", "user-1", "hash-1",
                NOW.minusSeconds(1), true, NOW.minusSeconds(TTL));

        assertThat(t.id()).isEqualTo("id-1");
        assertThat(t.revoked()).isTrue();
        assertThat(t.isUsable(NOW)).isFalse();
    }
}
