package com.biadevcosta.identity.domain;

import com.biadevcosta.identity.domain.exception.InvalidRefreshTokenException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Refresh token aggregate. The opaque token value never reaches the domain — only its
 * {@code tokenHash} (a SHA-256 of the raw value, produced in infrastructure) is stored.
 * Holds the lifecycle rules: a token is usable while it is not revoked and not expired;
 * every refresh revokes the presented token and issues a new one (rotation, handled by the use case).
 * This class has no framework dependency.
 */
public class RefreshToken {

    private final String id;
    private final String userId;
    private final String tokenHash;
    private final LocalDateTime expiresAt;
    private boolean revoked;
    private final LocalDateTime createdAt;

    private RefreshToken(String id, String userId, String tokenHash,
                         LocalDateTime expiresAt, boolean revoked, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
        this.createdAt = createdAt;
    }

    /** Issue factory: a fresh, non-revoked token valid for {@code ttlSeconds} from {@code now}. */
    public static RefreshToken issue(String userId, String tokenHash, long ttlSeconds, LocalDateTime now) {
        if (userId == null || userId.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        if (ttlSeconds <= 0 || now == null) {
            throw new InvalidRefreshTokenException();
        }
        return new RefreshToken(UUID.randomUUID().toString(), userId, tokenHash,
                now.plusSeconds(ttlSeconds), false, now);
    }

    /** Rebuilds a refresh token from persistence without re-checking invariants. */
    public static RefreshToken rehydrate(String id, String userId, String tokenHash,
                                         LocalDateTime expiresAt, boolean revoked, LocalDateTime createdAt) {
        return new RefreshToken(id, userId, tokenHash, expiresAt, revoked, createdAt);
    }

    /** Usable while not revoked and not past {@code expiresAt}. */
    public boolean isUsable(LocalDateTime now) {
        return !revoked && now.isBefore(expiresAt);
    }

    /** Marks the token revoked. Idempotent. */
    public void revoke() {
        this.revoked = true;
    }

    public String id() {
        return id;
    }

    public String userId() {
        return userId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public LocalDateTime expiresAt() {
        return expiresAt;
    }

    public boolean revoked() {
        return revoked;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }
}
