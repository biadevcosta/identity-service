package com.biadevcosta.identity.domain.exception;

/** Raised when registering a user with an email that already exists. */
public class EmailAlreadyUsedException extends IdentityException {

    public EmailAlreadyUsedException(String email) {
        super("Email already used: " + email);
    }
}
