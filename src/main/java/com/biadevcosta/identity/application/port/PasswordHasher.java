package com.biadevcosta.identity.application.port;

/**
 * Password hashing port. The core never knows the algorithm; the infrastructure adapter
 * uses Argon2id (via {@code DelegatingPasswordEncoder}).
 */
public interface PasswordHasher {

    /** Hashes a raw password. The returned value carries the algorithm id and its salt. */
    String hash(String rawPassword);

    /** True when {@code rawPassword} matches the stored {@code hash}. */
    boolean matches(String rawPassword, String hash);
}
