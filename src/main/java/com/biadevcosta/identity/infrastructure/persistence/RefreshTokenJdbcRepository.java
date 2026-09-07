package com.biadevcosta.identity.infrastructure.persistence;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

/** Spring Data JDBC repository over {@link RefreshTokenEntity} (see {@link UserJdbcRepository} for the @Version note). */
public interface RefreshTokenJdbcRepository extends CrudRepository<RefreshTokenEntity, String> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);
}
