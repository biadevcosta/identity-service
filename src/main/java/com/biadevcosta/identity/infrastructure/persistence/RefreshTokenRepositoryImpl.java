package com.biadevcosta.identity.infrastructure.persistence;

import com.biadevcosta.identity.application.port.RefreshTokenRepository;
import com.biadevcosta.identity.domain.RefreshToken;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** {@link RefreshTokenRepository} backed by Spring Data JDBC. */
@Repository
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final RefreshTokenJdbcRepository jdbc;

    public RefreshTokenRepositoryImpl(RefreshTokenJdbcRepository jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenEntity entity = jdbc.findById(token.id())
                .map(existing -> {
                    RefreshTokenMapper.copyInto(token, existing); // keep @Version -> UPDATE (revoke)
                    return existing;
                })
                .orElseGet(() -> RefreshTokenMapper.toNewEntity(token)); // version null -> INSERT
        return RefreshTokenMapper.toDomain(jdbc.save(entity));
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jdbc.findByTokenHash(tokenHash).map(RefreshTokenMapper::toDomain);
    }
}
