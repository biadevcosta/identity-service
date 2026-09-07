package com.biadevcosta.identity.domain.exception;

/** Raised when a refresh token is unknown, expired or already revoked. */
public class InvalidRefreshTokenException extends IdentityException {

    public InvalidRefreshTokenException() {
        super("Invalid refresh token");
    }
}
