package com.biadevcosta.identity.domain;

/**
 * System roles carried by the {@code role} claim of the access token.
 * {@link #DOCTOR}, {@link #NURSE} and {@link #PATIENT} are clinical actors;
 * {@link #ADMIN} only manages users (it is the only role allowed to register other users).
 */
public enum Role {
    DOCTOR,
    NURSE,
    PATIENT,
    ADMIN
}
