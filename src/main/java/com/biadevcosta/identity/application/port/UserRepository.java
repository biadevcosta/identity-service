package com.biadevcosta.identity.application.port;

import com.biadevcosta.identity.domain.User;

import java.util.Optional;

/** Persistence port for {@link User}. Implemented by an infrastructure adapter (Spring Data JDBC). */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(String id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
