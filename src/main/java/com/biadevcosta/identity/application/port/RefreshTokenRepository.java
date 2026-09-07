package com.biadevcosta.identity.application.port;

import com.biadevcosta.identity.domain.RefreshToken;

import java.util.Optional;

/** Persistence port for {@link RefreshToken}. Only the token hash is ever stored or queried. */
public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
