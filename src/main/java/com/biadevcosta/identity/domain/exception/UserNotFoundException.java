package com.biadevcosta.identity.domain.exception;

/** Raised when a user id cannot be resolved. */
public class UserNotFoundException extends IdentityException {

    public UserNotFoundException(String id) {
        super("User not found: " + id);
    }
}
