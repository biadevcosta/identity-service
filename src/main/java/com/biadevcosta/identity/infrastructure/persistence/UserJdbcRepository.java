package com.biadevcosta.identity.infrastructure.persistence;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

/**
 * Spring Data JDBC repository over {@link UserEntity}. The entity carries a {@code @Version} field,
 * so {@code save()} inserts when it is null and updates otherwise (the id is assigned by the domain).
 */
public interface UserJdbcRepository extends CrudRepository<UserEntity, String> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
