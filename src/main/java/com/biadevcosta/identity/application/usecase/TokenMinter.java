package com.biadevcosta.identity.application.usecase;

import com.biadevcosta.identity.application.AuthTokens;
import com.biadevcosta.identity.application.port.RefreshTokenHasher;
import com.biadevcosta.identity.application.port.RefreshTokenRepository;
import com.biadevcosta.identity.application.port.TokenIssuer;
import com.biadevcosta.identity.domain.RefreshToken;
import com.biadevcosta.identity.domain.Role;
import com.biadevcosta.identity.domain.User;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Shared token-minting step used by both login and refresh: issues a signed access token and a fresh
 * opaque refresh token (persisted only as its hash) for a given user. The {@code patientId} claim is
 * the user's own id for a {@link Role#PATIENT} and {@code null} for everyone else.
 */
public class TokenMinter {

    private final TokenIssuer tokenIssuer;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenHasher refreshTokenHasher;
    private final long refreshTtlSeconds;
    private final Clock clock;

    public TokenMinter(TokenIssuer tokenIssuer,
                       RefreshTokenRepository refreshTokenRepository,
                       RefreshTokenHasher refreshTokenHasher,
                       long refreshTtlSeconds,
                       Clock clock) {
        this.tokenIssuer = tokenIssuer;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenHasher = refreshTokenHasher;
        this.refreshTtlSeconds = refreshTtlSeconds;
        this.clock = clock;
    }

    public AuthTokens mintFor(User user) {
        String patientId = user.role() == Role.PATIENT ? user.id() : null;
        TokenIssuer.AccessToken access = tokenIssuer.issueAccessToken(user.id(), user.role(), patientId);

        String rawRefresh = refreshTokenHasher.randomToken();
        RefreshToken refreshToken = RefreshToken.issue(
                user.id(), refreshTokenHasher.hash(rawRefresh), refreshTtlSeconds, LocalDateTime.now(clock));
        refreshTokenRepository.save(refreshToken);

        return new AuthTokens(access.value(), rawRefresh, access.expiresInSeconds());
    }
}
