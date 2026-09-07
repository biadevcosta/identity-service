package com.biadevcosta.identity.infrastructure.persistence;

import com.biadevcosta.identity.domain.RefreshToken;

/** Translates between the {@link RefreshToken} domain object and its persistence row. */
final class RefreshTokenMapper {

    private RefreshTokenMapper() {
    }

    static RefreshTokenEntity toNewEntity(RefreshToken token) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        copyInto(token, entity);
        return entity;
    }

    /** Copies the domain state onto an existing row, keeping its {@code version} (used by the revoke update). */
    static void copyInto(RefreshToken token, RefreshTokenEntity entity) {
        entity.setId(token.id());
        entity.setTokenHash(token.tokenHash());
        entity.setUserId(token.userId());
        entity.setExpiresAt(token.expiresAt());
        entity.setRevoked(token.revoked());
        entity.setCreatedAt(token.createdAt());
    }

    static RefreshToken toDomain(RefreshTokenEntity entity) {
        return RefreshToken.rehydrate(
                entity.getId(), entity.getUserId(), entity.getTokenHash(),
                entity.getExpiresAt(), entity.isRevoked(), entity.getCreatedAt());
    }
}
