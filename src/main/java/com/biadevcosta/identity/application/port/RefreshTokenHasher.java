package com.biadevcosta.identity.application.port;

/**
 * How the opaque refresh token is represented. The raw value is handed to the client once and
 * never stored; only its {@link #hash(String) hash} is persisted and looked up.
 */
public interface RefreshTokenHasher {

    /** A fresh, cryptographically-random opaque token to return to the client. */
    String randomToken();

    /** Deterministic hash (SHA-256) of a raw token — the form persisted and queried. */
    String hash(String rawToken);
}
