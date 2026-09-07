package com.biadevcosta.identity.domain.exception;

/** Raised when a user breaks a creation invariant (missing email, doctor without CRM, ...). */
public class InvalidUserException extends IdentityException {

    public InvalidUserException(String reason) {
        super(reason);
    }
}
