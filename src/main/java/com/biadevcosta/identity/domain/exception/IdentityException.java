package com.biadevcosta.identity.domain.exception;

/** Base type for every business rule violation raised by the identity domain. */
public class IdentityException extends RuntimeException {

    public IdentityException(String message) {
        super(message);
    }
}
