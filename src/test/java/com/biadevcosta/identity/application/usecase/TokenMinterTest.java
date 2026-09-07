package com.biadevcosta.identity.application.usecase;

import com.biadevcosta.identity.application.AuthTokens;
import com.biadevcosta.identity.application.port.RefreshTokenHasher;
import com.biadevcosta.identity.application.port.RefreshTokenRepository;
import com.biadevcosta.identity.application.port.TokenIssuer;
import com.biadevcosta.identity.domain.RefreshToken;
import com.biadevcosta.identity.domain.Role;
import com.biadevcosta.identity.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenMinterTest {

    private static final Instant FIXED = Instant.parse("2026-01-01T12:00:00Z");
    private static final long REFRESH_TTL = 604800; // 7 days

    @Mock
    TokenIssuer tokenIssuer;
    @Mock
    RefreshTokenRepository refreshTokenRepository;
    @Mock
    RefreshTokenHasher refreshTokenHasher;
    @Captor
    ArgumentCaptor<RefreshToken> savedToken;

    private TokenMinter minter() {
        return new TokenMinter(tokenIssuer, refreshTokenRepository, refreshTokenHasher,
                REFRESH_TTL, Clock.fixed(FIXED, ZoneOffset.UTC));
    }

    private User user(String id, Role role) {
        boolean doctor = role == Role.DOCTOR;
        return User.rehydrate(id, "u@x.com", "{argon2id}$h", role, "U", null,
                doctor ? "CRM-1" : null, doctor ? "Cardio" : null, LocalDateTime.now());
    }

    @Test
    void mintFor_nonPatient_issuesAccessWithNullPatientId_andPersistsHashedRefresh() {
        when(tokenIssuer.issueAccessToken(eq("user-1"), eq(Role.NURSE), isNull()))
                .thenReturn(new TokenIssuer.AccessToken("access-jwt", 3600));
        when(refreshTokenHasher.randomToken()).thenReturn("raw-refresh");
        when(refreshTokenHasher.hash("raw-refresh")).thenReturn("sha256-hash");

        AuthTokens tokens = minter().mintFor(user("user-1", Role.NURSE));

        assertThat(tokens).isEqualTo(new AuthTokens("access-jwt", "raw-refresh", 3600));

        verify(refreshTokenRepository).save(savedToken.capture());
        RefreshToken saved = savedToken.getValue();
        assertThat(saved.userId()).isEqualTo("user-1");
        assertThat(saved.tokenHash()).isEqualTo("sha256-hash");
        assertThat(saved.revoked()).isFalse();
        assertThat(saved.expiresAt())
                .isEqualTo(LocalDateTime.ofInstant(FIXED, ZoneOffset.UTC).plusSeconds(REFRESH_TTL));
    }

    @Test
    void mintFor_patient_passesUserIdAsPatientId() {
        when(tokenIssuer.issueAccessToken(eq("pat-1"), eq(Role.PATIENT), eq("pat-1")))
                .thenReturn(new TokenIssuer.AccessToken("access-jwt", 3600));
        when(refreshTokenHasher.randomToken()).thenReturn("raw");
        when(refreshTokenHasher.hash("raw")).thenReturn("hash");

        AuthTokens tokens = minter().mintFor(user("pat-1", Role.PATIENT));

        assertThat(tokens.accessToken()).isEqualTo("access-jwt");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }
}
