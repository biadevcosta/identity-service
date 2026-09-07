package com.biadevcosta.identity.domain.exception;

/**
 * Raised on login when the email is unknown or the password does not match.
 * The message is deliberately generic so the caller cannot tell which of the two failed.
 */
public class InvalidCredentialsException extends IdentityException {

    public InvalidCredentialsException() {
        super("Invalid credentials");
    }
}
