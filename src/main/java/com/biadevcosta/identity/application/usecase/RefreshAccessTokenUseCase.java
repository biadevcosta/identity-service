package com.biadevcosta.identity.application.usecase;

import com.biadevcosta.identity.application.AuthTokens;
import com.biadevcosta.identity.application.port.RefreshTokenHasher;
import com.biadevcosta.identity.application.port.RefreshTokenRepository;
import com.biadevcosta.identity.application.port.UserRepository;
import com.biadevcosta.identity.domain.RefreshToken;
import com.biadevcosta.identity.domain.User;
import com.biadevcosta.identity.domain.exception.InvalidRefreshTokenException;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Exchanges a valid refresh token for a new token pair (rotation): the presented token is revoked
 * and a fresh access + refresh token are minted for the same user. Anything wrong — unknown, expired
 * or revoked token, or a user that no longer exists — fails as {@link InvalidRefreshTokenException}
 * without changing any stored state (validate everything first, revoke last).
 */
public class RefreshAccessTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenHasher refreshTokenHasher;
    private final UserRepository userRepository;
    private final TokenMinter tokenMinter;
    private final Clock clock;

    public RefreshAccessTokenUseCase(RefreshTokenRepository refreshTokenRepository,
                                     RefreshTokenHasher refreshTokenHasher,
                                     UserRepository userRepository,
                                     TokenMinter tokenMinter,
                                     Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenHasher = refreshTokenHasher;
        this.userRepository = userRepository;
        this.tokenMinter = tokenMinter;
        this.clock = clock;
    }

    public AuthTokens refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        RefreshToken stored = refreshTokenRepository
                .findByTokenHash(refreshTokenHasher.hash(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);
        if (!stored.isUsable(LocalDateTime.now(clock))) {
            throw new InvalidRefreshTokenException();
        }
        User user = userRepository.findById(stored.userId())
                .orElseThrow(InvalidRefreshTokenException::new);

        stored.revoke();
        refreshTokenRepository.save(stored);

        return tokenMinter.mintFor(user);
    }
}
