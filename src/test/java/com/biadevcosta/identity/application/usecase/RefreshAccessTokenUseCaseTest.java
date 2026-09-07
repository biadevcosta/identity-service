package com.biadevcosta.identity.application.usecase;

import com.biadevcosta.identity.application.AuthTokens;
import com.biadevcosta.identity.application.port.RefreshTokenHasher;
import com.biadevcosta.identity.application.port.RefreshTokenRepository;
import com.biadevcosta.identity.application.port.UserRepository;
import com.biadevcosta.identity.domain.RefreshToken;
import com.biadevcosta.identity.domain.Role;
import com.biadevcosta.identity.domain.User;
import com.biadevcosta.identity.domain.exception.InvalidRefreshTokenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshAccessTokenUseCaseTest {

    private static final Instant FIXED = Instant.parse("2026-01-01T12:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(FIXED, ZoneOffset.UTC);

    @Mock
    RefreshTokenRepository refreshTokenRepository;
    @Mock
    RefreshTokenHasher refreshTokenHasher;
    @Mock
    UserRepository userRepository;
    @Mock
    TokenMinter tokenMinter;
    @Captor
    ArgumentCaptor<RefreshToken> savedToken;

    private RefreshAccessTokenUseCase useCase() {
        return new RefreshAccessTokenUseCase(refreshTokenRepository, refreshTokenHasher,
                userRepository, tokenMinter, Clock.fixed(FIXED, ZoneOffset.UTC));
    }

    private User user() {
        return User.rehydrate("user-1", "u@x.com", "{argon2id}$h", Role.NURSE, "U",
                null, null, null, NOW.minusDays(10));
    }

    @Test
    void refresh_validToken_rotatesAndMintsNewPair() {
        RefreshToken stored = RefreshToken.issue("user-1", "hash-1", 604800, NOW.minusHours(1));
        AuthTokens minted = new AuthTokens("new-access", "new-refresh", 3600);
        when(refreshTokenHasher.hash("raw")).thenReturn("hash-1");
        when(refreshTokenRepository.findByTokenHash("hash-1")).thenReturn(Optional.of(stored));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user()));
        when(tokenMinter.mintFor(any(User.class))).thenReturn(minted);

        AuthTokens result = useCase().refresh("raw");

        assertThat(result).isEqualTo(minted);

        verify(refreshTokenRepository).save(savedToken.capture());
        assertThat(savedToken.getValue().revoked()).isTrue();

        InOrder order = inOrder(refreshTokenRepository, userRepository, tokenMinter);
        order.verify(refreshTokenRepository).findByTokenHash("hash-1");
        order.verify(userRepository).findById("user-1");
        order.verify(refreshTokenRepository).save(any(RefreshToken.class));
        order.verify(tokenMinter).mintFor(any(User.class));
    }

    @Test
    void refresh_unknownToken_throws_withoutRotatingOrMinting() {
        when(refreshTokenHasher.hash("raw")).thenReturn("hash-x");
        when(refreshTokenRepository.findByTokenHash("hash-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().refresh("raw")).isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).save(any());
        verifyNoInteractions(userRepository, tokenMinter);
    }

    @Test
    void refresh_revokedToken_throws() {
        RefreshToken revoked = RefreshToken.issue("user-1", "hash-1", 604800, NOW.minusHours(1));
        revoked.revoke();
        when(refreshTokenHasher.hash("raw")).thenReturn("hash-1");
        when(refreshTokenRepository.findByTokenHash("hash-1")).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> useCase().refresh("raw")).isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).save(any());
        verifyNoInteractions(userRepository, tokenMinter);
    }

    @Test
    void refresh_expiredToken_throws() {
        RefreshToken expired = RefreshToken.rehydrate("id-1", "user-1", "hash-1",
                NOW.minusSeconds(1), false, NOW.minusDays(8));
        when(refreshTokenHasher.hash("raw")).thenReturn("hash-1");
        when(refreshTokenRepository.findByTokenHash("hash-1")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> useCase().refresh("raw")).isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).save(any());
        verifyNoInteractions(userRepository, tokenMinter);
    }

    @Test
    void refresh_blankInput_throws_withoutTouchingAnything() {
        assertThatThrownBy(() -> useCase().refresh("  ")).isInstanceOf(InvalidRefreshTokenException.class);

        verifyNoInteractions(refreshTokenHasher, refreshTokenRepository, userRepository, tokenMinter);
    }

    @Test
    void refresh_userGone_throws_withoutRevokingOrMinting() {
        RefreshToken stored = RefreshToken.issue("user-1", "hash-1", 604800, NOW.minusHours(1));
        when(refreshTokenHasher.hash("raw")).thenReturn("hash-1");
        when(refreshTokenRepository.findByTokenHash("hash-1")).thenReturn(Optional.of(stored));
        when(userRepository.findById("user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().refresh("raw")).isInstanceOf(InvalidRefreshTokenException.class);

        assertThat(stored.revoked()).isFalse();
        verify(refreshTokenRepository, never()).save(any());
        verifyNoInteractions(tokenMinter);
    }
}
